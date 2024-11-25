package no.ntnu.communication;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import no.ntnu.greenhouse.ActuatorCollection;
import no.ntnu.greenhouse.SensorActuatorNode;
import no.ntnu.tools.Logger;
import no.ntnu.ssl.SslConnection;

/**
 * The TcpServer class manages the TCP server that handles connections from
 * sensor-actuator nodes and control panels.
 * It handles accepting client connections, managing connected clients, and
 * broadcasting messages to control panels.
 */
public class TcpServer {
  public static final int PORT_NUMBER = 10025;
  private boolean isServerRunning;
  private final List<ClientHandler> controlPanelClients = new ArrayList<>();
  private final Map<Integer, SensorActuatorNode> nodes;
  private final List<ClientHandler> connectedClients = new ArrayList<>();
  private ServerSocket serverSocket;
  private final SslConnection sslConnection;

  /**
   * Instantiates a new TcpServer.
   *
   * @param nodes            the map of sensor-actuator nodes
   * @param keyStorePath     the path to the keystore file
   * @param keyStorePassword the password for the keystore
   * @throws KeyStoreException if there is an issue with the keystore
   */
  public TcpServer(Map<Integer, SensorActuatorNode> nodes, String keyStorePath, String keyStorePassword)
      throws KeyStoreException {
    this.nodes = nodes;
    this.sslConnection = new SslConnection(PORT_NUMBER, keyStorePath, keyStorePassword);
  }

  /**
   * Starts the server and begins listening for client connections.
   */
  public void startServer() {
    try {
      serverSocket = sslConnection.createServerSocket();
      isServerRunning = true;
      Logger.info("Server listening on port " + PORT_NUMBER);

      while (isServerRunning) {
        acceptNextClient();
      }
    } catch (IOException | KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException
        | KeyManagementException e) {
      Logger.error("Could not start server: " + e.getMessage());
    }
  }

  /**
   * Accepts the next client connection and starts a new client handler for it.
   */
  private void acceptNextClient() {
    try {
      Socket clientSocket = serverSocket.accept();
      ClientHandler clientHandler = new ClientHandler(clientSocket, this, nodes);
      connectedClients.add(clientHandler);
      clientHandler.start();
      Logger.info("New client connected from " + clientSocket.getRemoteSocketAddress());
    } catch (IOException e) {
      if (isServerRunning) {
        Logger.error("Error accepting client: " + e.getMessage());
      }
    }
  }

  /**
   * Gets the map of sensor-actuator nodes.
   *
   * @return the map of sensor-actuator nodes
   */
  public Map<Integer, SensorActuatorNode> getNodes() {
    return nodes;
  }

  /**
   * Stops the server and disconnects all connected clients.
   */
  public void stopServer() {
    isServerRunning = false;
    try {
      if (serverSocket != null && !serverSocket.isClosed()) {
        serverSocket.close();
      }
      connectedClients.clear();
    } catch (IOException e) {
      Logger.error("Error closing server: " + e.getMessage());
    }
  }

  /**
   * Registers a control panel client and sends information about all nodes to it.
   *
   * @param client the client to register as a control panel
   */
  public void registerControlPanel(ClientHandler client) {
    controlPanelClients.add(client);
    for (SensorActuatorNode node : nodes.values()) {
      String nodeInfo = formatNodeInfo(node);
      client.sendMessage("NODE_READY;" + nodeInfo);
    }
  }

  /**
   * Formats the information about a sensor actuator node.
   *
   * @param node the sensor actuator node to format
   * @return formatted node information as a string
   */
  private String formatNodeInfo(SensorActuatorNode node) {
    StringBuilder info = new StringBuilder();
    info.append(node.getId());

    ActuatorCollection actuators = node.getActuators();
    if (actuators.size() > 0) {
      info.append(";");
      Map<String, Integer> actuatorCounts = new HashMap<>();
      actuators.forEach(actuator -> actuatorCounts.merge(actuator.getType(), 1, Integer::sum));

      boolean first = true;
      for (Map.Entry<String, Integer> entry : actuatorCounts.entrySet()) {
        if (!first)
          info.append(",");
        info.append(entry.getValue()).append("_").append(entry.getKey());
        first = false;
      }
    }
    return info.toString();
  }

  /**
   * Broadcasts a message to all registered control panel clients.
   *
   * @param message the message to broadcast
   */
  public void broadcastToControlPanels(String message) {
    for (ClientHandler client : controlPanelClients) {
      client.sendMessage(message);
    }
  }

  /**
   * Remove client. Removes the given client handler from the list of connected
   * clients.
   *
   * @param clientHandler the client handler to remove
   */
  public void removeClient(ClientHandler clientHandler) {
    connectedClients.remove(clientHandler);
  }
}
