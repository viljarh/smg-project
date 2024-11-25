package no.ntnu.message;

/**
 * The SensorDataMessage class represents a message that contains sensor data.
 */
public class SensorDataMessage implements Message {
    private final int nodeId;
    private final String sensorData;

    /**
     * Constructs a new SensorDataMessage.
     *
     * @param nodeId     the ID of the node that contains the sensor
     * @param sensorData the sensor data as a string
     */
    public SensorDataMessage(int nodeId, String sensorData) {
        this.nodeId = nodeId;
        this.sensorData = sensorData;
    }

    /**
     * Gets the ID of the node that contains the sensor.
     *
     * @return the node ID
     */
    public int getNodeId() {
        return nodeId;
    }

    /**
     * Gets the sensor data.
     *
     * @return the sensor data as a string
     */
    public String getSensorData() {
        return sensorData;
    }

    /**
     * Gets the type of the message.
     *
     * @return the message type as a string
     */
    @Override
    public String getType() {
        return MessageSerializer.SENSOR_DATA;
    }
}