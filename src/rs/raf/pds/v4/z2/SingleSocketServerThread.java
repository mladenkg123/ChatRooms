package rs.raf.pds.v4.z2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class SingleSocketServerThread implements Runnable{
	private volatile Thread thread = null;
	
	final MultiSocketServer socketServer; 
	Socket socketToClient;
	CommProtocol protocol;
	volatile boolean running = false;
	
	public SingleSocketServerThread(Socket socket, MultiSocketServer socketServer) {
		this.socketToClient = socket;
		this.socketServer = socketServer;
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
		try(
	        PrintWriter out = new PrintWriter(socketToClient.getOutputStream(), true);
	        BufferedReader in = new BufferedReader(
	                new InputStreamReader(
	                		socketToClient.getInputStream()));
	    ){
	       String inputLine, outputLine;
	       protocol = new CommProtocol();
	       outputLine = protocol.processInput(null);
	       out.println(outputLine);
	       running = true;
	       
	       while (running) {
	    	   inputLine = in.readLine(); 
	    	   if (inputLine == null)
	    		   break;
	    	   outputLine = protocol.processInput(inputLine);
	           out.println(outputLine);
	           if (protocol.isDisconnected())
	               break;
	       }
	       	      
	              	         
		} catch (IOException e) {
	            e.printStackTrace();
		}
		try {
			running = false;
			if (!protocol.isDisconnected())
				protocol.clientDisconnected();
			socketToClient.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
		
}
