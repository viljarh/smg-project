package no.ntnu.message;

/**
 * An error message after a command execution which failed.
 */
public class ErrorMessage implements Message {
    private final String errorMessage;

    public ErrorMessage(String message) {
        this.errorMessage = message;
    }

    /**
     * Get the error message.
     *
     * @return Human-readable error message
     */
    public String getMessage() {
        return errorMessage;
    }

    @Override
    public String getType() {
        return MessageSerializer.ERROR;
    }
}