import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.representer.Representer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ConfigManager {

    private final File configFile;
    private Map<String, Object> configMap;
    private final Yaml yaml;

    public ConfigManager(File dataFolder, String fileName) {
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        this.configFile = new File(dataFolder, fileName);

        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Representer representer = new Representer(options);
        this.yaml = new Yaml(new Constructor(), representer, options);

        loadConfig();
    }

    private void loadConfig() {
        if (configFile.exists()) {
            try (FileInputStream fis = new FileInputStream(configFile)) {
                configMap = yaml.load(fis);
                if (configMap == null) {
                    configMap = new HashMap<>();
                }
            } catch (IOException e) {
                e.printStackTrace();
                configMap = new HashMap<>();
            }
        } else {
            configMap = new HashMap<>();
            saveConfig();
        }
    }

    public void saveConfig() {
        try (FileWriter writer = new FileWriter(configFile)) {
            yaml.dump(configMap, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Object getOption(String path) {
        return configMap.get(path);
    }

    public void setOption(String path, Object value) {
        configMap.put(path, value);
        saveConfig();
    }

    public Object getOptionOrDefault(String path, Object defaultValue) {
        Object value = configMap.get(path);
        if (value == null) {
            setOption(path, defaultValue);
            return defaultValue;
        }
        return value;
    }

    public void addDefault(String path, Object value) {
        if (!configMap.containsKey(path)) {
            setOption(path, value);
        }
    }

    public boolean contains(String path) {
        return configMap.containsKey(path);
    }
}
