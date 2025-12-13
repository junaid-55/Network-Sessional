package Server;

import java.io.IOException;
import java.io.File;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.*;

public class Server {

    public static final int BUFFER_SIZE = 1024000000;
    public static final int MIN_CHUNK_SIZE = 51200;
    public static final int MAX_CHUNK_SIZE = 102400;

    private Set<String> clients;
    private HashMap<String, Socket> clientSockets;
    private Long fileRequestId = 0L;

    public Server() {
        clients = new HashSet<>();
        clientSockets = new HashMap<>();
    }

    public synchronized boolean addClient(String clientName, Socket socket) {
        if (clientSockets.containsKey(clientName)) {
            return false;
        }
        clients.add(clientName);
        clientSockets.put(clientName, socket);
        System.out.println("Client added: " + clientName);
        return true;
    }

    public synchronized void removeClient(String clientName) {
        clientSockets.remove(clientName);
        System.out.println("Client removed: " + clientName);
    }

    public synchronized List<String> getAllClients() {
        List<String> clientList = new ArrayList<>();
        for (String client : clients) {
            if (clientSockets.containsKey(client)) {
                clientList.add(client + " (online)");
            } else {
                clientList.add(client + " (offline)");
            }
        }
        return clientList;
    }

    public synchronized boolean handleFileRequest(String fileDescription, String requestingClient, String receivingClient) {
        Long requestId = fileRequestId++;
        return true;
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        Server server = new Server();
        ServerSocket welcomeSocket = new ServerSocket(6666);
        new File("Files").mkdirs();

        while (true) {
            System.out.println("\nWaiting for connection...");
            Socket socket = welcomeSocket.accept();
            System.out.println("Connection established");

            Worker worker = new Worker(socket, server);
        }
    }
}
