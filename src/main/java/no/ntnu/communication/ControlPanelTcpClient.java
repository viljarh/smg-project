package no.ntnu.communication;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import no.ntnu.controlpanel.CommunicationChannel;
import no.ntnu.controlpanel.ControlPanelLogic;
import no.ntnu.controlpanel.SensorActuatorNodeInfo;
import no.ntnu.greenhouse.Actuator;
import no.ntnu.greenhouse.SensorReading;
import no.ntnu.tools.Logger;

public class ControlPanelTcpClient implements CommunicationChannel {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 10025;
    private Socket socket;
    private PrintWriter output;
    private BufferedReader input;
    private final ControlPanelLogic logic;
    private boolean isRunning;

    public ControlPanelTcpClient(ControlPanelLogic logic) {
        this.logic = logic;
    }

    @Override
public boolean open() {
    try {
        socket = new Socket(SERVER_HOST, SERVER_PORT);
        output = new PrintWriter(socket.getOutputStream(), true);
        input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        // Identify as control panel
        output.println("CONTROL_PANEL_CONNECT");
        isRunning = true;
        startListening();
        Logger.info("Control panel connected to server");
        return true;
    } catch (IOException e) {
        Logger.error("Could not connect to server: " + e.getMessage());
        return false;
    }
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
                    logic.onCommunicationChannelClosed();
                }
            }
        }, "ControlPanel-Listener").start();
    }

    private void handleMessage(String message) {
        String[] parts = message.split(";");
        if (parts.length >= 2) {
            switch (parts[0]) {
                case "NODE_READY":
                    // Handle node registration
                    handleNodeInfo(message.substring(message.indexOf(';') + 1));
                    break;
                case "SENSOR_DATA":
                    // Handle sensor updates
                    handleSensorData(parts);
                    break;
                case "ACTUATOR_STATE":
                    // Handle actuator state changes
                    handleActuatorState(parts);
                    break;
                case "NODE_STOPPED":
                    if (parts.length >= 2) {
                        try {
                            int nodeId = Integer.parseInt(parts[1]);
                            logic.onNodeRemoved(nodeId);
                        } catch (NumberFormatException e) {
                            Logger.error("Invalid node ID in stop message");
                        }
                    }
                    break;
                default:
                    Logger.error("Unknown message type: " + parts[0]);
            }
        }
    }

    @Override
    public void sendActuatorChange(int nodeId, int actuatorId, boolean isOn) {
        if (output != null) {
            output.println("ACTUATOR_COMMAND;" + nodeId + ";" + actuatorId + ";" + isOn);
        }
    }

    private void handleNodeInfo(String nodeInfo) {
    try {
        // Node info format: nodeId;actuatorCount_actuatorType,...
        // Example: "1;2_window,1_heater"
        String[] parts = nodeInfo.split(";");
        if (parts.length >= 1) {
            int nodeId = Integer.parseInt(parts[0]);
            SensorActuatorNodeInfo info = new SensorActuatorNodeInfo(nodeId);
            
            // If there are actuator specifications
            if (parts.length >= 2 && !parts[1].isEmpty()) {
                String[] actuators = parts[1].split(",");
                for (String actuator : actuators) {
                    String[] actuatorParts = actuator.split("_");
                    if (actuatorParts.length == 2) {
                        try {
                            int count = Integer.parseInt(actuatorParts[0]);
                            String type = actuatorParts[1];
                            // Create and add actuators of this type
                            for (int i = 0; i < count; i++) {
                                Actuator newActuator = new Actuator(type, nodeId);
                                info.addActuator(newActuator);
                            }
                        } catch (NumberFormatException e) {
                            Logger.error("Invalid actuator count format: " + actuator);
                        }
                    }
                }
            }
            Logger.info("Adding node: " + nodeId);
            logic.onNodeAdded(info);
        }
    } catch (NumberFormatException e) {
        Logger.error("Invalid node info format: " + nodeInfo);
    }
}

    private void handleSensorData(String[] parts) {
        if (parts.length >= 3) {
            try {
                int nodeId = Integer.parseInt(parts[1]);
                String[] sensorReadings = parts[2].split(",");
                List<SensorReading> readings = new ArrayList<>();
                
                for (String reading : sensorReadings) {
                    String[] readingParts = reading.split("=");
                    if (readingParts.length == 2) {
                        String type = readingParts[0];
                        String[] valueUnit = readingParts[1].split(" ");
                        if (valueUnit.length == 2) {
                            double value = Double.parseDouble(valueUnit[0]);
                            String unit = valueUnit[1];
                            readings.add(new SensorReading(type, value, unit));
                        }
                    }
                }
                
                logic.onSensorData(nodeId, readings);
            } catch (NumberFormatException e) {
                Logger.error("Invalid sensor data format");
            }
        }
    }

    private void handleActuatorState(String[] parts) {
        if (parts.length >= 4) {
            try {
                int nodeId = Integer.parseInt(parts[1]);
                int actuatorId = Integer.parseInt(parts[2]);
                boolean isOn = Boolean.parseBoolean(parts[3]);
                logic.onActuatorStateChanged(nodeId, actuatorId, isOn);
            } catch (NumberFormatException e) {
                Logger.error("Invalid actuator state format");
            }
        }
    }

    public void close() {
        isRunning = false;
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            Logger.error("Error closing connection: " + e.getMessage());
        }
    }
}