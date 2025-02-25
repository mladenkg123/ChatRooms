# Java Network Chat Project

## Overview

A sophisticated Java-based network communication system featuring a multi-user chat application built with KryoNet and JavaFX. The project demonstrates advanced networking patterns, real-time messaging, and graphical user interface implementation.

## Features

- TCP/IP-based communication
- Multi-user chat support
- Private messaging system
- Chat room management
- Real-time message delivery
- JavaFX-based GUI interface
- Message history tracking
- User presence management
- Room invitations system

## Technical Stack

- Java 11+
- JavaFX 17
- KryoNet 2.22.9
- Eclipse IDE (recommended)

## Project Structure

The project consists of several key components:

### Core Components

1. **ChatServer**: Handles all server-side operations including:

   - User connection management
   - Message broadcasting
   - Chat room administration
   - Private conversation handling

2. **ChatClient**: Manages client-side functionality:

   - Server connection
   - Message sending/receiving
   - GUI interaction
   - User input processing

3. **Message System**: Implements various message types for different chat functionalities

## Setup and Installation

### Prerequisites

1. Java 11 or higher
2. JavaFX 17 SDK
3. KryoNet 2.22.9
4. Eclipse IDE (recommended)

### Running the Application

1. **Start the Server**:

   ```bash
   java -jar chatServer.jar <port_number>
   ```

   Default recommended port: 54555

2. **Launch the Client**:

   ```bash
   java --module-path "<path-to-javafx-lib>" --add-modules javafx.controls,javafx.fxml -cp "lib/kryonet-2.22.9.main.jar;main.jar;chatClient.jar;chatServer.jar" rs.raf.pds.v4.z5.Main <hostname> <port> <username>
   ```

## Architecture

### Server Architecture

- Concurrent user management using ConcurrentHashMap
- Thread-safe message handling
- Room management system
- Private conversation support

### Client Architecture

- JavaFX-based GUI
- Asynchronous message handling
- Real-time updates
- Message history tracking

## Development

### Building the Project

The project uses Eclipse IDE with Java 11. Required configurations:

1. Configure JavaFX 17 SDK in your IDE
2. Add KryoNet 2.22.9 to your build path
3. Ensure proper module path configuration for JavaFX

### Project Configuration

- Java compiler compliance level: 11
- Required VM arguments for JavaFX modules
- KryoNet network buffer configuration

## Contributing

1. Fork the repository
2. Create your feature branch
3. Commit your changes
4. Push to the branch
5. Create a new Pull Request

## License

This project is intended for educational purposes and demonstrates various network communication patterns in Java.

## Notes

- Default client buffer sizes are configured for optimal performance
- The server supports multiple simultaneous connections
- The system includes error handling and connection management
- GUI is built using JavaFX for a user interface
