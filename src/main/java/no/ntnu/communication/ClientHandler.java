package no.ntnu.communication;

import java.io.*;
import java.net.Socket;
import java.util.Map;

import no.ntnu.greenhouse.SensorActuatorNode;
import no.ntnu.message.ActuatorCommandMessage;
import no.ntnu.message.ActuatorStateMessage;
import no.ntnu.message.ErrorMessage;
import no.ntnu.message.Message;
import no.ntnu.message.MessageSerializer;
import no.ntnu.message.NodeReadyMessage;
import no.ntnu.message.SensorDataMessage;
import no.ntnu.tools.Logger;

/**
 * The type Client handler.
 */
public class ClientHandler extends Thread {
    private final Socket clientSocket;
    private final TcpServer server;
    private final BufferedReader input;
    private final PrintWriter output;
    private final Map<Integer, SensorActuatorNode> nodes;

    /**
     * Instantiates a new Client handler.
     *
     * @param socket the socket
     * @param server the server
     * @param nodes  the nodes
     * @throws IOException the io exception
     */
    public ClientHandler(Socket socket, TcpServer server, Map<Integer, SensorActuatorNode> nodes) throws IOException {
        this.clientSocket = socket;
        this.server = server;
        this.nodes = nodes;
        this.input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.output = new PrintWriter(socket.getOutputStream(), true);
    }

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

    private void handleMessage(String message) {
        Message msg = MessageSerializer.fromString(message);
        if (msg instanceof NodeReadyMessage) {
            server.broadcastToControlPanels(message);
        } else if (msg instanceof SensorDataMessage) {
            server.broadcastToControlPanels(message);
        } else if (msg instanceof ActuatorStateMessage) {
            server.broadcastToControlPanels(message);
        } else if (msg instanceof ActuatorCommandMessage cmd) {
            SensorActuatorNode node = nodes.get(cmd.getNodeId());
            if (node != null) {
                node.setActuator(cmd.getActuatorId(), cmd.isOn());
                Logger.info("Received actuator command: node=" + cmd.isOn());
            }
        } else if (message.equals(MessageSerializer.CONTROL_PANEL_CONNECT)) {
            server.registerControlPanel(this);
        } else if (msg instanceof ErrorMessage error) {
            Logger.error(error.getMessage());
            server.broadcastToControlPanels(MessageSerializer.toString(error));
        }
    }

    /**
     * Send message.
     *
     * @param message the message
     */
    public void sendMessage(String message) {
        output.println(message);
    }

    private void closeConnection() {
        try {
            server.removeClient(this);
            clientSocket.close();
        } catch (IOException e) {
            Logger.error("Error closing client connection: " + e.getMessage());
        }
    }
}