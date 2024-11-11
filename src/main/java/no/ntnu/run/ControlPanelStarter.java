package no.ntnu.run;

import no.ntnu.controlpanel.CommunicationChannel;
import no.ntnu.controlpanel.ControlPanelLogic;
import no.ntnu.controlpanel.FakeCommunicationChannel;
import no.ntnu.controlpanel.TcpClient;
import no.ntnu.gui.controlpanel.ControlPanelApplication;
import no.ntnu.tools.Logger;

/**
 * Starter class for the control panel. Note: we could launch the Application class directly, but
 * then we would have issues with the debugger (JavaFX modules not found)
 */
public class ControlPanelStarter {
  private final boolean fake;
  private CommunicationChannel communicationChannel;

  public ControlPanelStarter(boolean fake) {
    this.fake = fake;
  }

  /**
   * Entrypoint for the application.
   *
   * @param args Command line arguments, only the first one of them used: when it is "fake", emulate
   *     fake events, when it is either something else or not present, use real socket
   *     communication. Go to Run → Edit Configurations. Add "fake" to the Program Arguments field.
   *     Apply the changes.
   */
  public static void main(String[] args) {
    boolean fake = false; // make it true to test in fake mode
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
    // This code is reached only after the GUI-window is closed
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
    TcpClient tcpClient = new TcpClient(logic, "localhost", 3000);

    if (tcpClient.open()) {
      Logger.info("TCP connection established");
      logic.setCommunicationChannel(tcpClient);
      return tcpClient;
    } else {
      Logger.error("Failed to establish TCP connection");
      return null;
    }
  }

  private CommunicationChannel initiateFakeSpawner(ControlPanelLogic logic) {
    // Here we pretend that some events will be received with a given delay
    FakeCommunicationChannel spawner = new FakeCommunicationChannel(logic);
    logic.setCommunicationChannel(spawner);
    final int START_DELAY = 5;
    spawner.spawnNode("4;3_window", START_DELAY);
    spawner.spawnNode("1", START_DELAY + 1);
    spawner.spawnNode("1", START_DELAY + 2);
    spawner.advertiseSensorData(
        "4;temperature=27.4 °C,temperature=26.8 °C,humidity=80 %", START_DELAY + 2);
    spawner.spawnNode("8;2_heater", START_DELAY + 3);
    spawner.advertiseActuatorState(4, 1, true, START_DELAY + 3);
    spawner.advertiseActuatorState(4, 1, false, START_DELAY + 4);
    spawner.advertiseActuatorState(4, 1, true, START_DELAY + 5);
    spawner.advertiseActuatorState(4, 2, true, START_DELAY + 5);
    spawner.advertiseActuatorState(4, 1, false, START_DELAY + 6);
    spawner.advertiseActuatorState(4, 2, false, START_DELAY + 6);
    spawner.advertiseActuatorState(4, 1, true, START_DELAY + 7);
    spawner.advertiseActuatorState(4, 2, true, START_DELAY + 8);
    spawner.advertiseSensorData(
        "4;temperature=22.4 °C,temperature=26.0 °C,humidity=81 %", START_DELAY + 9);
    spawner.advertiseSensorData("1;humidity=80 %,humidity=82 %", START_DELAY + 10);
    spawner.advertiseRemovedNode(8, START_DELAY + 11);
    spawner.advertiseRemovedNode(8, START_DELAY + 12);
    spawner.advertiseSensorData(
        "1;temperature=25.4 °C,temperature=27.0 °C,humidity=67 %", START_DELAY + 13);
    spawner.advertiseSensorData(
        "4;temperature=25.4 °C,temperature=27.0 °C,humidity=82 %", START_DELAY + 14);
    spawner.advertiseSensorData(
        "4;temperature=25.4 °C,temperature=27.0 °C,humidity=82 %", START_DELAY + 16);
    return spawner;
  }

  private void stopCommunication() {
    if (communicationChannel instanceof TcpClient) {
      ((TcpClient) communicationChannel).close();
      Logger.info("TCP connection closed");
    } else if (communicationChannel instanceof FakeCommunicationChannel) {
      Logger.info("Stopped fake communication channel");
      // TODO: handle fake communication stop
    }
  }
}
