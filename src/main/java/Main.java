import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {
    private static final int DEFAULT_PORT = 4221;
    private static final String DEFAULT_DIRECTORY = "/tmp";

    public static void main(String[] args) {
        String directory = parseDirectoryArgument(args);
        startServer(directory);
    }

    private static String parseDirectoryArgument(String[] args) {
        for (int i = 0; i < args.length - 1; i++) {
            if ("--directory".equals(args[i])) {
                return args[i + 1];
            }
        }
        return DEFAULT_DIRECTORY;
    }

    private static void startServer(String directory) {
        try (ServerSocket serverSocket = new ServerSocket(DEFAULT_PORT)) {
            serverSocket.setReuseAddress(true);
            System.out.println("HTTP Server started on port " + DEFAULT_PORT);
            
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Accepted new connection");
                new Thread(new ClientHandler(clientSocket, directory)).start();
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }
}
