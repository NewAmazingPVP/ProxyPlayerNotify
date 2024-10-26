package ppn;

import org.snakeyaml.engine.v2.api.Dump;
import org.snakeyaml.engine.v2.api.DumpSettings;
import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;
import org.snakeyaml.engine.v2.common.FlowStyle;
import org.snakeyaml.engine.v2.common.ScalarStyle;
import org.snakeyaml.engine.v2.nodes.Node;
import org.snakeyaml.engine.v2.nodes.Tag;
import org.snakeyaml.engine.v2.representer.StandardRepresenter;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.representer.Representer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Pattern;


public class ConfigManager {
    private final File configFile;
    private Map<String, Object> configMap;
    private final LoadSettings loadSettings;
    private final DumpSettings dumpSettings;
    private String originalContent;
    private final StandardRepresenter representer;

    public ConfigManager(File dataFolder, String fileName) {
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        this.configFile = new File(dataFolder, fileName);

        this.representer = new StandardRepresenter(DumpSettings.builder().build()) {
            @Override
            protected Node representScalar(Tag tag, String value, ScalarStyle style) {
                if (value.contains("\n")) {
                    return super.representScalar(tag, value, ScalarStyle.LITERAL);
                }
                if (Pattern.compile("[^a-zA-Z0-9._-]").matcher(value).find()) {
                    return super.representScalar(tag, value, ScalarStyle.SINGLE_QUOTED);
                }
                return super.representScalar(tag, value, style);
            }
        };

        this.loadSettings = LoadSettings.builder()
                .setParseComments(true)
                .build();

        this.dumpSettings = DumpSettings.builder()
                .setDefaultFlowStyle(FlowStyle.BLOCK)
                .setDumpComments(true)
                .setIndent(2)
                .setIndicatorIndent(0)
                .setWidth(4096)
                .build();

        loadConfig();
    }

