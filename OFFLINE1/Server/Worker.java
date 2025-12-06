package Server;

import java.net.Socket;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.util.List;
import util.*;
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
            Request req = (Request) in.readObject();
            clientName = (String) req.getData();
            if(clientName == null || clientName.trim().isEmpty()) {
                Response res = new Response("ERROR", "Invalid name!!!");
                out.writeObject((Object)res);
                socket.close();
                return;
            }
            boolean added = server.addClient(clientName,socket);
            if(!added) {
                Response res = new Response ("ERROR", "Client name already taken. Disconnecting...");
                out.writeObject((Object)res);
                socket.close();
                return;
            }
            
            // Send welcome message
            String welcomeMsg = "Hello from server - Welcome " + clientName;
            Response res = new Response("SUCCESS", (Object)welcomeMsg);
            out.writeObject((Object)res);
            while(true) {
                req = (Request) in.readObject();

                if(req.getType() == "GET_LIVE_USERS") {
                    List<String> allClients = server.getAllClients();
                    res = new Response("SUCCESS",allClients);
                    out.writeObject((Object)res);
                } else if(req.getType() == "LogOut") {
                    break;
                } else {
                    req = new Request("ERROR","Invalid Request!!!");
                    out.writeObject((Object)req);
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