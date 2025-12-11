package Client;

import java.io.File;
import java.io.FileInputStream;
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

        // Send client name
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter your name: ");
        String name = scanner.nextLine();
        Request request = new Request("POST","login", (Object)name);

        out.writeObject(request);
        out.flush();
        
        Response res = (Response) in.readObject();
        System.out.println((String)res.getData()+"\n");
        if(res.getStatus().equals("ERROR")) {
            socket.close();
            scanner.close();
            return;
        }
        // Interactive menu
        while(true) {
            System.out.println("\n1. Get live users");
            System.out.println("2. Upload file");
            System.out.println("3. Disconnect");
            System.out.print("Choose option: ");
            
            String choice = scanner.nextLine();
            
            if(choice.equals("1")) {
                Request req = new Request("GET","GET_LIVE_USERS");
                out.writeObject((Object)req);
                out.flush();
                res = (Response)in.readObject();
                var users = (List<String>)res.getData();
                System.out.println("Live users: " + users);
            } else if(choice.equals("2")) {
                String filePath;
                System.out.print("Enter file path to upload: ");
                filePath = scanner.nextLine();
                String fileName = filePath.substring(filePath.lastIndexOf("/") + 1);
                System.out.println("Uploading file: " + fileName+" with Size "+new java.io.File(filePath).length());
                FileDescriptor fileDesc = new FileDescriptor(fileName, new java.io.File(filePath).length()); 
                Request req = new Request("POST","UPLOAD_FILE", (Object)fileDesc);
                out.writeObject((Object)req);
                out.flush();
                res = (Response)in.readObject();
                Pair<Integer, Long> pair = (Pair<Integer, Long>)res.getData();
                int chunk_size = pair.first();
                System.out.println("Server approved upload. Chunk size: " + chunk_size + " bytes");
                for(int i = 0 ;i < new File(filePath).length(); i += chunk_size) {
                    int bytesToSend = (int)Math.min(chunk_size, new java.io.File(filePath).length() - i);
                    byte[] buffer = new byte[bytesToSend];
                    try (FileInputStream fis = new FileInputStream(filePath)) {
                        fis.skip(i);
                        fis.read(buffer, 0, bytesToSend);
                    }catch (IOException e) {
                        e.printStackTrace();
                    }
                    out.writeObject(new Request("POST","FILE_CHUNK", (Object)buffer, pair.second()));
                    out.flush();
                    System.out.println("Sent chunk of size: " + bytesToSend + " bytes");
                }
            } else if(choice.equals("3")) {
                Request req = new Request("POST","LogOut");
                out.writeObject((Object)req);
                out.flush();
                break;
            }
        }
        
        scanner.close();
        socket.close();
    }
}