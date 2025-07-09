import java.util.Map;

record HttpRequest(String method, String path, Map<String, String> headers, String body) {
}