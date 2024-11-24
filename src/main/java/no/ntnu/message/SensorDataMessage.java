package no.ntnu.message;

public class SensorDataMessage implements Message {
    private final int nodeId;
    private final String sensorData;
    
    public SensorDataMessage(int nodeId, String sensorData) {
        this.nodeId = nodeId;
        this.sensorData = sensorData;
    }
    
    public int getNodeId() { return nodeId; }
    public String getSensorData() { return sensorData; }

    @Override
    public String getType() {
        return MessageSerializer.SENSOR_DATA;
    }
}