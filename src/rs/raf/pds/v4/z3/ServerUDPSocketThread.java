package rs.raf.pds.v4.z3;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.ThreadLocalRandom;

public class ServerUDPSocketThread implements Runnable {

	private volatile Thread thread = null;
	
	volatile boolean running = false;
	
	protected final DatagramSocket udpSocket;
	
	public ServerUDPSocketThread(DatagramSocket udpSocket) {
		this.udpSocket = udpSocket;
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
		running = true;
		try { 
			while (running) {
				try {
	         	
	                byte[] buf = new byte[256];
	
	                // Prijem zahteva
	                DatagramPacket recvPacket = new DatagramPacket(buf, buf.length);
	                udpSocket.receive(recvPacket);
	                System.out.println("Primljen zahtev sa hosta:"+recvPacket.getAddress().getHostAddress()+
	                		", port:"+recvPacket.getPort());
	
	                int noviBroj = ThreadLocalRandom.current().nextInt(0, 2000);
	                String message = "Generisani broj = "+noviBroj;
	                
	                buf = message.getBytes();
	
	                // slanje odgovora klijentu tako sto se uzimaju IP adresa i port iz primljenog datagrama
	                InetAddress ipAddress = recvPacket.getAddress();
	                int port = recvPacket.getPort();
	                
	                DatagramPacket sendPacket = new DatagramPacket(buf, buf.length, ipAddress, port);
	                udpSocket.send(sendPacket);
	         	} catch (IOException e) {
	         		e.printStackTrace();
	         		running = false;
	         	}
			}	
		}finally {
        	udpSocket.close();
        }
		
	}
	public static void main(String[] args) throws IOException {
	   if (args.length != 1) {
	        System.err.println("Usage: java -jar serverUDPSocket.jar <port number>");
	        System.out.println("Recommended port number is 4443");
	        System.exit(1);
	   }
	    
	   int portNumber = Integer.parseInt(args[0]);
	   try { 
		   DatagramSocket udpSocket = new DatagramSocket(portNumber);
	   	   ServerUDPSocketThread udpServer = new ServerUDPSocketThread(udpSocket);
	   	   udpServer.start();
	   
			udpServer.thread.join();
	   } catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
	   }
	}
}
