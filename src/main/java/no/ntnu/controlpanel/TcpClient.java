package no.ntnu.controlpanel;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import no.ntnu.greenhouse.SensorReading;

public class TcpClient implements CommunicationChannel {
  private Socket socket;
  private ObjectOutputStream outputStream;
  private ObjectInputStream inputStream;
  private ControlPanelLogic logic;
  private String serverHost;
  private int serverPort;

  public TcpClient(ControlPanelLogic logic, String serverHost, int serverPort) {
    this.logic = logic;
    this.serverHost = serverHost;
    this.serverPort = serverPort;
  }

  @Override
  public boolean open() {
    try {
      this.socket = new Socket(serverHost, serverPort);
      this.outputStream = new ObjectOutputStream(socket.getOutputStream());
      this.inputStream = new ObjectInputStream(socket.getInputStream());
      startListening();
      return true;
    } catch (IOException e) {
      e.printStackTrace();
      return false;
    }
  }

  private void startListening() {
    new Thread(
            () -> {
              try {
                while (!socket.isClosed()) {
                  String data = (String) inputStream.readObject();
                  processData(data);
                }
              } catch (IOException | ClassNotFoundException e) {
                System.err.println("Error receiving data: " + e.getMessage());
              }
            })
        .start();
  }

  private void processData(String data) {
    int nodeId = 1;
    List<SensorReading> sensorReadings = parseSensorData(data);
    logic.onSensorData(nodeId, sensorReadings);
  }

  private List<SensorReading> parseSensorData(String data) {
    List<SensorReading> readings = new ArrayList<>();
    String[] sensorEntries = data.split(",");
    for (String entry : sensorEntries) {
      String[] parts = entry.split("=");
      if (parts.length == 2) {
        String type = parts[0];
        double value = Double.parseDouble(parts[1]);
        String unit = determineUnit(type);
        readings.add(new SensorReading(type, value, unit));
      }
    }
    return readings;
  }

  private String determineUnit(String type) {
    switch (type.toLowerCase()) {
      case "temperature":
        return "C";
      case "humidity":
        return "%";
      case "light":
        return "lux";
      default:
        return "";
    }
  }

  @Override
  public void sendActuatorChange(int nodeId, int actuatorId, boolean isOn) {
    String command = "ACTUATOR_CHANGE:" + nodeId + ":" + actuatorId + "=" + isOn;
    sendCommand(command);
  }

  private void sendCommand(String command) {
    try {
      outputStream.writeObject(command);
      outputStream.flush();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void close() {
    try {
      if (socket != null) {
        socket.close();
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
