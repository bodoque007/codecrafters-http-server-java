import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

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

class ClientHandler implements Runnable {
    private static final String HTTP_OK = "HTTP/1.1 200 OK";
    private static final String HTTP_CREATED = "HTTP/1.1 201 Created";
    private static final String HTTP_NOT_FOUND = "HTTP/1.1 404 Not Found";
    private static final String CRLF = "\r\n";

    private final Socket clientSocket;
    private final String directory;

    public ClientHandler(Socket clientSocket, String directory) {
        this.clientSocket = clientSocket;
        this.directory = directory;
    }

    @Override
    public void run() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             DataOutputStream writer = new DataOutputStream(clientSocket.getOutputStream())) {
            
            HttpRequest request = parseRequest(reader);
            String response = handleRequest(request);
            
            writer.writeBytes(response);
            writer.flush();
            
        } catch (IOException e) {
            System.err.println("Error handling client: " + e.getMessage());
        } finally {
            closeSocket();
        }
    }

    private void closeSocket() {
        try {
            clientSocket.close();
        } catch (IOException e) {
            System.err.println("Error closing socket: " + e.getMessage());
        }
    }
    private HttpRequest parseRequest(BufferedReader reader) throws IOException {
        String requestLine = reader.readLine();
        if (requestLine == null) {
            throw new IOException("Invalid request");
        }

        String[] parts = requestLine.split(" ");
        if (parts.length < 2) {
            throw new IOException("Invalid request line");
        }

        String method = parts[0];
        String path = parts[1];
        Map<String, String> headers = new HashMap<>();
        
        String line;
        while ((line = reader.readLine()) != null && !line.isEmpty()) {
            int colonIndex = line.indexOf(':');
            if (colonIndex > 0) {
                String headerName = line.substring(0, colonIndex).trim();
                String headerValue = line.substring(colonIndex + 1).trim();
                headers.put(headerName, headerValue);
            }
        }

        String body = "";
        if ("POST".equals(method) && headers.containsKey("Content-Length")) {
            int contentLength = Integer.parseInt(headers.get("Content-Length"));
            char[] buffer = new char[contentLength];
            int bytesRead = reader.read(buffer, 0, contentLength);
            body = new String(buffer, 0, bytesRead);
        }

        return new HttpRequest(method, path, headers, body);
    }

    private String handleRequest(HttpRequest request) {
        switch (request.method) {
            case "GET":
                return handleGetRequest(request);
            case "POST":
                return handlePostRequest(request);
            default:
                return buildResponse(HTTP_NOT_FOUND, null, null, null);
        }
    }

    private String handleGetRequest(HttpRequest request) {
        String path = request.path;
        
        if ("/".equals(path)) {
            return buildResponse(HTTP_OK, null, null, null);
        }
        
        if (path.startsWith("/echo/")) {
            String message = path.substring("/echo/".length());
            return buildResponse(HTTP_OK, "text/plain", message.length(), message);
        }
        
        if ("/user-agent".equals(path)) {
            String userAgent = request.headers.get("User-Agent");
            if (userAgent != null) {
                return buildResponse(HTTP_OK, "text/plain", userAgent.length(), userAgent);
            }
        }
        
        if (path.startsWith("/files/")) {
            return handleFileRequest(path);
        }
        
        return buildResponse(HTTP_NOT_FOUND, null, null, null);
    }

    private String handlePostRequest(HttpRequest request) {
        String path = request.path;
        
        if (path.startsWith("/files/")) {
            String fileName = path.substring("/files/".length());
            Path filePath = Paths.get(directory, fileName);
            
            if (saveFile(filePath, request.body)) {
                return buildResponse(HTTP_CREATED, null, null, null);
            }
        }
        
        return buildResponse(HTTP_NOT_FOUND, null, null, null);
    }

    private String handleFileRequest(String path) {
        String fileName = path.substring("/files/".length());
        Path filePath = Paths.get(directory, fileName);
        
        if (Files.exists(filePath)) {
            try {
                String content = Files.readString(filePath);
                return buildResponse(HTTP_OK, "application/octet-stream", content.length(), content);
            } catch (IOException e) {
                System.err.println("Error reading file: " + e.getMessage());
            }
        }
        
        return buildResponse(HTTP_NOT_FOUND, null, null, null);
    }
    private boolean saveFile(Path filePath, String content) {
        try {
            Files.write(filePath, content.getBytes());
            System.out.println("File saved successfully to: " + filePath);
            return true;
        } catch (IOException e) {
            System.err.println("Failed to save file: " + e.getMessage());
            return false;
        }
    }

    private String buildResponse(String status, String contentType, Integer contentLength, String body) {
        StringBuilder response = new StringBuilder();
        response.append(status).append(CRLF);
        
        if (contentType != null) {
            response.append("Content-Type: ").append(contentType).append(CRLF);
        }
        
        if (contentLength != null) {
            response.append("Content-Length: ").append(contentLength).append(CRLF);
        }
        
        response.append(CRLF);
        
        if (body != null) {
            response.append(body);
        }
        
        return response.toString();
    }
}

class HttpRequest {
    final String method;
    final String path;
    final Map<String, String> headers;
    final String body;

    HttpRequest(String method, String path, Map<String, String> headers, String body) {
        this.method = method;
        this.path = path;
        this.headers = headers;
        this.body = body;
    }
}
