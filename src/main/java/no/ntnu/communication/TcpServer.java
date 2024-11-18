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

public class TcpServer {
    public static final int PORT_NUMBER = 10025;
    private boolean isServerRunning;
    private final List<ClientHandler> nodeClients = new ArrayList<>();
    private final List<ClientHandler> controlPanelClients = new ArrayList<>();
    private final Map<Integer, SensorActuatorNode> nodes;
    private final List<ClientHandler> connectedClients = new ArrayList<>();
    private ServerSocket serverSocket;

    public TcpServer(Map<Integer, SensorActuatorNode> nodes) {
        this.nodes = nodes;
    }

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

    public Map<Integer, SensorActuatorNode> getNodes() {
        return nodes;
    }

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

    public void registerControlPanel(ClientHandler client) {
        controlPanelClients.add(client);
        // Send existing node information
        for (SensorActuatorNode node : nodes.values()) {
            String nodeInfo = formatNodeInfo(node);
            client.sendMessage("NODE_READY;" + nodeInfo);
        }
    }

    private String formatNodeInfo(SensorActuatorNode node) {
        StringBuilder info = new StringBuilder();
        info.append(node.getId());
        
        ActuatorCollection actuators = node.getActuators();
        if (actuators.size() > 0) {
            info.append(";");
            Map<String, Integer> actuatorCounts = new HashMap<>();
            actuators.forEach(actuator -> 
                actuatorCounts.merge(actuator.getType(), 1, Integer::sum));
            
            boolean first = true;
            for (Map.Entry<String, Integer> entry : actuatorCounts.entrySet()) {
                if (!first) info.append(",");
                info.append(entry.getValue())
                    .append("_")
                    .append(entry.getKey());
                first = false;
            }
        }
        return info.toString();
    }

    public void broadcastToControlPanels(String message) {
        for (ClientHandler client : controlPanelClients) {
            client.sendMessage(message);
        }
    }

    public void removeClient(ClientHandler clientHandler) {
        connectedClients.remove(clientHandler);
    }
}