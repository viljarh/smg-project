package no.ntnu.message;

/**
 * Message sent from the control panel to the server to connect.
 */
public class ControlPanelConnectMessage implements Message {
    @Override
    public String getType() {
        return MessageSerializer.CONTROL_PANEL_CONNECT;
    }
}