# Java Network Communication Project

## Overview

This project demonstrates various network communication patterns in Java, including TCP sockets, UDP communication, and a multi-user chat application using KryoNet.

## Project Structure

The project contains several distinct implementations:

1. Simple Socket Communication
2. Multi-threaded Socket Server
3. UDP Socket Communication
4. Advanced Chat System

## Requirements

- Java 11 or higher
- JavaFX 17
- KryoNet 2.22.9
- Eclipse IDE (recommended)

## Components

### 1. Simple Socket Communication

Basic TCP socket implementation with:

- Server: Echoes back client messages
- Client: Sends messages to server and displays responses

### 2. UDP Communication

Implements UDP socket communication:

- Client: Sends datagram packets and receives responses
- Server: Generates random numbers and responds to client requests

### 3. Chat Application

Advanced chat system featuring:

- Multi-user support
- Private messaging
- Chat rooms
- Message history
- JavaFX GUI

## Running the Applications

### Chat Application

1. Start the server:

   ```bash
   java -jar chatServer.jar <port_number>
   ```

   Recommended port: 54555

2. Start the client:

   ```bash
   java --module-path "<path-to-javafx-lib>" --add-modules javafx.controls,javafx.fxml -cp "lib/kryonet-2.22.9.main.jar;main.jar;chatClient.jar;chatServer.jar" rs.raf.pds.v4.z5.Main <hostname> <port> <username>
   ```

### Simple Socket Server

```bash
java -jar ProstSocketServer.jar <port_number>
```

### UDP Server

```bash
java -jar serverUDPSocket.jar <port_number>
```

Recommended port: 4443

## Features

- TCP/IP communication
- UDP datagram packets
- Multi-threading support
- GUI-based chat interface
- Private messaging system
- Chat room management
- Real-time message delivery
- Connection management
- Error handling

## Development

The project uses Eclipse IDE with Java 11. Required libraries:

- JavaFX 17 SDK
- KryoNet 2.22.9

## License

This project is for educational purposes and demonstrates various network communication patterns in Java.
