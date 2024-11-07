package no.ntnu.controlpanel;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class TcpChannel {
    private Socket socket;
    private ObjectOutputStream outputStream;
    private ObjectInputStream inputStream;
    private ControlPanelLogic logic;

    public TcpChannel(ControlPanelLogic logic, String serverHost, int serverPort) throws IOException {
        this.logic = logic;
        this.socket = new Socket(serverHost, serverPort);
        this.outputStream = new ObjectOutputStream(socket.getOutputStream());
        this.inputStream = new ObjectInputStream(socket.getInputStream());
        startListening();
    }

    private void startListening(){
        new Thread(() -> {
            try {
               while (!socket.isClosed()){
                String data = (String) inputStream.readObject();
                processData(data);
               } 
            } catch (IOException e | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void processData(String data){
        logic.updateSensorData(data);   
    }
}
