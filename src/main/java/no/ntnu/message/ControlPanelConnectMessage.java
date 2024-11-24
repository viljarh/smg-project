package no.ntnu.message;

public class ControlPanelConnectMessage implements Message {
    @Override
    public String getType() {
        return MessageSerializer.CONTROL_PANEL_CONNECT;
    }
}