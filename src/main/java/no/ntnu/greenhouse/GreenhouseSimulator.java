package no.ntnu.greenhouse;

import java.security.KeyStoreException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import no.ntnu.communication.SensorActuatorTcpClient;
import no.ntnu.communication.TcpServer;
import no.ntnu.listeners.greenhouse.NodeStateListener;
import no.ntnu.tools.Logger;

/**
 * Application entrypoint - a simulator for a greenhouse.
 */
public class GreenhouseSimulator {
    private final Map<Integer, SensorActuatorNode> nodes = new HashMap<>();
    private TcpServer server;
    private final List<SensorActuatorTcpClient> clients = new ArrayList<>();

    private final List<PeriodicSwitch> periodicSwitches = new LinkedList<>();
    private final boolean fake;
    private final String keyStorePath;
    private final String keyStorePassword;

    /**
     * Create a greenhouse simulator.
     *
     * @param fake When true, simulate a fake periodic events instead of creating socket communication
     * @param keyStorePath the path to the keystore file
     * @param keyStorePassword the password for the keystore
     */
    public GreenhouseSimulator(boolean fake, String keyStorePath, String keyStorePassword) {
        this.fake = fake;
        this.keyStorePath = keyStorePath;
        this.keyStorePassword = keyStorePassword;
        if (!fake) {
            try {
                server = new TcpServer(nodes, keyStorePath, keyStorePassword);
            } catch (KeyStoreException e) {
                Logger.error("Failed to initialize server: " + e.getMessage());
            }
        }
    }

    /**
     * Initialise the greenhouse but don't start the simulation just yet.
     */
    public void initialize() {
        Logger.info("GreenhouseSimulator.initialize() called");
        createNode(1, 2, 1, 0, 0);
        createNode(1, 0, 0, 2, 1);
        createNode(2, 0, 0, 0, 0);
        Logger.info("Nodes created: " + nodes.keySet());
        Logger.info("Greenhouse initialized");
    }

    private void createNode(int temperature, int humidity, int windows, int fans, int heaters) {
        SensorActuatorNode node = DeviceFactory.createNode(
                temperature, humidity, windows, fans, heaters);
        nodes.put(node.getId(), node);
    }

    /**
     * Start a simulation of a greenhouse - all the sensor and actuator nodes inside it.
     */
    public void start() {
        Logger.info("GreenhouseSimulator.start() called");
        initiateCommunication();
        for (SensorActuatorNode node : nodes.values()) {
            node.start();
            Logger.info("Node " + node.getId() + " started");
        }
        for (PeriodicSwitch periodicSwitch : periodicSwitches) {
            periodicSwitch.start();
        }

        Logger.info("Simulator started");
    }

    private void initiateCommunication() {
        Logger.info("Initiating communication");
        if (fake) {
            initiateFakePeriodicSwitches();
            Logger.info("Fake periodic switches initiated");
        } else {
            initiateRealCommunication();
            Logger.info("Real communication initiated");
        }
    }

    /**
     * Initiate real communication.
     */
    public void initiateRealCommunication() {
        Logger.info("Initiating real communication");
        if (server != null) {
            new Thread(() -> {
                try {
                    server.startServer();
                    Logger.info("Server started on port " + TcpServer.PORT_NUMBER);
                } catch (Exception e) {
                    Logger.error("Failed to start server: " + e.getMessage());
                }
            }, "TCP-Server").start();

            for (SensorActuatorNode node : nodes.values()) {
                try {
                    SensorActuatorTcpClient client = new SensorActuatorTcpClient(node, keyStorePath, keyStorePassword);
                    node.addSensorListener(client);
                    node.addStateListener(client);
                    node.addActuatorListener(client);
                    clients.add(client);

                    new Thread(() -> {
                        try {
                            client.start();
                            Logger.info("Client started for node " + node.getId());
                        } catch (Exception e) {
                            Logger.error("Failed to start client for node " + node.getId() + ": " + e.getMessage());
                        }
                    }, "TCP-Client-" + node.getId()).start();
                } catch (KeyStoreException e) {
                    Logger.error("Failed to initialize client for node " + node.getId() + ": " + e.getMessage());
                }
            }
        } else {
            Logger.error("Server not initialized");
        }
    }

    private void initiateFakePeriodicSwitches() {
        periodicSwitches.add(new PeriodicSwitch("Window DJ", nodes.get(1), 2, 20000));
        periodicSwitches.add(new PeriodicSwitch("Heater DJ", nodes.get(2), 7, 8000));
    }

    /**
     * Stop the simulation of the greenhouse - all the nodes in it.
     */
    public void stop() {
        stopCommunication();
        for (SensorActuatorNode node : nodes.values()) {
            node.stop();
        }
    }

    private void stopCommunication() {
        if (fake) {
            for (PeriodicSwitch periodicSwitch : periodicSwitches) {
                periodicSwitch.stop();
            }
        } else {
            for (SensorActuatorTcpClient client : clients) {
                client.stop();
            }
            clients.clear();
        }
    }

    /**
     * Add a listener for notification of node staring and stopping.
     *
     * @param listener The listener which will receive notifications
     */
    public void subscribeToLifecycleUpdates(NodeStateListener listener) {
        for (SensorActuatorNode node : nodes.values()) {
            node.addStateListener(listener);
        }
    }
}
