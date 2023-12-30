package rs.raf.pds.v4.z5.messages;

import java.util.List;

public class ListRoomsUpdate {
    private List<String> rooms;

    public ListRoomsUpdate() {
    }

    public ListRoomsUpdate(List<String> rooms) {
        this.rooms = rooms;
    }

    public List<String> getRooms() {
        return rooms;
    }

	@Override
	public String toString() {
		return  "Available rooms:" + rooms ;
	}
}