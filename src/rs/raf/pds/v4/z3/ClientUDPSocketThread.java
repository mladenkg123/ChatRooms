package rs.raf.pds.v4.z3;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class ClientUDPSocketThread {
	public static void main(String[] args) throws IOException {
	
		if (args.length != 2) {
	         System.out.println("Usage: java -jar clientUDPSocket.jar <hostname> <port number>");
	         System.out.println("Recommended port number is 4443");
	         return;
	    }
		String hostName = args[0];
		int portNumber = Integer.parseInt(args[1]);
		
		
        // Kreiranje DatagraSocket-a. Pošto nije naveden port, dodeljuje mu se neki slobodan UDP port
		DatagramSocket socket = new DatagramSocket();

        // send request
	    byte[] buf = new byte[256];
	    InetAddress ipAddress = InetAddress.getByName(hostName);	// Prevodi hostname u IP adresu
	    
	    // Kreiranje datagram paketa, sa baferom koji sadrzi poruku, IP adresom i portom na koji se salje
	    DatagramPacket sendPacket = new DatagramPacket(buf, buf.length, ipAddress, portNumber);	
	    socket.send(sendPacket);	// Slanje DatagramPacket-a
	
	    // Primanje paketa - odgovora 
	    DatagramPacket recvPacket = new DatagramPacket(buf, buf.length);
	    socket.receive(recvPacket);
	
	    // Prikaz primljene poruke
	    String prijemPoruka = new String(recvPacket.getData(), 0, recvPacket.getLength());
	    System.out.println("Odgovor od servera: " + prijemPoruka);
	    System.out.println("Adresa servera:"+recvPacket.getAddress().getHostAddress()+", port:"+recvPacket.getPort());
	
	    socket.close();
	}
}
