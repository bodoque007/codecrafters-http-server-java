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
import java.util.concurrent.atomic.AtomicReference;

public class Main {
  public static void main(String[] args) {
    String directory = null;

    // Iterate through the command line arguments to find the --directory flag
    for (int i = 0; i < args.length; i++) {
      if (args[i].equals("--directory") && i + 1 < args.length) {
        // The directory value is the argument following the --directory flag
        directory = args[i + 1];
        break; // Stop searching once we find the flag
      }
    }
    ServerSocket serverSocket = null;
    Socket clientSocket = null;

    try {
      serverSocket = new ServerSocket(4221);
      serverSocket.setReuseAddress(true);
      while (true) {
        clientSocket = serverSocket.accept(); // Wait for connection from client.
        System.out.println("accepted new connection");
        Thread clientHandler = new Thread(new ClientHandler(clientSocket, directory));
        clientHandler.start();
      }
    } catch (IOException e) {
      System.out.println("IOException: " + e.getMessage());
    }
  }
}

class ClientHandler implements Runnable {
  private final Socket clientSocket;
  private String directory;

  public ClientHandler(Socket clientSocket, String directory) {
    this.clientSocket = clientSocket;
    this.directory = directory;
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
  private void saveFile(Path filePath, String fileContent) {
    // Save the file content to the file path
    try {
      Files.write(filePath, fileContent.getBytes());
      System.out.println("File saved successfully to: " + filePath.toString());
    } catch (IOException e) {
      System.err.println("Failed to save file: " + e.getMessage());
    }
  }
  private String getString(BufferedReader clientRequest) throws IOException {
    String userAgent = null;
    String messageToClient = "";
    List<String> requestLines = new ArrayList<>();
    int contentLength = 0;
    String line;
    while ((line = clientRequest.readLine()) != null && !line.isEmpty()) {
      requestLines.add(line);
      // Check for User-Agent header
      if (line.startsWith("User-Agent:")) {
        String[] parts = line.split(":");
        userAgent = parts[1].trim();
      } else if (line.startsWith("Content-Length:")) {
        String[]parts = line.split(":");
        contentLength = Integer.parseInt(parts[1].trim());
      }
    }
    for (String requestLine : requestLines) {
      if (requestLine.startsWith("POST")) {
        String[] requestParts = requestLine.split(" ");
        String path = requestParts[1];
        if (path.startsWith("/files/")) {

          // Read the body separately (if there is any)
          char[] bodyContent = new char[contentLength];
          int bytesRead = clientRequest.read(bodyContent, 0, contentLength);
          String requestBody = new String(bodyContent, 0, bytesRead);
          System.out.println(requestBody);

          String fileName = path.substring("/files/".length());
          Path filePath = Paths.get(directory, fileName);
          saveFile(filePath, requestBody);
          messageToClient = "HTTP/1.1 201 Created\r\n\r\n";
        }
      }
      else if (requestLine.startsWith("GET")) {
        String[] requestParts = requestLine.split(" ");
        String path = requestParts[1];
        if (path.equals("/")) {
          messageToClient = "HTTP/1.1 200 OK\r\n\r\n";
        } else if (path.startsWith("/echo/")) {
          String messageToEcho = path.substring("/echo/".length());
          messageToClient = buildGetResponse(messageToEcho.length(), messageToEcho, "text/plain" );
        } else if (path.equals("/user-agent")) {
            messageToClient = buildGetResponse(userAgent.length(), userAgent, "text/plain");
        } else if (path.startsWith("/files/")) {
            String fileName = path.substring("/files/".length());
            Path filePath = Paths.get(directory, fileName);
            try {
              if (Files.exists(filePath)) {
                String contents = Files.readString(filePath);
                System.out.println(contents);
                messageToClient = buildGetResponse(contents.length(), contents, "application/octet-stream");
              }
            } catch (IOException e) {
              System.out.println("Error reading file:" + e.getMessage());
            }
        }
      }
    }
    if (messageToClient.isEmpty()) {
      messageToClient = "HTTP/1.1 404 Not Found\r\n\r\n";
    }
    return messageToClient;
  }
  private String buildGetResponse(int length, String content, String contentType) {
    return String.format(
            "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: %s\r\n"+
                    "Content-Length: %d\r\n\r\n" +
                    "%s\r\n",
            contentType, length, content);
  }
}
