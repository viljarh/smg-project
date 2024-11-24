package no.ntnu.run;

import no.ntnu.communication.ControlPanelTcpClient;
import no.ntnu.controlpanel.CommunicationChannel;
import no.ntnu.controlpanel.ControlPanelLogic;
import no.ntnu.controlpanel.FakeCommunicationChannel;
import no.ntnu.gui.controlpanel.ControlPanelApplication;
import no.ntnu.tools.Logger;

/**
 * Starter class for the control panel.
 * Note: we could launch the Application class directly, but then we would have issues with the
 * debugger (JavaFX modules not found)
 */
public class ControlPanelStarter {
    private final boolean fake;
    private ControlPanelTcpClient client;

    /**
     * Instantiates a new Control panel starter.
     *
     * @param fake the fake
     */
    public ControlPanelStarter(boolean fake) {
        this.fake = fake;
    }

    /**
     * The entry point of application.
     *
     * @param args the input arguments
     */
    public static void main(String[] args) {
        boolean fake = false;
        if (args.length == 1 && "fake".equals(args[0])) {
            fake = true;
            Logger.info("Using FAKE events");
        }
        ControlPanelStarter starter = new ControlPanelStarter(fake);
        starter.start();
    }

    private void start() {
        ControlPanelLogic logic = new ControlPanelLogic();
        CommunicationChannel channel = initiateCommunication(logic, fake);
        ControlPanelApplication.startApp(logic, channel);
        Logger.info("Exiting the control panel application");
        stopCommunication();
    }

    private CommunicationChannel initiateCommunication(ControlPanelLogic logic, boolean fake) {
        CommunicationChannel channel;
        if (fake) {
            channel = initiateFakeSpawner(logic);
        } else {
            channel = initiateSocketCommunication(logic);
        }
        return channel;
    }

    private CommunicationChannel initiateSocketCommunication(ControlPanelLogic logic) {
        ControlPanelTcpClient client = new ControlPanelTcpClient(logic);
        logic.setCommunicationChannel(client);
        return client;
    }

    private CommunicationChannel initiateFakeSpawner(ControlPanelLogic logic) {
        FakeCommunicationChannel spawner = new FakeCommunicationChannel(logic);
        logic.setCommunicationChannel(spawner);
        return spawner;
    }

    private void stopCommunication() {
        if (client != null) {
            Logger.info("Closing the TCP client");
            client.close();
        }
    }
}
