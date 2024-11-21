package no.ntnu.communication;

import java.io.*;
import java.net.Socket;
import java.util.Map;

import no.ntnu.greenhouse.SensorActuatorNode;
import no.ntnu.tools.Logger;

public class ClientHandler extends Thread {
    private final Socket clientSocket;
    private final TcpServer server;
    private final BufferedReader input;
    private final PrintWriter output;
    private final Map<Integer, SensorActuatorNode> nodes;

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
        String[] parts = message.split(";");
        if (parts.length >= 1) {
            switch (parts[0]) {
                default:
                    Logger.error("Unknown message type: " + parts[0]);
                    break;
                case "CONTROL_PANEL_CONNECT":
                    server.registerControlPanel(this);
                    break;
                case "NODE_READY":
                    server.broadcastToControlPanels(message);
                    break;
                case "SENSOR_DATA":
                    server.broadcastToControlPanels(message);
                    break;
                case "ACTUATOR_STATE":
                    server.broadcastToControlPanels(message);
                    break;
            }
        }
    }

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