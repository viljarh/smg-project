package no.ntnu.message;

/**
 * Message sent to the server to turn off all actuators.
 */
public class TurnOffAllActuatorsMessage implements Message {
    
    @Override
    public String getType() {
        return MessageSerializer.TURN_OFF_ALL;
    }
}