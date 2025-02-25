package rs.raf.pds.v4.z2;

public class CommProtocol {
	enum ClientState {START, CONNECTED, LOGGED, MESSAGES, DISCONNECTED};
	
	private ClientState clientState; 
	private String username = null;
	
	public CommProtocol() {
		clientState = ClientState.START; 
	}
	public boolean isDisconnected() {
		return clientState == ClientState.DISCONNECTED;
	}
	public String processInput(String message) {
		switch(clientState) {
			case START:
				return moveToStateConnected();
				
			case CONNECTED:
				return checkLogin(message);
				
			case LOGGED:
				return processMessage(message);
				
			case MESSAGES:
				return processMessage(message);
			
			case DISCONNECTED:
				return invalidMessage(message);
				
		}
		return "";
	}
	
	private String moveToStateConnected() {
		clientState = ClientState.CONNECTED;
		return "Hi!";
	}
	
	private String checkLogin(String userName) {
		clientState = ClientState.LOGGED;
		this.username = userName;
		System.out.println("User:"+username+" has logged!");
		return "Hello, "+username;
	
	}
	
	private String processMessage(String message) {
		clientState = ClientState.MESSAGES;
		System.out.println(username+":"+message);
		if ("BYE".equalsIgnoreCase(message)){
			return clientDisconnected();
		}
		else return "OK";
	}

	public String clientDisconnected() {
		clientState = ClientState.DISCONNECTED;
		System.out.println(username+" has disconnected!");
		return "Bye "+username;
	}
	
	private String invalidMessage(String message) {
		System.err.println("Message received while client is disconnected! Message:"+message);
		
		return "";
	}
}
