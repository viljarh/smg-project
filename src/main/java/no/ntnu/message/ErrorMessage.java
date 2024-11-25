package no.ntnu.message;

/**
 * The ErrorMessage class represents an error message after a command execution
 * which failed.
 */
public class ErrorMessage implements Message {
    private final String errorMessage;

    /**
     * Constructs a new ErrorMessage.
     *
     * @param message the error message
     */
    public ErrorMessage(String message) {
        this.errorMessage = message;
    }

    /**
     * Gets the error message.
     *
     * @return the human-readable error message
     */
    public String getMessage() {
        return errorMessage;
    }

    /**
     * Gets the type of the message.
     *
     * @return the message type as a string
     */
    @Override
    public String getType() {
        return MessageSerializer.ERROR;
    }
}