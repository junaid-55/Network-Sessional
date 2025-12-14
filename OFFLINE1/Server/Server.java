package Server;

import java.io.IOException;
import java.io.File;
import java.io.FileOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.*;
import util.*;
public class Server {

    public static final int BUFFER_SIZE = 1024;
    public static final int MIN_CHUNK_SIZE = 5;
    public static final int MAX_CHUNK_SIZE = 10;

    private Set<String> clients;
    private HashMap<String, Socket> clientSockets;
    private Long fileRequestId = 0L;
    private HashMap<Long, FileRequest> fileRequests = new HashMap<>();

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

    public synchronized boolean checkValidityOfRequest(Long requestId, String clientName) {
        if(!fileRequests.containsKey(requestId)){
            return false;
        }
        FileRequest req = fileRequests.get(requestId);
        return req.getReceivingClient().equals(clientName) || req.getReceivingClient().equals("ALL");
    }

    public synchronized boolean handleFileRequest(String fileDescription, String requestingClient, String receivingClient) {
        if(clients.contains(requestingClient)==false || requestingClient.equals(receivingClient)){
            return false;
        }
        Long requestId = fileRequestId++;
        FileRequest fileRequest = new FileRequest(fileDescription, requestingClient, receivingClient, receivingClient.equals("ALL") ? clients.size() : 1);
        fileRequests.put(requestId, fileRequest);
        if(receivingClient.equals("ALL")){
            File file = new File("Files" );
            for(File f: file.listFiles()){
                if(f.getName().equals(requestingClient)){
                    continue;
                }
                String path = "Files/" + f.getName()+"/messages.txt";
                try {
                    String msg = "RequestID: " + requestId + ", From: " + requestingClient + ", File: " + fileDescription+"\n";
                    FileOutputStream fos = new FileOutputStream(path, true);
                    fos.write(msg.getBytes());
                    fos.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            String path = "Files/" + receivingClient + "/messages.txt";
            try {
                String msg = "RequestID: " + requestId + ", From: " + requestingClient + ", FileDescription: " + fileDescription+"\n";
                FileOutputStream fos = new FileOutputStream(path, true);
                fos.write(msg.getBytes());
                fos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    public synchronized boolean forwardRequestCompletion(Long requestId, String receivingClient,String fileName) {
        if(!fileRequests.containsKey(requestId)){
            return false;
        }
        FileRequest req = fileRequests.get(requestId);
        req.decrementReqCount();
        String msg = "Client : "+ receivingClient + " has completed your file request for: \"" + req.getFileDescription() + "\" with file: " + fileName + "\n";
        String path = "Files/" + req.getRequestingClient() + "/messages.txt";
        try {
            FileOutputStream fos = new FileOutputStream(path, true);
            fos.write(msg.getBytes());
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if(req.getReqCount() == 0){
            fileRequests.remove(requestId);
        }
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
