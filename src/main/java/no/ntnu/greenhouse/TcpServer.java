package no.ntnu.greenhouse;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;

public class TcpServer {
  private ServerSocket serverSocket;
  private final CopyOnWriteArrayList<Socket> clientSockets = new CopyOnWriteArrayList<>();
  private final Map<Integer, SensorActuatorNode> nodes;
  private Timer sensorDataTimer;

  public TcpServer(int port, Map<Integer, SensorActuatorNode> nodes) throws IOException {
    this.serverSocket = new ServerSocket(port);
    this.nodes = nodes;
    System.out.println("Greenhouse server started on port " + port);
    waitForClientConnection();
    startSendingSensorData();
  }

  private void waitForClientConnection() {
    new Thread(
            () -> {
              try {
                while (true) {
                  Socket clientSocket = serverSocket.accept();
                  clientSockets.add(clientSocket);
                  System.out.println("Client connected: " + clientSocket.getInetAddress());
                  startListening(clientSocket);
                }
              } catch (IOException e) {
                System.err.println("Error accepting client: " + e.getMessage());
              }
            })
        .start();
  }

  private void startListening(Socket clientSocket) {
    new Thread(
            () -> {
              try (ObjectInputStream inputStream =
                  new ObjectInputStream(clientSocket.getInputStream())) {
                while (true) {
                  String command = (String) inputStream.readObject();
                  System.out.println("Received command from client: " + command);
                  processCommand(command);
                }
              } catch (IOException | ClassNotFoundException e) {
                System.err.println("Client disconnected or error: " + e.getMessage());
                clientSockets.remove(clientSocket);
              }
            })
        .start();
  }

  private void startSendingSensorData() {
    sensorDataTimer = new Timer();
    sensorDataTimer.scheduleAtFixedRate(
        new TimerTask() {
          @Override
          public void run() {
            try {
              for (SensorActuatorNode node : nodes.values()) {
                String sensorData = formatSensorData(node);
                broadcastSensorData(sensorData);
              }
            } catch (IOException e) {
              System.err.println("Error sending sensor data to client: " + e.getMessage());
            }
          }
        },
        0,
        5000);
  }

  private String formatSensorData(SensorActuatorNode node) {
    StringBuilder data = new StringBuilder();
    data.append("Node ").append(node.getId()).append(": ");
    for (Sensor sensor : node.getSensors()) {
      data.append(sensor.getType())
          .append("=")
          .append(sensor.getReading().getFormatted())
          .append(", ");
    }
    if (data.length() > 2) {
      data.setLength(data.length() - 2);
    }
    return data.toString();
  }

  private void processCommand(String command) {
    String[] parts = command.split(":|=");
    if (parts.length == 4 && "ACTUATOR_CHANGE".equals(parts[0])) {
      int nodeId = Integer.parseInt(parts[1]);
      int actuatorId = Integer.parseInt(parts[2]);
      boolean isOn = Boolean.parseBoolean(parts[3]);

      SensorActuatorNode node = nodes.get(nodeId);
      if (node != null) {
        node.setActuator(actuatorId, isOn);
        System.out.println("Actuator " + actuatorId + " on node " + nodeId + " set to " + isOn);
      } else {
        System.err.println("Node with ID " + nodeId + " not found.");
      }
    }
  }

  private void broadcastSensorData(String data) throws IOException {
    for (Socket clientSocket : clientSockets) {
      try {
        ObjectOutputStream outputStream = new ObjectOutputStream(clientSocket.getOutputStream());
        outputStream.writeObject(data);
        outputStream.flush();
      } catch (Exception e) {
        System.err.println("Error broadcasting to client: " + clientSocket.getInetAddress());
        clientSockets.remove(clientSocket);
      }
    }
  }

  public void stop() {
    try {
      if (sensorDataTimer != null) {
        sensorDataTimer.cancel();
      }
      closeConnection();
    } catch (Exception e) {
      System.err.println("Error stopping TCP server: " + e.getMessage());
    }
  }

  private void closeConnection() {
    try {
      for (Socket clientSocket : clientSockets) {
        if (clientSocket != null) {
          clientSocket.close();
        }
      }
      if (serverSocket != null) {
        serverSocket.close();
      }
    } catch (IOException e) {
      System.err.println("Error closing server: " + e.getMessage());
    }
  }
}
