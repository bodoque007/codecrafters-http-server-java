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
      String message = data.readLine();
      String messageToClient = "HTTP/1.1 200 OK\\r\\n\\r\\n";
      clientOutput.writeUTF(messageToClient);
      clientOutput.flush();
    } catch (IOException e) {
      System.out.println("IOException: " + e.getMessage());
    }
  }
}
