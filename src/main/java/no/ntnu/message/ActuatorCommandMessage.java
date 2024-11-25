package no.ntnu.message;

/**
 * Message sent to the server to turn an actuator on or off.
 */
public class ActuatorCommandMessage implements Message {
    private final int nodeId;
    private final int actuatorId;
    private final boolean isOn;

    public ActuatorCommandMessage(int nodeId, int actuatorId, boolean isOn) {
        this.nodeId = nodeId;
        this.actuatorId = actuatorId;
        this.isOn = isOn;
    }

    public int getNodeId() {
        return nodeId;
    }

    public int getActuatorId() {
        return actuatorId;
    }

    public boolean isOn() {
        return isOn;
    }

    @Override
    public String getType() {
        return MessageSerializer.ACTUATOR_COMMAND;
    }
}