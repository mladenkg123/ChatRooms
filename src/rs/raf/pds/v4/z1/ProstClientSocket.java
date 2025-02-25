package rs.raf.pds.v4.z1;

import java.io.*;
import java.net.*;

public class ProstClientSocket {
	
    public static void main(String[] args) throws IOException {
	         
        if (args.length != 2) {
            System.err.println(
                "Usage: java ProstClientSocket <host name> <port number>");
            System.exit(1);
        }
 
        String hostName = args[0];
        int portNumber = Integer.parseInt(args[1]);
 
        try (
            Socket socket = new Socket(hostName, portNumber);
            PrintWriter out =
                new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in =
                new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));
            BufferedReader stdIn =
                new BufferedReader(
                    new InputStreamReader(System.in))	// Za čitanje sa standardnog ulaza - tastature!
        ) {
            String userInput;
            while ((userInput = stdIn.readLine()) != null) { 	// userInput - tekst koji je unet sa tastature!
                out.println(userInput);							// Slanje unetog teksta ka serveru
                System.out.println("Server: " + in.readLine());	// Štampanje odgovora koji je stigao od servera
            }
        } catch (UnknownHostException e) {
            System.err.println("Don't know about host " + hostName);
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Couldn't get I/O for the connection to " +
                hostName);
            System.exit(1);
        } 
    }

}
