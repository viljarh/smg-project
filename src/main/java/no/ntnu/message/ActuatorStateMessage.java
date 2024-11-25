package no.ntnu.message;

/**
 * Message sent from a node to the server to update the state of an actuator.
 */
public class ActuatorStateMessage implements Message {
    private final int nodeId;
    private final int actuatorId;
    private final boolean isOn;

    public ActuatorStateMessage(int nodeId, int actuatorId, boolean isOn) {
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
        return MessageSerializer.ACTUATOR_STATE;
    }
}