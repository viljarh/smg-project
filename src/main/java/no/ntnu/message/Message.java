package no.ntnu.message;

/**
 * A message that can be sent between the server and the client.
 */
public interface Message {

    /**
     * Gets the type of the message.
     *
     * @return the message type as a string
     */
    String getType();
}
