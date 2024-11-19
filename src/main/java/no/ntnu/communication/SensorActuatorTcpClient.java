package no.ntnu.communication;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import no.ntnu.greenhouse.Actuator;
import no.ntnu.greenhouse.ActuatorCollection;
import no.ntnu.greenhouse.Sensor;
import no.ntnu.greenhouse.SensorActuatorNode;
import no.ntnu.listeners.common.ActuatorListener;
import no.ntnu.listeners.greenhouse.NodeStateListener;
import no.ntnu.listeners.greenhouse.SensorListener;
import no.ntnu.tools.Logger;

public class SensorActuatorTcpClient implements SensorListener, NodeStateListener, ActuatorListener {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 10025;
    private final SensorActuatorNode node;
    private Socket socket;
    private PrintWriter output;
    private BufferedReader input;
    private boolean isRunning;

    public SensorActuatorTcpClient(SensorActuatorNode node) {
        this.node = node;
    }

    public void start() {
        try {
            socket = new Socket(SERVER_HOST, SERVER_PORT);
            output = new PrintWriter(socket.getOutputStream(), true);
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            isRunning = true;
            sendNodeInfo();
            startListening();
            Logger.info("Node " + node.getId() + " connected to server");
        } catch (IOException e) {
            Logger.error("Could not connect to server: " + e.getMessage());
        }
    }

    private void sendNodeInfo() {
        StringBuilder nodeInfo = new StringBuilder();
        nodeInfo.append("NODE_READY;")
               .append(node.getId());
        
        ActuatorCollection actuators = node.getActuators();
        if (actuators.size() > 0) {
            nodeInfo.append(";");
            Map<String, Integer> actuatorCounts = new HashMap<>();
            actuators.forEach(actuator -> 
                actuatorCounts.merge(actuator.getType(), 1, Integer::sum));
            
            boolean first = true;
            for (Map.Entry<String, Integer> entry : actuatorCounts.entrySet()) {
                if (!first) {
                    nodeInfo.append(",");
                }
                nodeInfo.append(entry.getValue())
                       .append("_")
                       .append(entry.getKey());
                first = false;
            }
        }
        output.println(nodeInfo);
        Logger.info("Node " + node.getId() + " sent ready notification: " + nodeInfo);
    }

    private void startListening() {
        new Thread(() -> {
            try {
                String message;
                while (isRunning && (message = input.readLine()) != null) {
                    handleMessage(message);
                }
            } catch (IOException e) {
                if (isRunning) {
                    Logger.error("Error reading from server: " + e.getMessage());
                }
            }
        }, "Client-Listener-" + node.getId()).start();
    }

    @Override
    public void sensorsUpdated(List<Sensor> sensors) {
        if (output != null) {
            String sensorData = formatSensorData(sensors);
            output.println("SENSOR_DATA;" + node.getId() + ";" + sensorData);
            Logger.info("Node " + node.getId() + " sent sensor data: " + sensorData);
        }
    }

    private String formatSensorData(List<Sensor> sensors) {
        StringBuilder sb = new StringBuilder();
        for (Sensor sensor : sensors) {
            if (sb.length() > 0) {
                sb.append(",");
            }
            sb.append(sensor.getType())
              .append("=")
              .append(sensor.getReading().getValue())
              .append(" ")
              .append(sensor.getReading().getUnit());
        }
        return sb.toString();
    }

    @Override
    public void actuatorUpdated(int nodeId, Actuator actuator) {
        if (output != null) {
            String message = String.format("ACTUATOR_STATE;%d;%d;%b", 
                nodeId, actuator.getId(), actuator.isOn());
            output.println(message);
            Logger.info("Node " + nodeId + " sent actuator update: " + message);
        }
    }

    private void handleMessage(String message) {
        String[] parts = message.split(";");
        if (parts.length >= 4 && parts[0].equals("ACTUATOR_COMMAND")) {
            try {
                int nodeId = Integer.parseInt(parts[1]);
                int actuatorId = Integer.parseInt(parts[2]);
                boolean state = Boolean.parseBoolean(parts[3]);
                if (nodeId == node.getId()) {
                    node.setActuator(actuatorId, state);
                }
            } catch (NumberFormatException e) {
                Logger.error("Invalid actuator command format: " + message);
            }
        }
    }

    public void stop() {
        isRunning = false;
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            Logger.error("Error closing client connection: " + e.getMessage());
        }
    }

    @Override
public void onNodeReady(SensorActuatorNode node) {
    Logger.info("Node " + node.getId() + " is ready.");
}

    @Override
    public void onNodeStopped(SensorActuatorNode node) {
        // Implementation for when a node stops
        Logger.info("Node " + node.getId() + " has stopped.");
    }
}