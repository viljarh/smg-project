package no.ntnu.message;

/**
 * The NodeReadyMessage class represents a message that indicates a node is
 * ready.
 */
public class NodeReadyMessage implements Message {
    private final String nodeInfo;

    /**
     * Constructs a new NodeReadyMessage.
     *
     * @param nodeInfo the information about the node
     */
    public NodeReadyMessage(String nodeInfo) {
        this.nodeInfo = nodeInfo;
    }

    /**
     * Gets the information about the node.
     *
     * @return the node information
     */
    public String getNodeInfo() {
        return nodeInfo;
    }

    /**
     * Gets the type of the message.
     *
     * @return the message type as a string
     */
    @Override
    public String getType() {
        return MessageSerializer.NODE_READY;
    }
}