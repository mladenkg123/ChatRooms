package rs.raf.pds.v4.z1;

import java.net.*;
import java.io.*;

public class ProstSocketServer {

    public static void main(String[] args) throws IOException {
         
        if (args.length != 1) {
            System.err.println("Usage: java ProstSocketServer <port number>");
            System.exit(1);
        }
         
        int portNumber = Integer.parseInt(args[0]);
         
        try (
            ServerSocket serverSocket =
                new ServerSocket(portNumber);
           	Socket clientSocket = serverSocket.accept();     
            PrintWriter out =
                new PrintWriter(clientSocket.getOutputStream(), true);                   
            BufferedReader in = new BufferedReader(
                new InputStreamReader(clientSocket.getInputStream()));
        ) {
        	System.out.println("Server osluskuje port:"+portNumber);
        	String inputLine;
            while ((inputLine = in.readLine()) != null) {
                System.out.println("Client:"+inputLine);
            	out.println(inputLine);
            }
        } catch (IOException e) {
            System.out.println("Exception caught when trying to listen on port "
                + portNumber + " or listening for a connection");
            System.out.println(e.getMessage());
        }
    }

}
