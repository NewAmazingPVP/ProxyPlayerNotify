package ppn;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

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

    public static void send(String url, String content) {
        if (url == null || url.isEmpty()) {
            return;
        }
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");
            String sanitized = sanitize(content);
            String payload = "{\"content\":\"" + escapeJson(sanitized) + "\"}";
            try (OutputStream os = connection.getOutputStream()) {
                os.write(payload.getBytes(StandardCharsets.UTF_8));
            }
            connection.getInputStream().close();
        } catch (Exception ignored) {
        }
    }

    public static void sendEmbed(String url, String description, int color) {
        if (url == null || url.isEmpty()) {
            return;
        }
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");
            String sanitized = sanitize(description);
            String payload = "{\"embeds\":[{\"description\":\"" + escapeJson(sanitized) + "\",\"color\":" + color + "}]}";
            try (OutputStream os = connection.getOutputStream()) {
                os.write(payload.getBytes(StandardCharsets.UTF_8));
            }
            connection.getInputStream().close();
        } catch (Exception ignored) {
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
}
