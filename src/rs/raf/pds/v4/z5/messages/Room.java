package rs.raf.pds.v4.z5.messages;

import java.util.ArrayList;
import java.util.List;

public class Room {
    private String name;
    private List<String> members;
    private List<ChatMessage> messages;  
    
    public Room(String name) {
        this.name = name;
        this.members = new ArrayList<>();
        this.messages = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public List<String> getMembers() {
        return new ArrayList<>(members);
    }

    public void addMember(String member) {
        members.add(member);
    }

    public void removeMember(String member) {
        members.remove(member);
    }
    public List<ChatMessage> getMessages() {
        return new ArrayList<>(messages);
    }
    
    public List<ChatMessage> getLastNMessages(int n) {
        int startIndex = Math.max(messages.size() - n, 0);
        return new ArrayList<>(messages.subList(startIndex, messages.size()));
    }

    
    
 

    public void addMessage(ChatMessage message) {
        messages.add(message);
    }
    public boolean contains(String username) {
        return members.contains(username);
    }
}