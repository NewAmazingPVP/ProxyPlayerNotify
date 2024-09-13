package bpn.velocity;

import com.moandjiezana.toml.Toml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class ConfigLoader {

    public static Toml loadConfig(Path path) {
        File folder = path.toFile();
        File file = new File(folder, "config.toml");

        if (!file.exists()) {
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            try (InputStream input = ConfigLoader.class.getResourceAsStream("/" + file.getName())) {
                if (input != null) {
                    Files.copy(input, file.toPath());
                } else {
                    file.createNewFile();
                }
            } catch (IOException exception) {
                exception.printStackTrace();
                return null;
            }
        }
        return new Toml().read(file);
    }

    public static void loadServerNames(Toml config, Map<String, String> serverNames) {
        serverNames.clear();
        Toml serverNamesConfig = config.getTable("ServerNames");
        if (serverNamesConfig != null) {
            for (Map.Entry<String, Object> entry : serverNamesConfig.entrySet()) {
                serverNames.put(entry.getKey().toLowerCase(), entry.getValue().toString());
            }
        }
    }
}