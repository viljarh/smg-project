package no.ntnu.message;

/**
 * A message that can be sent between the server and the client.
 */
public interface Message {
    String getType();
}