    private void loadConfig() {
        if (configFile.exists()) {
            try {
                originalContent = Files.readString(configFile.toPath(), StandardCharsets.UTF_8);
                Load loader = new Load(loadSettings);
                configMap = (Map<String, Object>) loader.loadFromString(originalContent);
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
        try {
            Dump dumper = new Dump(dumpSettings, representer);

            String newContent = dumper.dumpToString(configMap);

            if (originalContent != null && !originalContent.isEmpty()) {
                newContent = preserveCommentFormat(originalContent, newContent);
            }

            Files.writeString(configFile.toPath(), newContent, StandardCharsets.UTF_8);

            originalContent = newContent;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String preserveCommentFormat(String original, String updated) {
        String[] originalLines = original.split("\n");
        String[] updatedLines = updated.split("\n");

        Map<String, String> keyComments = new LinkedHashMap<>();
        StringBuilder currentComment = new StringBuilder();
        String lastKey = null;

        for (String line : originalLines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("#")) {
                if (currentComment.length() > 0) {
                    currentComment.append("\n");
                }
                currentComment.append(line);
            } else if (!trimmed.isEmpty()) {
                if (trimmed.contains(":")) {
                    lastKey = trimmed.split(":", 2)[0].trim();
                    if (currentComment.length() > 0) {
                        keyComments.put(lastKey, currentComment.toString());
                        currentComment = new StringBuilder();
                    }
                } else if (trimmed.startsWith("-") && lastKey != null) {
                    // Handle list items
                    if (currentComment.length() > 0) {
                        String commentKey = lastKey + "." + trimmed;
                        keyComments.put(commentKey, currentComment.toString());
                        currentComment = new StringBuilder();
                    }
                }
            }
        }

        StringBuilder result = new StringBuilder();
        lastKey = null;
        boolean firstLine = true;

        for (String line : updatedLines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                if (trimmed.contains(":")) {
                    String key = trimmed.split(":", 2)[0].trim();
                    String comments = keyComments.get(key);
                    if (comments != null) {
                        if (!firstLine) {
                            result.append("\n");
                        }
                        result.append(comments).append("\n");
                    }
                    lastKey = key;
                } else if (trimmed.startsWith("-") && lastKey != null) {
                    String commentKey = lastKey + "." + trimmed;
                    String comments = keyComments.get(commentKey);
                    if (comments != null) {
                        result.append(comments).append("\n");
                    }
                }
            }
            result.append(line).append("\n");
            firstLine = false;
        }

        return result.toString().trim();
    }

    public Object getOption(String path) {
        return getNestedOption(path);
    }

    public void setOption(String path, Object value) {
        setNestedOption(path, value);
        saveConfig();
    }

    public Object getOptionOrDefault(String path, Object defaultValue) {
        Object value = getNestedOption(path);
        if (value == null) {
            setOption(path, defaultValue);
            return defaultValue;
        }
        return value;
    }

    public void addDefault(String path, Object value) {
        if (getNestedOption(path) == null) {
            setOption(path, value);
        }
    }

    public boolean contains(String path) {
        return getNestedOption(path) != null;
    }

    public String getString(String path) {
        Object value = getNestedOption(path);
        return value != null ? value.toString() : null;
    }

    public int getInt(String path) {
        Object value = getNestedOption(path);
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }

    public double getDouble(String path) {
        Object value = getNestedOption(path);
        return value instanceof Number ? ((Number) value).doubleValue() : 0.0;
    }

    public boolean getBoolean(String path) {
        Object value = getNestedOption(path);
        return value instanceof Boolean ? (Boolean) value : false;
    }

    public byte getByte(String path) {
        Object value = getNestedOption(path);
        return value instanceof Number ? ((Number) value).byteValue() : 0;
    }

    public short getShort(String path) {
        Object value = getNestedOption(path);
        return value instanceof Number ? ((Number) value).shortValue() : 0;
    }

    public long getLong(String path) {
        Object value = getNestedOption(path);
        return value instanceof Number ? ((Number) value).longValue() : 0L;
    }

    public float getFloat(String path) {
        Object value = getNestedOption(path);
        return value instanceof Number ? ((Number) value).floatValue() : 0.0f;
    }

    public char getChar(String path) {
        Object value = getNestedOption(path);
        return value instanceof Character ? (Character) value : '\u0000';
    }

    public List<Byte> getByteList(String path) {
        List<?> list = getList(path);
        List<Byte> result = new ArrayList<>();
        for (Object object : list) {
            if (object instanceof Number) {
                result.add(((Number) object).byteValue());
            }
        }
        return result;
    }

    public List<Short> getShortList(String path) {
        List<?> list = getList(path);
        List<Short> result = new ArrayList<>();
        for (Object object : list) {
            if (object instanceof Number) {
                result.add(((Number) object).shortValue());
            }
        }
        return result;
    }

    public Set<String> getKeys(String path) {
        Object section = getNestedOption(path);
        if (section instanceof Map) {
            return ((Map<String, Object>) section).keySet();
        }
        return Collections.emptySet();
    }

    public Map<String, Object> getSection(String path) {
        Object section = getNestedOption(path);
        if (section instanceof Map) {
            return (Map<String, Object>) section;
        }
        return Collections.emptyMap();
    }

    public List<Integer> getIntList(String path) {
        List<?> list = getList(path);
        List<Integer> result = new ArrayList<>();
        for (Object object : list) {
            if (object instanceof Number) {
                result.add(((Number) object).intValue());
            }
        }
        return result;
    }

    public List<Long> getLongList(String path) {
        List<?> list = getList(path);
        List<Long> result = new ArrayList<>();
        for (Object object : list) {
            if (object instanceof Number) {
                result.add(((Number) object).longValue());
            }
        }
        return result;
    }

    public List<Float> getFloatList(String path) {
        List<?> list = getList(path);
        List<Float> result = new ArrayList<>();
        for (Object object : list) {
            if (object instanceof Number) {
                result.add(((Number) object).floatValue());
            }
        }
        return result;
    }

    public List<Double> getDoubleList(String path) {
        List<?> list = getList(path);
        List<Double> result = new ArrayList<>();
        for (Object object : list) {
            if (object instanceof Number) {
                result.add(((Number) object).doubleValue());
            }
        }
        return result;
    }

    public List<Boolean> getBooleanList(String path) {
        List<?> list = getList(path);
        List<Boolean> result = new ArrayList<>();
        for (Object object : list) {
            if (object instanceof Boolean) {
                result.add((Boolean) object);
            }
        }
        return result;
    }

    public List<Character> getCharList(String path) {
        List<?> list = getList(path);
        List<Character> result = new ArrayList<>();
        for (Object object : list) {
            if (object instanceof Character) {
                result.add((Character) object);
            }
        }
        return result;
    }

    public List<String> getStringList(String path) {
        List<?> list = getList(path);
        List<String> result = new ArrayList<>();
        for (Object object : list) {
            if (object instanceof String) {
                result.add((String) object);
            }
        }
        return result;
    }

    public List<?> getList(String path) {
        Object value = getNestedOption(path);
        return value instanceof List<?> ? (List<?>) value : Collections.emptyList();
    }

    private Object getNestedOption(String path) {
        String[] keys = path.split("\\.");
        Map<String, Object> currentMap = configMap;
        for (int i = 0; i < keys.length - 1; i++) {
            Object nested = currentMap.get(keys[i]);
            if (nested instanceof Map) {
                currentMap = (Map<String, Object>) nested;
            } else {
                return null;
            }
        }
        return currentMap.get(keys[keys.length - 1]);
    }

    private void setNestedOption(String path, Object value) {
        String[] keys = path.split("\\.");
        Map<String, Object> currentMap = configMap;
        for (int i = 0; i < keys.length - 1; i++) {
            currentMap = (Map<String, Object>) currentMap.computeIfAbsent(keys[i], k -> new HashMap<>());
        }
        currentMap.put(keys[keys.length - 1], value);
    }
}