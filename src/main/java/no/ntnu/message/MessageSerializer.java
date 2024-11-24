package no.ntnu.message;

/**
 * Serializes messages to protocol-defined strings and vice versa.
 */
public class MessageSerializer {
    public static final String CONTROL_PANEL_CONNECT = "CONTROL_PANEL_CONNECT";
    public static final String NODE_READY = "NODE_READY";
    public static final String SENSOR_DATA = "SENSOR_DATA";
    public static final String ACTUATOR_STATE = "ACTUATOR_STATE";
    public static final String ACTUATOR_COMMAND = "ACTUATOR_COMMAND";
    public static final String NODE_STOPPED = "NODE_STOPPED";
    public static final String ERROR = "ERROR";

    /**
     * Not allowed to instantiate this utility class.
     */
    private MessageSerializer() {
    }

    /**
     * Create message from a string, according to the communication protocol.
     *
     * @param s The string sent over the communication channel
     * @return The logical message, as interpreted according to the protocol
     */
    public static Message fromString(String s) {
        try {
            if (s == null || s.isEmpty()) {
                return new ErrorMessage("Empty message received");
            }

            if (s.equals(CONTROL_PANEL_CONNECT)) {
                return new ControlPanelConnectMessage();
            }

            String[] parts = s.split(";");
            if (parts.length < 1) {
                return new ErrorMessage("Invalid message format");
            }

            return switch (parts[0]) {
                case NODE_READY -> parseNodeReady(parts);
                case SENSOR_DATA -> parseSensorData(parts);
                case ACTUATOR_COMMAND -> parseActuatorCommand(parts);
                case ACTUATOR_STATE -> parseActuatorState(parts);
                default -> new ErrorMessage("Unknown message type: " + parts[0]);
            };
        } catch (NumberFormatException e) {
            return new ErrorMessage("Invalid number format: " + e.getMessage());
        }
    }

    private static Message parseNodeReady(String[] parts) {
        if (parts.length < 2) {
            return new ErrorMessage("Invalid NODE_READY format");
        }
        return new NodeReadyMessage(parts[1]);
    }

    private static Message parseSensorData(String[] parts) {
        if (parts.length < 3) {
            return new ErrorMessage("Invalid SENSOR_DATA format");
        }
        try {
            return new SensorDataMessage(
                    Integer.parseInt(parts[1]),
                    parts[2]);
        } catch (NumberFormatException e) {
            return new ErrorMessage("Invalid node ID in sensor data");
        }
    }

    private static Message parseActuatorCommand(String[] parts) {
        if (parts.length < 4) {
            return new ErrorMessage("Invalid ACTUATOR_COMMAND format");
        }
        try {
            return new ActuatorCommandMessage(
                    Integer.parseInt(parts[1]),
                    Integer.parseInt(parts[2]),
                    Boolean.parseBoolean(parts[3]));
        } catch (NumberFormatException e) {
            return new ErrorMessage("Invalid ID in actuator command");
        }
    }

    public static Message parseActuatorState(String[] parts) {
        if (parts.length < 4) {
            return new ErrorMessage("Invalid actuator state message format");
        }
        try {
            int nodeId = Integer.parseInt(parts[1]);
            int actuatorId = Integer.parseInt(parts[2]);
            boolean isOn = Boolean.parseBoolean(parts[3]);
            return new ActuatorStateMessage(nodeId, actuatorId, isOn);
        } catch (NumberFormatException e) {
            return new ErrorMessage("Invalid number in actuator state message");
        }
    }

    /**
     * Convert a message to a serialized string.
     *
     * @param m The message to translate
     * @return String representation of the message
     */
    public static String toString(Message m) {
        if (m instanceof NodeReadyMessage msg) {
            return NODE_READY + ";" + msg.getNodeInfo();
        } else if (m instanceof SensorDataMessage msg) {
            return SENSOR_DATA + ";" + msg.getNodeId() + ";" + msg.getSensorData();
        } else if (m instanceof ActuatorCommandMessage msg) {
            return ACTUATOR_COMMAND + ";" + msg.getNodeId() + ";" +
                    msg.getActuatorId() + ";" + msg.isOn();
        } else if (m instanceof ActuatorStateMessage msg) {
            return ACTUATOR_STATE + ";" + msg.getNodeId() + ";" +
                    msg.getActuatorId() + ";" + msg.isOn();
        } else if (m instanceof ErrorMessage msg) {
            return ERROR + ";" + msg.getMessage();
        }
        return null;
    }
}