package no.ntnu.controlpanel;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import no.ntnu.greenhouse.SensorReading;

/**
 * The type Tcp client.
 */
public class TcpClient implements CommunicationChannel {
  private Socket socket;
  private ObjectOutputStream outputStream;
  private ObjectInputStream inputStream;
  private ControlPanelLogic logic;
  private String serverHost;
  private int serverPort;

  /**
   * Instantiates a new Tcp client.
   *
   * @param logic      the logic
   * @param serverHost the server host
   * @param serverPort the server port
   */
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
              while (true) {
                try {
                  if (socket == null || socket.isClosed()) {
                    System.err.println("Connection lost. Attempting to reconnect.");
                    boolean connected = open();
                    if (!connected) {
                      Thread.sleep(5000);
                      continue;
                    }
                  }
                  String data = (String) inputStream.readObject();
                  processData(data);
                } catch (IOException | ClassNotFoundException | InterruptedException e) {
                  System.err.println("Error receiving data or reconnecting: " + e.getMessage());
                }
              }
            })
        .start();
  }

  private void processData(String data) {
    // int nodeId = 1;
    // List<SensorReading> sensorReadings = parseSensorData(data);
    // logic.onSensorData(nodeId, sensorReadings);
    String[] mainParts = data.split(": ");
    if (mainParts.length == 2) {
      try {
        int nodeId = Integer.parseInt(mainParts[0].replace("Node ", "").trim());
        List<SensorReading> sensorReadings = parseSensorData(mainParts[1]);
        logic.onSensorData(nodeId, sensorReadings);
      } catch (NumberFormatException e) {
        System.err.println("Error parsing node ID from data: " + data);
      }
    } else {
      System.err.println("Unexpected data format: " + data);
    }
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

  /**
   * Close.
   */
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
