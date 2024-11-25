package no.ntnu.message;

/**
 * The ControlPanelConnectMessage class represents a message that indicates a
 * control panel is connecting to the server.
 */
public class ControlPanelConnectMessage implements Message {

    /**
     * Gets the type of the message.
     *
     * @return the message type as a string
     */
    @Override
    public String getType() {
        return MessageSerializer.CONTROL_PANEL_CONNECT;
    }
}