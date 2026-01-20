package Server;

import java.net.Socket;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
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
    private HashMap<Long, FileDescriptor> currentTransmission = new HashMap<>();
    private Long fileIDCounter = 0L;
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

    Response checkUploadMetadata(Request req) {
        FileDescriptor fileDesc = (FileDescriptor) req.getData();
        if (fileDesc.getFileSize() + bufferCounter > Server.BUFFER_SIZE) {
            return new Response("ERROR", "Server buffer full. Try again later.");
        }
        if(fileDesc.isRequested() && server.checkValidityOfRequest(fileDesc.getRequestId(), clientName)==false ){
            return new Response("ERROR", "Invalid file request ID.");
        }
        int chunk_size = new Random().nextInt((Server.MAX_CHUNK_SIZE - Server.MIN_CHUNK_SIZE) + 1)
                + Server.MIN_CHUNK_SIZE;
        Response res = new Response("SUCCESS", new Pair<Integer, Long>(chunk_size, fileIDCounter));
        currentTransmission.put(fileIDCounter, fileDesc);
        fileIDCounter++;
        return res;
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
        String fileName = currentTransmission.get(fileId).getFileName();
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
        String fileName = currentTransmission.get(fileId).getFileName();
        for (Pair<Long, Pair<Integer, Integer>> chunkInfo : chunksInfo) {
            if (!chunkInfo.first().equals(fileId)) {
                continue;
            }
            size += chunkInfo.second().second();
        }
        if (size == currentTransmission.get(fileId).getFileSize()) {
            System.out.println("All chunks received for file: " + fileName);
        } else {
            System.out.println("Waiting for more chunks for file ID: " + fileId);
            return;
        }
        String finalFilePath = "Files/" + clientName + "/";
        if (currentTransmission.get(fileId).isPublic() || currentTransmission.get(fileId).isRequested()) {
            finalFilePath += "public/";
        } else {
            finalFilePath += "private/";
        }
        try {
            FileOutputStream fos = new FileOutputStream(finalFilePath + fileName, false);
            for (Pair<Long, Pair<Integer, Integer>> chunkInfo : chunksInfo) {
                Integer startIndex = chunkInfo.second().first();
                Integer length = chunkInfo.second().second();
                fos.write(buffer, startIndex, length);
            }
            fos.close();
            System.out.println("Written buffered data to file: " + fileName);
            FileOutputStream fosLog = new FileOutputStream("Files/" + clientName + "/log.txt", true);
            String logEntry = "FileName: " + fileName + ", DateTime: " + new Date().toString() + ", Action: UPLOAD" + ", Status: SUCCESSFUL" + "\n";
            fosLog.write(logEntry.getBytes());
            bufferCounter = 0;
            chunksInfo.clear();
        } catch (IOException e) {
            e.printStackTrace();
        }
        FileDescriptor fd = currentTransmission.get(fileId);
        if (fd.isRequested()) {
            server.forwardRequestCompletion(fd.getRequestId(), clientName, fileName);
        }
        currentTransmission.remove(fileId);

    }

    Response sendFileChunk(Request req) {
        Pair<String, Pair<String, String>> data = (Pair<String, Pair<String, String>>) req.getData();
        String fileName = (String) data.first();
        String ownerType = (String) data.second().first();
        String ownerName = (String) data.second().second();
        String filePath;
        if (ownerType.equals("LOCAL")) {
            filePath = "Files/" + clientName + "/";
            if (req.isPublic()) {
                filePath += "public/" + fileName;
            } else {
                filePath += "private/" + fileName;
            }
        } else {
            filePath = "Files/" + ownerName + "/public/" + fileName;
        }

        File file = new File(filePath);
        if (!file.exists()) {
            return new Response("ERROR", "File not found on server.");
        }
        try{
        out.writeObject(new Response("SUCCESS", "START_FILE_TRANSFER"));
        out.flush();
        } catch (IOException e){
            e.printStackTrace();
            return new Response("ERROR", "Failed to initiate file transfer.");
        }
        for(int i = 0; i < file.length(); i += Server.MAX_CHUNK_SIZE){
        int bytesToSend = (int) Math.min(Server.MAX_CHUNK_SIZE, file.length() - i);
        byte[] fileChunk = new byte[bytesToSend];
        try (FileInputStream fis = new FileInputStream(filePath)) {
            fis.skip(i);
            fis.read(fileChunk, 0, bytesToSend);
            out.writeObject(new Response("SUCCESS", "FILE_CHUNK", (Object) fileChunk));
            out.flush();
        } catch (IOException e) {
            try {
                FileOutputStream fosLog = new FileOutputStream("Files/" + clientName + "/log.txt", true);
                String logEntry = "FileName: " + fileName + ", DateTime: " + new Date().toString() + ", Action: DOWNLOAD" + ", Status: FAILED" + "\n";
                fosLog.write(logEntry.getBytes());
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
            return new Response("ERROR", "Failed to send file.");
        }
    }
        try {
            FileOutputStream fosLog = new FileOutputStream("Files/" + clientName + "/log.txt", true);
            String logEntry = "FileName: " + fileName + ", DateTime: " + new Date().toString() + ", Action: DOWNLOAD" + ", Status: SUCCESSFUL" + "\n";
            fosLog.write(logEntry.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new Response("SUCCESS", "END_OF_FILE", null);
    }

    Pair<List<String>, List<String>> getOwnFiles(String clientName) {
        List<String> publicFiles = new ArrayList<>();
        List<String> privateFiles = new ArrayList<>();
        File publicDir = new File("Files/" + clientName + "/public");
        File privateDir = new File("Files/" + clientName + "/private");
        for (File file : publicDir.listFiles()) {
            publicFiles.add(file.getName());
        }
        for (File file : privateDir.listFiles()) {
            privateFiles.add(file.getName());
        }
        return new Pair<>(publicFiles, privateFiles);
    }

    List<Pair<String, List<String>>> getOthersFiles(String clientName) {
        List<Pair<String, List<String>>> othersFiles = new ArrayList<>();
        File filesDir = new File("Files/");
        for (File userDir : filesDir.listFiles()) {
            if (userDir.getName().equals(clientName)) {
                continue;
            }
            File publicDir = new File(userDir.getPath() + "/public");
            List<String> publicFiles = new ArrayList<>();
            for (File file : publicDir.listFiles()) {
                publicFiles.add(file.getName());
            }
            othersFiles.add(new Pair<>(userDir.getName(), publicFiles));
        }
        return othersFiles;
    }
    List<String> getUploadHistory() {
        List<String> uploadHistory = new ArrayList<>();
        try (Scanner scanner = new Scanner(new File("Files/" + clientName + "/log.txt"))) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line.contains("Action: UPLOAD")) {
                    uploadHistory.add(line);
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return uploadHistory;
    }

    List<String> getDownloadHistory() {
        List<String> downloadHistory = new ArrayList<>();
        try (Scanner scanner = new Scanner(new File("Files/" + clientName + "/log.txt"))) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line.contains("Action: DOWNLOAD")) {
                    downloadHistory.add(line);
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return downloadHistory;
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
            new File("Files/" + clientName).mkdirs();
            new File("Files/" + clientName + "/private").mkdirs();
            new File("Files/" + clientName + "/public").mkdirs();
            new File("Files/" + clientName + "/log.txt").createNewFile();
            new File("Files/" + clientName + "/messages.txt").createNewFile();
            String welcomeMsg = "Hello from server - Welcome " + clientName;
            Response res = new Response("SUCCESS", (Object) welcomeMsg);
            out.writeObject((Object) res);
            while (true) {
                req = (Request) in.readObject();

                if (req.getType().equals("GET_LIVE_USERS")) {
                    List<String> allClients = server.getAllClients();
                    res = new Response("SUCCESS", allClients);
                    out.writeObject((Object) res);
                } else if (req.getType().equals("LogOut")) {
                    break;
                } else if (req.getType().equals("UPLOAD_FILE")) {
                    res = checkUploadMetadata(req);
                    out.writeObject(res);
                    out.flush();
                } else if (req.getType().equals("FILE_CHUNK")) {
                    out.writeObject((Object) receive_chunk(req));
                } else if (req.getType().equals("END_OF_FILE")) {
                    Long fileId = req.getFileId();
                    writeToFile(fileId);
                    res = new Response("SUCCESS", "File upload complete.");
                    out.writeObject((Object) res);
                } else if (req.getType().equals("DOWNLOAD_FILE")) {
                    res = sendFileChunk(req);
                    out.writeObject((Object) res);
                } else if (req.getType().equals("VIEW_OWN_FILES")) {
                    res = new Response("SUCCESS", getOwnFiles(clientName));
                    out.writeObject((Object) res);
                } else if (req.getType().equals("VIEW_OTHERS_FILES")) {
                    res = new Response("SUCCESS", getOthersFiles(clientName));
                    out.writeObject((Object) res);
                } else if (req.getType().equals("REQUEST_FILE")){
                    var data = (Pair<String, String>) req.getData();
                    boolean status = server.handleFileRequest(data.first(), clientName, data.second());
                    String msg = status ? "File request sent successfully." : "File request failed.";
                    res = new Response(status ? "SUCCESS" : "ERROR", msg);
                    out.writeObject((Object) res);
                } else if (req.getType().equals("VIEW_UPLOAD_HISTORY")) {
                    res = new Response("SUCCESS", getUploadHistory());
                    out.writeObject((Object) res);
                } else if (req.getType().equals("VIEW_DOWNLOAD_HISTORY")) {
                    res = new Response("SUCCESS", getDownloadHistory());
                    out.writeObject((Object) res);
                } else if(req.getType().equals("VIEW_MESSAGES")){
                    String path = "Files/" + clientName + "/messages.txt";
                    List<String> messages = new ArrayList<>();
                    try (Scanner scanner = new Scanner(new File(path))) {
                        while (scanner.hasNextLine()) {
                            String line = scanner.nextLine();
                            messages.add(line);
                        }
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                    try {
                        new FileOutputStream(path).close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    res = new Response("SUCCESS", messages);
                    out.writeObject((Object) res);
                }
                else {
                    res = new Response("ERROR", "Invalid Request!!!");
                    out.writeObject((Object) res);
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Client disconnected: " + clientName);
        } finally {
            if (clientName != null) {
                FileOutputStream fosLog = null;
                try {
                    fosLog = new FileOutputStream("Files/" + clientName + "/log.txt", true);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                for (Long fileId : currentTransmission.keySet()) {

                    String logEntry = "FileName: " + currentTransmission.get(fileId).getFileName() + ", DateTime: " + new Date().toString() + ", Action: UPLOAD" + ", Status: FAILED" + "\n";
                    try {
                        if (fosLog != null) {
                            fosLog.write(logEntry.getBytes());
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    System.out.println("Cleaning up incomplete transfer for file ID: " + fileId);
                }
                currentTransmission.clear();
                chunksInfo.clear();
                bufferCounter = 0;
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
