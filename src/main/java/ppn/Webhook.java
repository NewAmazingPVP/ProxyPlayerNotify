package ppn;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public class Webhook {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final PlainTextComponentSerializer PLAIN_SERIALIZER = PlainTextComponentSerializer.plainText();
    private static final LegacyComponentSerializer AMPERSAND_SERIALIZER = LegacyComponentSerializer.legacyAmpersand();
    private static final LegacyComponentSerializer SECTION_SERIALIZER = LegacyComponentSerializer.legacySection();
    private static final Pattern LEGACY_CODE_PATTERN = Pattern.compile("(?i)[&§][0-9A-FK-ORX]");
    private static final Pattern RGB_HEX_PATTERN = Pattern.compile("(?i)&#[0-9A-F]{6}");
    private static final Pattern SHORT_RGB_HEX_PATTERN = Pattern.compile("(?i)&#[0-9A-F]{3}");
    private static final Pattern BUKKIT_HEX_PATTERN = Pattern.compile("(?i)&x(?:&[0-9A-F]){6}");

    public static WebhookResult send(String url, String content) {
        return execute(url, content, false, 0);
    }

    public static WebhookResult sendEmbed(String url, String description, int color) {
        return execute(url, description, true, color);
    }

    private static WebhookResult execute(String url, String content, boolean embed, int color) {
        if (url == null || url.isEmpty()) {
            return WebhookResult.skipped("No webhook URL configured");
        }
        String sanitized = sanitize(content);
        if (sanitized.isEmpty()) {
            return WebhookResult.skipped("Webhook payload is empty after sanitizing");
        }
        String payload = embed
                ? "{\"embeds\":[{\"description\":\"" + escapeJson(sanitized) + "\",\"color\":" + color + "}]}"
                : "{\"content\":\"" + escapeJson(sanitized) + "\"}";

        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(7000);
            connection.setRequestProperty("Content-Type", "application/json");

            try (OutputStream os = connection.getOutputStream()) {
                os.write(payload.getBytes(StandardCharsets.UTF_8));
            }

            int status = connection.getResponseCode();
            if (status >= 200 && status < 300) {
                closeQuietly(connection.getInputStream());
                return WebhookResult.success(status);
            }

            String body = readBody(connection.getErrorStream());
            return WebhookResult.failure(status, body);
        } catch (Exception ex) {
            return WebhookResult.failure(-1, ex.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static String sanitize(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        String sanitized = input;

        try {
            Component component = MINI_MESSAGE.deserialize(input);
            sanitized = PLAIN_SERIALIZER.serialize(component);
        } catch (Exception ignored) {
        }

        if (LEGACY_CODE_PATTERN.matcher(input).find()) {
            Component legacyComponent = AMPERSAND_SERIALIZER.deserialize(input);
            sanitized = PLAIN_SERIALIZER.serialize(legacyComponent);
        } else if (input.indexOf('§') >= 0) {
            Component legacyComponent = SECTION_SERIALIZER.deserialize(input);
            sanitized = PLAIN_SERIALIZER.serialize(legacyComponent);
        }

        sanitized = RGB_HEX_PATTERN.matcher(sanitized).replaceAll("");
        sanitized = SHORT_RGB_HEX_PATTERN.matcher(sanitized).replaceAll("");
        sanitized = BUKKIT_HEX_PATTERN.matcher(sanitized).replaceAll("");
        sanitized = LEGACY_CODE_PATTERN.matcher(sanitized).replaceAll("");
        return sanitized;
    }

    private static String escapeJson(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        String escaped = input.replace("\\", "\\\\").replace("\"", "\\\"");
        escaped = escaped.replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
        return escaped;
    }

    private static String readBody(InputStream stream) {
        if (stream == null) {
            return "";
        }
        try {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            return "";
        } finally {
            closeQuietly(stream);
        }
    }

    private static void closeQuietly(InputStream stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (Exception ignored) {
            }
        }
    }

    public static class WebhookResult {
        private final boolean success;
        private final int statusCode;
        private final String error;

        private WebhookResult(boolean success, int statusCode, String error) {
            this.success = success;
            this.statusCode = statusCode;
            this.error = error;
        }

        public static WebhookResult success(int statusCode) {
            return new WebhookResult(true, statusCode, "");
        }

        public static WebhookResult failure(int statusCode, String error) {
            return new WebhookResult(false, statusCode, error == null ? "" : error);
        }

        public static WebhookResult skipped(String reason) {
            return new WebhookResult(true, 0, reason);
        }

        public boolean isSuccess() {
            return success;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public String getError() {
            return error;
        }
    }
}
