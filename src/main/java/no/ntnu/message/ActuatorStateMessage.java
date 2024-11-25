package no.ntnu.message;

/**
 * The ActuatorStateMessage class represents a message that contains the state
 * of an actuator.
 * It includes the node ID, actuator ID, and the current state (on or off) of
 * the actuator.
 */
public class ActuatorStateMessage implements Message {
    private final int nodeId;
    private final int actuatorId;
    private final boolean isOn;

    /**
     * Constructs a new ActuatorStateMessage.
     *
     * @param nodeId     the ID of the node that contains the actuator
     * @param actuatorId the ID of the actuator
     * @param isOn       the current state of the actuator (true for on, false for
     *                   off)
     */
    public ActuatorStateMessage(int nodeId, int actuatorId, boolean isOn) {
        this.nodeId = nodeId;
        this.actuatorId = actuatorId;
        this.isOn = isOn;
    }

    /**
     * Gets the ID of the node that contains the actuator.
     *
     * @return the node ID
     */
    public int getNodeId() {
        return nodeId;
    }

    /**
     * Gets the ID of the actuator.
     *
     * @return the actuator ID
     */
    public int getActuatorId() {
        return actuatorId;
    }

    /**
     * Gets the current state of the actuator.
     *
     * @return true if the actuator is on, false if it is off
     */
    public boolean isOn() {
        return isOn;
    }

    /**
     * Gets the type of the message.
     *
     * @return the message type as a string
     */
    @Override
    public String getType() {
        return MessageSerializer.ACTUATOR_STATE;
    }
}