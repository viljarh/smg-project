package no.ntnu.run;

import javafx.application.*;

import no.ntnu.gui.greenhouse.GreenhouseApplication;
import no.ntnu.tools.Logger;

/**
 * Starter for GUI version of the greenhouse simulator.
 */
public class GreenhouseGuiStarter {
  /**
   * Entrypoint gor the Greenhouse GUI application.
   *
   * @param args Command line arguments, only the first one of them used: when it is "fake",
   *             emulate fake events, when it is either something else or not present,
   *             use real socket communication.
   */
  public static void main(String[] args) {
    Logger.info("Starting GreenhouseGuiStarter...");
    boolean fake = false;
    if (args.length == 1 && "fake".equals(args[0])) {
        fake = true;
        Logger.info("Using FAKE events");
    }
    Logger.info("Launching GreenhouseApplication...");
    GreenhouseApplication.startApp(fake);
}
}
