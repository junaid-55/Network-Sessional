package Client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;
import java.util.Scanner;
import util.*;

public class Client {

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        Socket socket = new Socket("localhost", 6666);
        System.out.println("\nConnection established");

        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
        ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter your name: ");
        String name = scanner.nextLine();
        Request request = new Request("POST", "login", (Object) name);

        out.writeObject(request);
        out.flush();

        Response res = (Response) in.readObject();
        System.out.println((String) res.getData() + "\n");
        if (res.getStatus().equals("ERROR")) {
            socket.close();
            scanner.close();
            return;
        }
        // menu
        new File("Downloads").mkdirs();
        while (true) {
            System.out.println("\n1. Get live users");
            System.out.println("2. Upload file");
            System.out.println("3. Download file");
            System.out.println("4. Request file");
            System.out.println("5. View Files");
            System.out.println("6. View Upload History");
            System.out.println("7. View Download History");
            System.out.println("8. View Unread Messages");
            System.out.println("9. Disconnect");
            System.out.print("Choose option: ");

            String choice = scanner.nextLine();

            if (choice.equals("1")) {
                Request req = new Request("GET", "GET_LIVE_USERS");
                out.writeObject((Object) req);
                out.flush();
                res = (Response) in.readObject();
                var users = (List<String>) res.getData();
                System.out.println("Live users: ");
                for (String user : users) {
                    System.out.println("\t" + user);
                }
            } else if (choice.equals("2")) {
                System.out.println("Select upload type:\n1. Public\n2. Private\n3. Requested file\nChoose option: ");
                int option = Integer.parseInt(scanner.nextLine());
                Long requestId = 0L;
                if (option == 3) {
                    System.out.print("Enter request ID: ");
                    requestId = Long.parseLong(scanner.nextLine());
                }
                String filePath;
                System.out.print("Enter file path to upload: ");
                filePath = scanner.nextLine();
                if (!new File(filePath).exists()) {
                    System.out.println("File does not exist. Please check the path and try again.");
                    continue;
                }
                String fileName = filePath.substring(filePath.lastIndexOf("/") + 1);
                System.out.println("Uploading file: " + fileName + " with Size " + new java.io.File(filePath).length());
                FileDescriptor fileDesc;
                if (option == 3) {
                    fileDesc = new FileDescriptor(fileName, new java.io.File(filePath).length(), true, requestId);
                } else {
                    fileDesc = new FileDescriptor(fileName, new java.io.File(filePath).length(), option == 1);
                }
                Request req = new Request("POST", "UPLOAD_FILE", (Object) fileDesc);
                out.writeObject((Object) req);
                out.flush();
                res = (Response) in.readObject();
                if (res.getStatus().equals("ERROR")) {
                    System.out.println((String) res.getData());
                    continue;
                }
                System.out.println("Server Response: " + (String) res.getStatus());
                Pair<Integer, Long> pair = (Pair<Integer, Long>) res.getData();

                int chunk_size = pair.first();
                System.out.println("Server approved upload. Chunk size: " + chunk_size + " bytes");
                boolean uploadFailed = false;
                for (int i = 0; i < new File(filePath).length(); i += chunk_size) {
                    int bytesToSend = (int) Math.min(chunk_size, new File(filePath).length() - i);
                    byte[] buffer = new byte[bytesToSend];
                    try (FileInputStream fis = new FileInputStream(filePath)) {
                        fis.skip(i);
                        fis.read(buffer, 0, bytesToSend);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    out.writeObject(new Request("POST", "FILE_CHUNK", (Object) buffer, pair.second()));
                    out.flush();
                    System.out.println("Sent chunk of size: " + bytesToSend + " bytes");
                    res = (Response) in.readObject();
                    System.out.println((String) res.getData());
                    if (res.getStatus().equals("ERROR")) {
                        uploadFailed = true;
                        System.out.println("Upload failed.");
                        break;
                    } else {
                        System.out.println(i / chunk_size + "'th Chunk uploaded successfully.");
                    }
                }
                if (!uploadFailed) {
                    System.out.println("File upload completed successfully.");
                    Request completeReq = new Request("POST", "END_OF_FILE", null, pair.second());
                    out.writeObject((Object) completeReq);
                    out.flush();
                    res = (Response) in.readObject();
                    System.out.println((String) res.getData());
                }
            } else if (choice.equals("3")) {
                System.out.print("Enter file name to download: ");
                String fileName = scanner.nextLine();
                System.out.print("Choose Owner Type : \n1.Local User\n2.Other User\nEnter choice: ");
                String ownerType = Integer.parseInt(scanner.nextLine()) == 1 ? "LOCAL" : "OTHER";
                String ownerName = "";
                boolean isPublic = false;
                if (ownerType.equals("LOCAL")) {
                    System.out.print("Is the file public? (yes/no): ");
                    isPublic = scanner.nextLine().toLowerCase().equals("yes");
                } else if (ownerType.equals("OTHER")) {
                    System.out.print("Enter owner name of the file: ");
                    ownerName = scanner.nextLine();
                }
                Request req = new Request("GET", "DOWNLOAD_FILE", new Pair<>(fileName, new Pair<>(ownerType, ownerName)), isPublic);
                out.writeObject((Object) req);
                out.flush();
                res = (Response) in.readObject();
                if (res.getStatus().equals("ERROR")) {
                    System.out.println((String) res.getData());
                    continue;
                } else {
                    System.out.println("Starting download of file: " + fileName);
                    FileOutputStream fos = new FileOutputStream("Downloads/" + fileName, false);
                    while (true) {
                        res = (Response) in.readObject();
                        if (res.getStatus().equals("ERROR")) {
                            System.out.println((String) res.getData());
                            fos.close();
                            break;
                        }
                        if (res.getType().equals("FILE_CHUNK")) {
                            fos.write((byte[]) res.getData());
                            System.out.println("Received chunk of size: " + ((byte[]) res.getData()).length + " bytes");
                        } else if (res.getType().equals("END_OF_FILE")) {
                            System.out.println("File download completed successfully.");
                            fos.close();
                            break;
                        }
                    }
                }
            } else if (choice.equals("4")) {
                System.out.print("Enter the description of the file you want to request: ");
                String fileDescription = scanner.nextLine();
                System.out.print("Choose recipient of the request:\n1.ALL \n2.CUSTOM\nEnter choice: ");
                String recipientChoice = scanner.nextLine();
                String ownerName = "ALL";
                if (recipientChoice.equals("2")) {
                    System.out.print("Enter the name of the user you want to send the request to: ");
                    ownerName = scanner.nextLine();
                }
                Request req = new Request("POST", "REQUEST_FILE", new Pair<>(fileDescription, ownerName));
                out.writeObject((Object) req);
                out.flush();
                res = (Response) in.readObject();
                if( res.getStatus().equals("ERROR")) {
                    System.out.println((String) res.getData());
                    continue;
                }else {
                    System.out.println("File request sent successfully.");
                }
            } else if (choice.equals("5")) {
                System.out.print("Enter file to view :\n1. Own Files\n2. Other's Files\nChoose option: ");
                int option = Integer.parseInt(scanner.nextLine());
                Request req = new Request("GET", option == 1 ? "VIEW_OWN_FILES" : "VIEW_OTHERS_FILES");
                out.writeObject((Object) req);
                out.flush();
                res = (Response) in.readObject();
                if (res.getStatus().equals("ERROR")) {
                    System.out.println((String) res.getData());
                    continue;
                }
                if (option == 1) {
                    Pair<List<String>, List<String>> ownFiles = (Pair<List<String>, List<String>>) res.getData();
                    List<String> publicFiles = ownFiles.first();
                    List<String> privateFiles = ownFiles.second();
                    System.out.println("Your Public Files:");
                    for (String fileName : publicFiles) {
                        System.out.println("\t" + fileName);
                    }
                    System.out.println("Your Private Files:");
                    for (String fileName : privateFiles) {
                        System.out.println("\t" + fileName);
                    }
                } else {
                    List<Pair<String, List<String>>> othersFiles = (List<Pair<String, List<String>>>) res.getData();
                    System.out.println("Others' Public Files:");
                    for (Pair<String, List<String>> entry : othersFiles) {
                        String ownerName = entry.first();
                        List<String> publicFiles = entry.second();
                        System.out.println("\tOwner: " + ownerName);
                        for (String fileName : publicFiles) {
                            System.out.println("\t\t" + fileName);
                        }
                    }
                }
            } else if( choice.equals("6")) {
                Request req = new Request("GET", "VIEW_UPLOAD_HISTORY");
                out.writeObject((Object) req);
                out.flush();
                res = (Response) in.readObject();
                List<String> uploadHistory = (List<String>) res.getData();
                System.out.println("Upload History:");
                if( uploadHistory.isEmpty()) {
                    System.out.println("\tNo upload history found.");
                    continue;
                }
                for (String logEntry : uploadHistory) {
                    System.out.println("\t" + logEntry);
                }
            } else if (choice.equals("7")) {
                Request req = new Request("GET", "VIEW_DOWNLOAD_HISTORY");
                out.writeObject((Object) req);
                out.flush();
                res = (Response) in.readObject();
                if(res.getStatus().equals("ERROR")) {
                    System.out.println((String) res.getData());
                    continue;
                }
                List<String> downloadHistory = (List<String>) res.getData();
                System.out.println("Download History:");
                if( downloadHistory.isEmpty()) {
                    System.out.println("\tNo download history found.");
                    continue;
                }
                for (String logEntry : downloadHistory) {
                    System.out.println("\t" + logEntry);
                }
            }else if(choice.equals("8")){
                Request req = new Request("GET", "VIEW_MESSAGES");
                out.writeObject((Object) req);
                out.flush();
                res = (Response) in.readObject();
                if(res.getStatus().equals("ERROR")) {
                    System.out.println((String) res.getData());
                    continue;
                }
                List<String> messages = (List<String>) res.getData();
                System.out.println("Unread Messages:"); 
                if( messages.isEmpty()) {
                    System.out.println("\tNo unread messages.");
                    continue;
                }
                for (String msg : messages) {
                    System.out.println("\t" + msg);
                }
            }
            else if (choice.equals("9")) {
                Request req = new Request("POST", "LogOut");
                out.writeObject((Object) req);
                out.flush();
                break;
            }else {
                System.out.println("Invalid option. Please try again.");
            }
        }

        scanner.close();
        socket.close();
    }
}
