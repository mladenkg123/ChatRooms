package rs.raf.pds.v4.z2;

import java.io.IOException;
import java.net.ServerSocket;

public class MultiSocketServer implements Runnable{
	private volatile Thread thread = null;
	
	final int portNumber;
	final ServerSocket serverSocket;
	volatile boolean listening = false;
	
	public MultiSocketServer(int portNumber) throws IOException {
		this.portNumber = portNumber;
		this.serverSocket = new ServerSocket(portNumber);
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
		listening = false;
		if (stopThread != null)
			stopThread.interrupt();
	}
    @Override
	public void run() {
			
    	listening = true; 
    	System.out.println("Server is listening port:"+portNumber);
    	try { 
    		while (listening) {
    			SingleSocketServerThread clientSocketThread = new SingleSocketServerThread(serverSocket.accept(), this); 
    			clientSocketThread.start();
    		}
    	} catch (IOException e) {
    		listening = false;
    		System.err.println("Could not listen on port " + portNumber);
    	}
    	
    }
    
    public static void main(String[] args) throws IOException {

       if (args.length != 1) {
            System.err.println("Usage: java -jar multiSocketServer.jar <port number>");
            System.exit(1);
       }
        
       int portNumber = Integer.parseInt(args[0]);
       MultiSocketServer server = new MultiSocketServer(portNumber);
       server.start();
	   try {
			server.thread.join();
	   } catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
	   }
        
    }
    
}
