package no.ntnu.message;

/**
 * Message sent from a node to the server when it is ready to receive commands.
 */
public class NodeReadyMessage implements Message {
    private final String nodeInfo;

    public NodeReadyMessage(String nodeInfo) {
        this.nodeInfo = nodeInfo;
    }

    public String getNodeInfo() {
        return nodeInfo;
    }

    @Override
    public String getType() {
        return MessageSerializer.NODE_READY;
    }
}