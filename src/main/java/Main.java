import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

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

      String clientRequest = data.readLine();

      String messageToClient = getString(clientRequest);
      clientOutput.writeBytes(messageToClient);
      clientOutput.flush();
    } catch (IOException e) {
      System.out.println("IOException: " + e.getMessage());
    }
  }

  private static String getString(String clientRequest) {
    String[] requestParts = clientRequest.split(" ");
    String path = requestParts[1];
    String messageToClient = "";
    if (path.equals("/")) {
      messageToClient = "HTTP/1.1 200 OK\r\n\r\n";
    } else if (path.startsWith("/echo/")) {
        String messageToEcho = path.substring("/echo/".length());
        messageToClient = String.format(
                        "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: text/plain\r\n"+
                        "Content-length: %d\r\n" +
                                "%s\r\n\r\n",
                        messageToEcho.length(), messageToEcho);
    } else {
        messageToClient = "HTTP/1.1 404 Not Found\r\n\r\n";
    }
    return messageToClient;
  }
}
