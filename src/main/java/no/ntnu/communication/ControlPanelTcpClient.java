package no.ntnu.communication;

import java.io.*;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import no.ntnu.controlpanel.CommunicationChannel;
import no.ntnu.controlpanel.ControlPanelLogic;
import no.ntnu.controlpanel.SensorActuatorNodeInfo;
import no.ntnu.greenhouse.Actuator;
import no.ntnu.greenhouse.SensorReading;
import no.ntnu.tools.Logger;
import no.ntnu.ssl.SslConnection;

/**
 * The ControlPanelTcpClient class manages the TCP connection between the
 * control panel and the server.
 * It handles sending and receiving messages, including actuator commands and
 * sensor data.
 */
public class ControlPanelTcpClient implements CommunicationChannel {
  private static final String SERVER_HOST = "localhost";
  private static final int SERVER_PORT = 10025;
  private Socket socket;
  private PrintWriter output;
  private BufferedReader input;
  private final ControlPanelLogic logic;
  private boolean isRunning;
  private final SslConnection sslConnection;

  /**
   * Constructs a new ControlPanelTcpClient.
   *
   * @param logic            the logic handler for the control panel
   * @param keyStorePath     the path to the keystore file for SSL connection
   * @param keyStorePassword the password for the keystore
   * @throws KeyStoreException if there is an issue with the keystore
   */
  public ControlPanelTcpClient(ControlPanelLogic logic, String keyStorePath, String keyStorePassword)
      throws KeyStoreException {
    this.logic = logic;
    this.sslConnection = new SslConnection(SERVER_PORT, keyStorePath, keyStorePassword);
  }

  /**
   * Opens the connection to the server.
   *
   * @return true if the connection is successfully opened, false otherwise
   */
  @Override
  public boolean open() {
    try {
      socket = sslConnection.createClientSocket(SERVER_HOST);
      output = new PrintWriter(socket.getOutputStream(), true);
      input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      output.println("CONTROL_PANEL_CONNECT");
      isRunning = true;
      startListening();
      Logger.info("Control panel connected to server");
      return true;
    } catch (IOException | KeyStoreException | NoSuchAlgorithmException | KeyManagementException e) {
      Logger.error("Could not connect to server: " + e.getMessage());
      return false;
    }
  }

  /**
   * Starts a new thread to listen for incoming messages from the server.
   */
  private void startListening() {
    new Thread(
        () -> {
          try {
            String message;
            while (isRunning && (message = input.readLine()) != null) {
              handleMessage(message);
            }
          } catch (IOException e) {
            if (isRunning) {
              Logger.error("Error reading from server: " + e.getMessage());
              logic.onCommunicationChannelClosed();
            }
          }
        },
        "ControlPanel-Listener")
        .start();
  }

  /**
   * Handles incoming messages from the server.
   *
   * @param message the message received from the server
   */
  private void handleMessage(String message) {
    String[] parts = message.split(";");
    if (parts.length >= 2) {
      switch (parts[0]) {
        case "NODE_READY":
          handleNodeInfo(message.substring(message.indexOf(';') + 1));
          break;
        case "SENSOR_DATA":
          handleSensorData(parts);
          break;
        case "ACTUATOR_STATE":
          handleActuatorState(parts);
          break;
        case "NODE_STOPPED":
          if (parts.length >= 2) {
            try {
              int nodeId = Integer.parseInt(parts[1]);
              logic.onNodeRemoved(nodeId);
            } catch (NumberFormatException e) {
              Logger.error("Invalid node ID in stop message");
            }
          }
          break;
        default:
          Logger.error("Unknown message type: " + parts[0]);
      }
    }
  }

