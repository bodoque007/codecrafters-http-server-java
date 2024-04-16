import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Main {
  public static void main(String[] args) {
    ServerSocket serverSocket = null;
    Socket clientSocket = null;

    try {
      serverSocket = new ServerSocket(4221);
      serverSocket.setReuseAddress(true);
      while (true) {
        clientSocket = serverSocket.accept(); // Wait for connection from client.
        System.out.println("accepted new connection");
        Thread clientHandler = new Thread(new ClientHandler(clientSocket));
        clientHandler.start();
      }
    } catch (IOException e) {
      System.out.println("IOException: " + e.getMessage());
    }
  }
}

class ClientHandler implements Runnable {
  private final Socket clientSocket;

  public ClientHandler(Socket clientSocket) {
    this.clientSocket = clientSocket;
  }

  @Override
  public void run() {
    try {
      // Create input and output streams for the client socket
      BufferedReader data = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
      DataOutputStream clientOutput = new DataOutputStream(clientSocket.getOutputStream());

      // Process the request and generate the response
      String messageToClient = getString(data);
      clientOutput.writeBytes(messageToClient);
      clientOutput.flush();

      // Close the client socket
      clientSocket.close();
    } catch (IOException e) {
      System.out.println("IOException: " + e.getMessage());
    }
  }
  private String getString(BufferedReader clientRequest) throws IOException {
    String line;
    List<String> requestLines = new ArrayList<>();
    String userAgent = null;
    String messageToClient = "";

    while ((line = clientRequest.readLine()) != null && !line.isEmpty()){
      if (line.startsWith("User-Agent:")) {
        String[] parts = line.split(":");
        userAgent = parts[1].trim();
      }
      requestLines.add(line);
    }
    for (String requestLine : requestLines) {
      if (requestLine.startsWith("GET")) {
        String[] requestParts = requestLine.split(" ");
        String path = requestParts[1];
        if (path.equals("/")) {
          messageToClient = "HTTP/1.1 200 OK\r\n\r\n";
        } else if (path.startsWith("/echo/")) {
          String messageToEcho = path.substring("/echo/".length());
          messageToClient = String.format(
                  "HTTP/1.1 200 OK\r\n" +
                          "Content-Type: text/plain\r\n"+
                          "Content-Length: %d\r\n\r\n" +
                          "%s\r\n",
                  messageToEcho.length(), messageToEcho);
        } else if (path.equals("/user-agent")) {
            messageToClient = String.format(
                    "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: text/plain\r\n"+
                            "Content-Length: %d\r\n\r\n" +
                            "%s\r\n",
                    userAgent.length(), userAgent);
        } else if (path.startsWith("/files/")) {
            String fileName = path.substring("/files/".length());
            String directoryName = "/tmp/codecrafters-http-target";
            Path filePath = Paths.get(directoryName, fileName);
            try {
              if (Files.exists(filePath)) {
                String contents = Files.readString(filePath);
                System.out.println(contents);
                messageToClient = String.format(
                        "HTTP/1.1 200 OK\r\n" +
                                "Content-Type: application/octet-stream\r\n"+
                                "Content-Length: %d\r\n\r\n" +
                                "%s\r\n",
                        contents.length(), contents);
              }
            } catch (IOException e) {
              System.out.println("Error reading file:" + e.getMessage());
            }
        }
        else {
          messageToClient = "HTTP/1.1 404 Not Found\r\n\r\n";
        }
      }
    }
    if (messageToClient.isEmpty()) {
      messageToClient = "HTTP/1.1 404 Not Found\r\n\r\n";
    }
    return messageToClient;
  }
}
