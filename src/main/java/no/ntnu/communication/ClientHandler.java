package no.ntnu.communication;

import java.io.*;
import java.net.Socket;
import java.util.Map;
import no.ntnu.greenhouse.SensorActuatorNode;
import no.ntnu.message.ActuatorCommandMessage;
import no.ntnu.message.ActuatorStateMessage;
import no.ntnu.message.ControlPanelConnectMessage;
import no.ntnu.message.ErrorMessage;
import no.ntnu.message.Message;
import no.ntnu.message.MessageSerializer;
import no.ntnu.message.NodeReadyMessage;
import no.ntnu.message.SensorDataMessage;
import no.ntnu.message.TurnOffAllActuatorsMessage;
import no.ntnu.tools.Logger;

/**
 * The ClientHandler class manages the communication between the server and a
 * connected client.
 * It handles receiving and processing messages from the client and sending
 * responses back.
 */
public class ClientHandler extends Thread {
  private final Socket clientSocket;
  private final TcpServer server;
  private final BufferedReader input;
  private final PrintWriter output;
  private final Map<Integer, SensorActuatorNode> nodes;

  /**
   * Constructs a new ClientHandler.
   *
   * @param socket the socket connected to the client
   * @param server the server instance
   * @param nodes  the map of sensor-actuator nodes
   * @throws IOException if an I/O error occurs when creating the input or output
   *                     streams
   */
  public ClientHandler(Socket socket, TcpServer server, Map<Integer, SensorActuatorNode> nodes)
      throws IOException {
    this.clientSocket = socket;
    this.server = server;
    this.nodes = nodes;
    this.input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    this.output = new PrintWriter(socket.getOutputStream(), true);
  }

  /**
   * Handles incoming client messages and processes them accordingly.
   * This method runs in a separate thread.
   */
  @Override
  public void run() {
    try {
      String message;
      while ((message = input.readLine()) != null) {
        handleMessage(message);
      }
    } catch (IOException e) {
      Logger.error("Error handling client: " + e.getMessage());
    } finally {
      closeConnection();
    }
  }

  /**
   * Handles the incoming message based on its type.
   *
   * @param message the message received from the client
   */
  private void handleMessage(String message) {
    Message msg = MessageSerializer.fromString(message);
    handleMessageByType(msg, message);
  }

  /**
   * Processes the message based on its specific type.
   *
   * @param msg             the parsed message object
   * @param originalMessage the original message string
   */
  private void handleMessageByType(Message msg, String originalMessage) {
    if (msg instanceof TurnOffAllActuatorsMessage) {
      handleTurnOffAll();
    } else if (msg instanceof NodeReadyMessage) {
      server.broadcastToControlPanels(originalMessage);
    } else if (msg instanceof SensorDataMessage) {
      server.broadcastToControlPanels(originalMessage);
    } else if (msg instanceof ActuatorStateMessage) {
      server.broadcastToControlPanels(originalMessage);
    } else if (msg instanceof ActuatorCommandMessage cmd) {
      handleActuatorCommand(cmd);
    } else if (msg instanceof ControlPanelConnectMessage) {
      server.registerControlPanel(this);
    } else if (msg instanceof ErrorMessage error) {
      handleError(error);
    } else {
      Logger.error("Unknown message type: " + msg.getType());
    }
  }

  /**
   * Handles the TurnOffAllActuatorsMessage by turning off all actuators in all
   * nodes.
   */
  private void handleTurnOffAll() {
    for (SensorActuatorNode node : nodes.values()) {
      node.setAllActuators(false);
    }
  }

  /**
   * Handles the ActuatorCommandMessage by setting the state of the specified
   * actuator.
   *
   * @param cmd the actuator command message
   */
  private void handleActuatorCommand(ActuatorCommandMessage cmd) {
    SensorActuatorNode node = nodes.get(cmd.getNodeId());
    if (node != null) {
      node.setActuator(cmd.getActuatorId(), cmd.isOn());
      Logger.info("Received actuator command: node=" + cmd.getNodeId() +
          ", actuator=" + cmd.getActuatorId() +
          ", state=" + cmd.isOn());
    }
  }

  /**
   * Handles the ErrorMessage by logging the error and broadcasting it to control
   * panels.
   *
   * @param error the error message
   */
  private void handleError(ErrorMessage error) {
    Logger.error(error.getMessage());
    server.broadcastToControlPanels(MessageSerializer.toString(error));
  }

  /**
   * Sends a message to the connected client.
   *
   * @param message the message to send
   */
  public void sendMessage(String message) {
    output.println(message);
  }

  /**
   * Closes the connection to the client.
   */
  private void closeConnection() {
    try {
      server.removeClient(this);
      clientSocket.close();
    } catch (IOException e) {
      Logger.error("Error closing client connection: " + e.getMessage());
    }
  }
}
