package rs.raf.pds.v4.z5.messages;

public class ListRoomsResponse {
    private String[] rooms;

    public ListRoomsResponse(String[] rooms) {
        this.rooms = rooms;
    }

    public String[] getRooms() {
        return rooms;
    }
}