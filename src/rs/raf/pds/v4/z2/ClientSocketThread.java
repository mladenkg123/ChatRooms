package rs.raf.pds.v4.z2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientSocketThread implements Runnable{

	private volatile Thread thread = null;
	
	Socket socket;
	volatile boolean running = false;
	final String userName;
	
	public ClientSocketThread(Socket socket, String userName) {
		this.socket = socket;
		this.userName = userName;
	}
	
	public void start() {
		if (thread == null) {
			thread = new Thread(this);
			thread.start();
		}
	}
	public void stop() {
		Thread stopThread = thread;
		thread = null;
		running = false;
		if (stopThread != null)
			stopThread.interrupt();
	}
		
	@Override
	public void run() {
		try (
	         PrintWriter out =
	         	new PrintWriter(socket.getOutputStream(), true);
	         BufferedReader in =
	                new BufferedReader(
	                    new InputStreamReader(socket.getInputStream()));
	         BufferedReader stdIn =
	               new BufferedReader(
	                    new InputStreamReader(System.in))	// Za čitanje sa standardnog ulaza - tastature!
	        ) {
				System.out.println("Server:"+in.readLine());
				out.println(userName);	// Login 
				System.out.println("Server:"+in.readLine());
	            
				String userInput;
				running = true;
				
	            while (running) {
	            	userInput = stdIn.readLine();
	            	if (userInput == null || "BYE".equalsIgnoreCase(userInput))// userInput - tekst koji je unet sa tastature!
	            	{
	            		userInput = "Bye";
	            		running = false;
	            	}
	                out.println(userInput);							// Slanje unetog teksta ka serveru
	                System.out.println("Server: " + in.readLine());	// Štampanje odgovora koji je stigao od servera
	            }
	            
	    } catch (IOException e) {
			e.printStackTrace();
		}
		finally {
			try {
				running = false;
				socket.close();
			} catch (IOException e) {
				
				e.printStackTrace();
			}
		}
		
	}

	public static void main(String[] args) {
		if (args.length != 3) {
            System.err.println(
                "Usage: java -jar clientSocketThread.jar <host name> <port number> <username>");
            System.exit(1);
        }
 
        String hostName = args[0];
        int portNumber = Integer.parseInt(args[1]);
        String userName = args[2];
        
        try{
             Socket socket = new Socket(hostName, portNumber);
        	
        	 ClientSocketThread client = new ClientSocketThread(socket, userName);
        	 client.start();
        	 
        	 client.thread.join();
         } catch (IOException e) {
           System.err.println("Couldn't get I/O for the connection to " +
                    hostName);
                
         } catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
        System.exit(1);
	}
}
