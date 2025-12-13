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
    private Integer bufferCounter;
    private HashMap<Long, Pair<String, Long>> currentTransmission = new HashMap<>();
    private Long fileIDCounter = 0L;
    // pair<fileID, pair<startIndex, length>>
    private List<Pair<Long, Pair<Integer, Integer>>> chunksInfo;

    public Worker(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
        this.chunksInfo = new ArrayList<>();
        this.bufferCounter = 0;
        try {
            this.out = new ObjectOutputStream(socket.getOutputStream());
            this.in = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        new Thread(this).start();
    }

    Response receive_chunk(Request req) {
        byte[] fileChunk = (byte[]) req.getData();
        Long fileId = (Long) req.getFileId();
        if (fileId == null) {
            return new Response("ERROR", "Invalid file ID");
        }
        if (currentTransmission.get(fileId) == null) {
            return new Response("ERROR", "File ID not recognized");
        }
        String fileName = currentTransmission.get(fileId).first();
        System.out.println("Received file chunk of size " + fileChunk.length + " bytes from " + clientName
                + " for file: " + fileName);

        chunksInfo.add(new Pair<Long, Pair<Integer, Integer>>(fileId,
                new Pair<Integer, Integer>(bufferCounter, fileChunk.length)));
        System.arraycopy(fileChunk, 0, buffer, bufferCounter, fileChunk.length);
        bufferCounter += fileChunk.length;
        System.out.println("Buffered chunk. Total buffered: " + bufferCounter + " bytes");
        return new Response("SUCCESS", "Chunk received");
    }

    void writeToFile(Long fileId) {
        int size = 0;
        String fileName = currentTransmission.get(fileId).first();
        for (Pair<Long, Pair<Integer, Integer>> chunkInfo : chunksInfo) {
            if (!chunkInfo.first().equals(fileId))
                continue;
            size += chunkInfo.second().second();
        }
        if (size == currentTransmission.get(fileId).second()) {
            System.out.println("All chunks received for file: " + fileName);
            currentTransmission.remove(fileId);
        } else {
            System.out.println("Waiting for more chunks for file ID: " + fileId);
            return;
        }
        try {
            FileOutputStream fos = new FileOutputStream(new File(clientName + "/" + fileName), true);
            for (Pair<Long, Pair<Integer, Integer>> chunkInfo : chunksInfo) {
                Integer startIndex = chunkInfo.second().first();
                Integer length = chunkInfo.second().second();
                fos.write(buffer, startIndex, length);
            }
            fos.close();
            System.out.println("Written buffered data to file: " + fileName);
            bufferCounter = 0;
            chunksInfo.clear();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
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

                if (req.getType().equals("GET_LIVE_USERS")) {
                    List<String> allClients = server.getAllClients();
                    res = new Response("SUCCESS", allClients);
                    out.writeObject((Object) res);
                } else if (req.getType() == "LogOut") {
                    break;
                } else if (req.getType().equals("UPLOAD_FILE")) {
                    FileDescriptor fileDesc = (FileDescriptor) req.getData();
                    if (Server.BUFFER_SIZE - bufferCounter >= fileDesc.getFileSize()) {
                        int chunk_size = new Random().nextInt((Server.MAX_CHUNK_SIZE - Server.MIN_CHUNK_SIZE) + 1)
                                + Server.MIN_CHUNK_SIZE;
                        res = new Response("SUCCESS", new Pair<Integer, Long>(chunk_size, fileIDCounter));
                        currentTransmission.put(fileIDCounter,
                                new Pair<String, Long>(fileDesc.getFileName(), fileDesc.getFileSize()));
                        fileIDCounter++;
                    } else {
                        res = new Response("ERROR", "Server buffer full. Try again later.");
                    }
                    out.writeObject(res);
                    out.flush();
                } else if (req.getType().equals("FILE_CHUNK")) {
                    out.writeObject((Object) receive_chunk(req));
                } else if (req.getType().equals("END_OF_FILE")) {
                    Long fileId = (Long) req.getFileId();
                    writeToFile(fileId);
                    res = new Response("SUCCESS", "File upload complete.");
                    out.writeObject((Object) res);
                } else {
                    res = new Response("ERROR", "Invalid Request!!!");
                    out.writeObject((Object) res);
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Client disconnected: " + clientName);
        } finally {
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