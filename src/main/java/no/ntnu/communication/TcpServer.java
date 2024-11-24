package no.ntnu.communication;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import no.ntnu.greenhouse.ActuatorCollection;
import no.ntnu.greenhouse.SensorActuatorNode;
import no.ntnu.tools.Logger;

/** The type Tcp server. */
public class TcpServer {
  public static final int PORT_NUMBER = 10025;
  private boolean isServerRunning;
  private final List<ClientHandler> controlPanelClients = new ArrayList<>();
  private final Map<Integer, SensorActuatorNode> nodes;
  private final List<ClientHandler> connectedClients = new ArrayList<>();
  private ServerSocket serverSocket;

  /**
   * Instantiates a new Tcp server.
   *
   * @param nodes the nodes
   */
  public TcpServer(Map<Integer, SensorActuatorNode> nodes) {
    this.nodes = nodes;
  }

  /** Start server. Initializes the server socket and listens for incoming client connections. */
  public void startServer() {
    try {
      serverSocket = new ServerSocket(PORT_NUMBER);
      isServerRunning = true;
      Logger.info("Server listening on port " + PORT_NUMBER);

      while (isServerRunning) {
        acceptNextClient();
      }
    } catch (IOException e) {
      Logger.error("Could not start server: " + e.getMessage());
    }
  }

  /** Accepts the next client connection and starts a new client handler for it. */
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
   * Gets nodes.
   *
   * @return the nodes
   */
  public Map<Integer, SensorActuatorNode> getNodes() {
    return nodes;
  }

  /** Stop server. Stops the server and disconnects all connected clients. */
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
   * Register control panel. Adds the given client to the control panel clients list and sends
   * information about all nodes.
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
        if (!first) info.append(",");
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
   * Remove client. Removes the given client handler from the list of connected clients.
   *
   * @param clientHandler the client handler to remove
   */
  public void removeClient(ClientHandler clientHandler) {
    connectedClients.remove(clientHandler);
  }
}

