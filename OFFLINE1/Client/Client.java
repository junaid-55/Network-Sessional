package Client;

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
            System.out.println("2. Send message");
            System.out.println("3. Disconnect");
            System.out.print("Choose option: ");
            
            String choice = scanner.nextLine();
            
            if(choice.equals("1")) {
                Request req = new Request("GET","GET_LIVE_USERS");
                out.writeObject((Object)req);
                out.flush();
                Response res = (Response)in.readObject();
                var users = (List<String>)res.getData();
                System.out.println("Live users: " + users);
            } else if(choice.equals("2")) {
                // System.out.print("Enter message: ");
                // String message = scanner.nextLine();
                // out.writeObject(message);
                // out.flush();
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