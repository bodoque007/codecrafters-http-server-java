import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Main {
  public static void main(String[] args) {
    ServerSocket serverSocket = null;
    Socket clientSocket = null;

    try {
      serverSocket = new ServerSocket(4221);
      serverSocket.setReuseAddress(true);
      clientSocket = serverSocket.accept(); // Wait for connection from client.
      System.out.println("accepted new connection");
      DataOutputStream clientOutput = new DataOutputStream(clientSocket.getOutputStream());
      BufferedReader data = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));


      String messageToClient = getString(data);
      clientOutput.writeBytes(messageToClient);
      clientOutput.flush();
    } catch (IOException e) {
      System.out.println("IOException: " + e.getMessage());
    }
  }

  private static String getString(BufferedReader clientRequest) throws IOException {
    String line;
    List<String> requestLines = new ArrayList<>();
    String userAgent = null;
    String messageToClient = null;

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
          }
          else {
            messageToClient = "HTTP/1.1 404 Not Found\r\n\r\n";
          }
      }
    }
    return messageToClient;
  }
}
