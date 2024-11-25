package no.ntnu.message;

/**
 * The ActuatorCommandMessage class represents a message that contains a command
 * for an actuator.
 * It includes the node ID, actuator ID, and the desired state (on or off) of
 * the actuator.
 */
public class ActuatorCommandMessage implements Message {
    private final int nodeId;
    private final int actuatorId;
    private final boolean isOn;

    /**
     * Constructs a new ActuatorCommandMessage.
     *
     * @param nodeId     the ID of the node that contains the actuator
     * @param actuatorId the ID of the actuator to be controlled
     * @param isOn       the desired state of the actuator (true for on, false for
     *                   off)
     */
    public ActuatorCommandMessage(int nodeId, int actuatorId, boolean isOn) {
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
     * Gets the ID of the actuator to be controlled.
     *
     * @return the actuator ID
     */
    public int getActuatorId() {
        return actuatorId;
    }

    /**
     * Gets the desired state of the actuator.
     *
     * @return true if the actuator should be turned on, false if it should be
     *         turned off
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
        return MessageSerializer.ACTUATOR_COMMAND;
    }
}