package Server;

import java.net.Socket;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.*;
import util.*;

public class Worker implements Runnable {
    private Socket socket;
    private Server server;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private String clientName;
    private byte[] buffer = new byte[Server.BUFFER_SIZE];
    private HashMap<Long, String> currentTransmission = new HashMap<>();
    private Long fileIDCounter = 0L;

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
            if (clientName == null || clientName.trim().isEmpty()) {
                Response res = new Response("ERROR", "Invalid name!!!");
                out.writeObject((Object) res);
                socket.close();
                return;
            }
            boolean added = server.addClient(clientName, socket);
            if (!added) {
                Response res = new Response("ERROR", "Client name already taken. Disconnecting...");
                out.writeObject((Object) res);
                socket.close();
                return;
            }
            new File(clientName).mkdirs(); 
            String welcomeMsg = "Hello from server - Welcome " + clientName;
            Response res = new Response("SUCCESS", (Object) welcomeMsg);
            out.writeObject((Object) res);
            while (true) {
                req = (Request) in.readObject();

                if (req.getType() == "GET_LIVE_USERS") {
                    List<String> allClients = server.getAllClients();
                    res = new Response("SUCCESS", allClients);
                    out.writeObject((Object) res);
                } else if (req.getType() == "LogOut") {
                    break;
                } else if (req.getType() == "UPLOAD_FILE") {
                    System.out.println("Received file upload request from " + clientName);
                } else if (req.getType().equals("UPLOAD_FILE")) {
                    FileDescriptor fileDesc = (FileDescriptor) req.getData();
                    int chunk_size = new Random().nextInt((Server.MAX_CHUNK_SIZE - Server.MIN_CHUNK_SIZE) + 1)
                            + Server.MIN_CHUNK_SIZE;
                    int fileID = 0;
                    currentTransmission.put(fileIDCounter, fileDesc.getFileName());
                    fileIDCounter++;
                    res = new Response("SUCCESS", new Pair<Integer, Long>(chunk_size, fileIDCounter - 1));
                    out.writeObject(res);
                    out.flush();

                } else if (req.getType().equals("FILE_CHUNK")) {
                    byte[] fileChunk = (byte[]) req.getData();
                    System.out.println("Received file chunk of size " + fileChunk.length + " bytes from " + clientName+" for file: " + fileName);

                    Long fileId = (Long) req.getFileId();
                    String fileName = currentTransmission.get(fileId);

                    try (FileOutputStream fos = new FileOutputStream(clientName + "/" + fileName, true)) {
                        fos.write(fileChunk);
                        System.out.println("Wrote chunk to disk");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    req = new Request("ERROR", "Invalid Request!!!");
                    out.writeObject((Object) req);
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Client disconnected: " + clientName);
        } finally {
            // Remove client when disconnected
            if (clientName != null) {
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