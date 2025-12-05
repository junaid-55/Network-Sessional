package Main;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;
import java.util.Scanner;

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
        out.writeObject(name);
        out.flush();
        
        // Read welcome message
        String msg = (String) in.readObject();
        System.out.println(msg);

        // Interactive menu
        while(true) {
            System.out.println("\n1. Get live users");
            System.out.println("2. Send message");
            System.out.println("3. Disconnect");
            System.out.print("Choose option: ");
            
            String choice = scanner.nextLine();
            
            if(choice.equals("1")) {
                out.writeObject("GET_USERS");
                out.flush();
                List<String> users = (List<String>) in.readObject();
                System.out.println("Live users: " + users);
            } else if(choice.equals("2")) {
                System.out.print("Enter message: ");
                String message = scanner.nextLine();
                out.writeObject(message);
                out.flush();
            } else if(choice.equals("3")) {
                out.writeObject("DISCONNECT");
                out.flush();
                break;
            }
        }
        
        scanner.close();
        socket.close();
    }
}