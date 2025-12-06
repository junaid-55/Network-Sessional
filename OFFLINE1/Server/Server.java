package Server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.*;
public class Server {
    
    private Set<String> clients;
    private HashMap<String, Socket> clientSockets;
    
    public Server() {
        clients = new HashSet<>();
        clientSockets = new HashMap<>();    
    }
    
    public synchronized boolean addClient(String clientName, Socket socket) {
        if(clientSockets.containsKey(clientName)) {
            return false;
        }
        clients.add(clientName);
        clientSockets.put(clientName, socket);
        System.out.println("Client added: " + clientName + " | Total clients: " + clients.size());
        return true;
    }
    
    public synchronized void removeClient(String clientName) {
        clientSockets.remove(clientName);
        System.out.println("Client removed: " + clientName + " | Total clients: " + clients.size());
    }
    
    public synchronized List<String> getAllClients() {
        List<String> clientList = new ArrayList<>();
        for(String client : clients) {
            if(clientSockets.containsKey(client)) {
                clientList.add(client + " (online)");
            }
            else {
                clientList.add(client + " (offline)");
            }
        }
        return clientList;
    }
    
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        Server server = new Server();
        ServerSocket welcomeSocket = new ServerSocket(6666);

        while(true) {
            System.out.println("\nWaiting for connection...");
            Socket socket = welcomeSocket.accept();
            System.out.println("Connection established");

            Worker worker = new Worker(socket, server);
        }
    }
}