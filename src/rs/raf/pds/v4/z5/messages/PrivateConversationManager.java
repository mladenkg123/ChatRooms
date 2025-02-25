package rs.raf.pds.v4.z5.messages;
import java.util.HashMap;
import java.util.Map;

public class PrivateConversationManager {
	
    private Map<String, String[]> privateConversations;

    public PrivateConversationManager() {
        this.privateConversations = new HashMap<>();
    }

    public void startPrivateConversation(String username1, String username2) {
        String conversationId = generateConversationId(username1, username2);
        privateConversations.put(conversationId, new String[]{username1, username2});
    }

    public String[] getConversationParticipants(String conversationId) {
        return privateConversations.get(conversationId);
    }

    private String generateConversationId(String username1, String username2) {
    	return username1 + "_" + username2;    }
}