  /**
   * Sends an actuator change command to the server.
   *
   * @param nodeId     the ID of the node
   * @param actuatorId the ID of the actuator
   * @param isOn       true if the actuator is to be turned on, false otherwise
   */
  @Override
  public void sendActuatorChange(int nodeId, int actuatorId, boolean isOn) {
    if (output != null) {
      String command = "ACTUATOR_COMMAND;" + nodeId + ";" + actuatorId + ";" + isOn;
      output.println(command);
      Logger.info("Control panel sending command: " + command);
    } else {
      Logger.error("Cannot send actuator command - no connection to server");
    }
  }

  public void sendTurnOffAllActuators() {
    if (output != null) {
        output.println("TURN_OFF_ALL");
        Logger.info("Control panel sending turn off all command");
    }
}

  /**
   * Handles the node information message received from the server.
   *
   * @param nodeInfo the information about the node in the message
   */
  private void handleNodeInfo(String nodeInfo) {
    try {
      String[] parts = nodeInfo.split(";");
      if (parts.length >= 1) {
        int nodeId = Integer.parseInt(parts[0]);
        SensorActuatorNodeInfo info = new SensorActuatorNodeInfo(nodeId);

        if (parts.length >= 2 && !parts[1].isEmpty()) {
          String[] actuators = parts[1].split(",");
          int baseId = 1;
          for (String actuator : actuators) {
            String[] actuatorParts = actuator.split("_");
            if (actuatorParts.length == 2) {
              try {
                int count = Integer.parseInt(actuatorParts[0]);
                String type = actuatorParts[1];
                for (int i = 0; i < count; i++) {
                  int actuatorId;
                  switch (type) {
                    case "window":
                      actuatorId = 2;
                      break;
                    case "fan":
                      actuatorId = 4 + i;
                      break;
                    case "heater":
                      actuatorId = 7;
                      break;
                    default:
                      actuatorId = baseId++;
                  }
                  Actuator newActuator = new Actuator(actuatorId, type, nodeId);
                  newActuator.setListener(logic);
                  info.addActuator(newActuator);
                }
              } catch (NumberFormatException e) {
                Logger.error("Invalid actuator count format: " + actuator);
              }
            }
          }
        }
        Logger.info("Adding node: " + nodeId);
        logic.onNodeAdded(info);
      }
    } catch (NumberFormatException e) {
      Logger.error("Invalid node info format: " + nodeInfo);
    }
  }

  /**
   * Handle sensor data messages received from the server.
   *
   * @param parts the parts of the message received
   */
  private void handleSensorData(String[] parts) {
    if (parts.length >= 3) {
      try {
        int nodeId = Integer.parseInt(parts[1]);
        String[] sensorReadings = parts[2].split(",");
        List<SensorReading> readings = new ArrayList<>();

        for (String reading : sensorReadings) {
          String[] readingParts = reading.split("=");
          if (readingParts.length == 2) {
            String type = readingParts[0];
            String[] valueUnit = readingParts[1].split(" ");
            if (valueUnit.length == 2) {
              double value = Double.parseDouble(valueUnit[0]);
              String unit = valueUnit[1];
              readings.add(new SensorReading(type, value, unit));
            }
          }
        }

        logic.onSensorData(nodeId, readings);
      } catch (NumberFormatException e) {
        Logger.error("Invalid sensor data format");
      }
    }
  }

  /**
   * Handle actuator state messages received from the server.
   *
   * @param parts the parts of the message received
   */
  private void handleActuatorState(String[] parts) {
    if (parts.length >= 4) {
      try {
        int nodeId = Integer.parseInt(parts[1]);
        int actuatorId = Integer.parseInt(parts[2]);
        boolean isOn = Boolean.parseBoolean(parts[3]);
        logic.onActuatorStateChanged(nodeId, actuatorId, isOn);
      } catch (NumberFormatException e) {
        Logger.error("Invalid actuator state format");
      }
    }
  }

  /**
   * Closes the connection to the server.
   */
  public void close() {
    isRunning = false;
    try {
      if (socket != null && !socket.isClosed()) {
        socket.close();
      }
    } catch (IOException e) {
      Logger.error("Error closing connection: " + e.getMessage());
    }
  }
}
