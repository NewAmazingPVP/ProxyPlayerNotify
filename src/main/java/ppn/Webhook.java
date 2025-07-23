package ppn;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class Webhook {
    public static void send(String url, String content) {
        if (url == null || url.isEmpty()) {
            return;
        }
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");
            String payload = "{\"content\":\"" + content.replace("\"", "\\\"") + "\"}";
            try (OutputStream os = connection.getOutputStream()) {
                os.write(payload.getBytes(StandardCharsets.UTF_8));
            }
            connection.getInputStream().close();
        } catch (Exception ignored) {
        }
    }
}