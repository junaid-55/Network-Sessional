package Main;

import java.net.Socket;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.util.List;

public class Worker implements Runnable {
    private Socket socket;
    private Server server;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private String clientName;
    
    public Worker(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
        try {
            this.out = new ObjectOutputStream(socket.getOutputStream());
            this.in = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        new Thread(this).start();
    }

    @Override
    public void run() {
        try {
            // Read client name
            clientName = (String) in.readObject();
            boolean added = server.addClient(clientName,socket);
            if(!added) {
                out.writeObject("Client name already taken. Disconnecting...");
                socket.close();
                return;
            }
            
            // Send welcome message
            String welcomeMsg = "Hello from server - Welcome " + clientName;
            out.writeObject(welcomeMsg);
            
            // Handle client requests in a loop
            while(true) {
                String request = (String) in.readObject();
                
                if(request.equalsIgnoreCase("GET_USERS")) {
                    List<String> allClients = server.getAllClients();
                    out.writeObject(allClients);
                } else if(request.equalsIgnoreCase("DISCONNECT")) {
                    break;
                } else {
                    out.writeObject("Unknown request: " + request);
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Client disconnected: " + clientName);
        } finally {
            // Remove client when disconnected
            if(clientName != null) {
                server.removeClient(clientName);
            }
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}