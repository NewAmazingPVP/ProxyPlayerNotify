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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConfigManager {
    private static final Pattern YAML_KEY_PATTERN = Pattern.compile("^\\s*([A-Za-z0-9_.-]+):(?:\\s|$)");

    private final File configFile;
    private Map<String, Object> configMap;
    private final LoadSettings loadSettings;
    private final DumpSettings dumpSettings;
    private String originalContent;
    private final StandardRepresenter representer;
    private final Map<String, String> commentMap;
    private final Set<String> processedPaths;
    private boolean dirty;

    public ConfigManager(File dataFolder, String fileName) {
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        this.configFile = new File(dataFolder, fileName);
        this.commentMap = new LinkedHashMap<>();
        this.processedPaths = new HashSet<>();

        this.dumpSettings = DumpSettings.builder()
                .setDefaultFlowStyle(FlowStyle.BLOCK)
                .setDumpComments(true)
                .setIndent(2)
                .setIndicatorIndent(1)
                .setWidth(4096)
                .build();

        this.representer = new StandardRepresenter(dumpSettings) {
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

        loadConfig();
    }

    private void loadConfig() {
        if (configFile.exists()) {
            try {
                originalContent = Files.readString(configFile.toPath(), StandardCharsets.UTF_8);
                Load loader = new Load(loadSettings);
                Object loaded = loader.loadFromString(originalContent);
                if (loaded instanceof Map<?, ?> map) {
                    configMap = normalizeMap(map);
                } else {
                    configMap = new LinkedHashMap<>();
                }
                extractExistingComments();
                dirty = false;
            } catch (IOException e) {
                e.printStackTrace();
                configMap = new LinkedHashMap<>();
                dirty = false;
            }
        } else {
            configMap = new LinkedHashMap<>();
            dirty = true;
            saveConfig();
        }
    }

    private void extractExistingComments() {
        if (originalContent == null || originalContent.isEmpty()) {
            return;
        }

        String[] lines = originalContent.split("\n");
        StringBuilder currentComment = new StringBuilder();
        Deque<PathEntry> pathStack = new ArrayDeque<>();

        for (String line : lines) {
            String trimmed = line.trim();

            if (trimmed.startsWith("#")) {
                if (currentComment.length() > 0) {
                    currentComment.append("\n");
                }
                currentComment.append(line);
                continue;
            }

            Matcher matcher = YAML_KEY_PATTERN.matcher(line);
            if (matcher.find()) {
                int currentIndent = getIndentLevel(line);
                while (!pathStack.isEmpty() && pathStack.peekLast().indent >= currentIndent) {
                    pathStack.removeLast();
                }

                pathStack.addLast(new PathEntry(currentIndent, matcher.group(1)));
                String currentPath = buildPath(pathStack);

                if (currentComment.length() > 0) {
                    commentMap.put(currentPath, currentComment.toString());
                    processedPaths.add(currentPath);
                    currentComment = new StringBuilder();
                }
            }
        }
    }

    private int getIndentLevel(String line) {
        int indent = 0;
        while (indent < line.length() && line.charAt(indent) == ' ') {
            indent++;
        }
        return indent;
    }

    public void saveConfig() {
        if (!dirty && configFile.exists()) {
            return;
        }

        try {
            Dump dumper = new Dump(dumpSettings, representer);
            String newContent = dumper.dumpToString(configMap);

            String[] lines = newContent.split("\n");
            StringBuilder result = new StringBuilder();
            Deque<PathEntry> pathStack = new ArrayDeque<>();

            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                Matcher matcher = YAML_KEY_PATTERN.matcher(line);
                if (matcher.find()) {
                    int lineIndent = getIndentLevel(line);
                    while (!pathStack.isEmpty() && pathStack.peekLast().indent >= lineIndent) {
                        pathStack.removeLast();
                    }
                    pathStack.addLast(new PathEntry(lineIndent, matcher.group(1)));
                    String fullPath = buildPath(pathStack);
                    String comment = commentMap.get(fullPath);

                    if (comment != null && !comment.isEmpty()) {
                        result.append(comment).append("\n");
                    }
                }

                result.append(line);
                if (i < lines.length - 1) {
                    result.append("\n");
                }
            }

            Files.writeString(configFile.toPath(), result.toString(), StandardCharsets.UTF_8);
            originalContent = result.toString();
            dirty = false;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Object getOption(String path) {
        return getNestedOption(path);
    }

    public void setOption(String path, Object value) {
        Object normalizedValue = normalizeValue(value);
        if (Objects.equals(getNestedOption(path), normalizedValue)) {
            return;
        }
        setNestedOption(path, normalizedValue);
        dirty = true;
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

    public void addDefault(String path, Object value, String comment) {
        boolean changed = false;
        if (!processedPaths.contains(path)) {
            if (getNestedOption(path) == null) {
                setNestedOption(path, normalizeValue(value));
                dirty = true;
                changed = true;
            }
            if (setCommentInternal(path, comment)) {
                dirty = true;
                changed = true;
            }
            processedPaths.add(path);
        }
        if (changed) {
            saveConfig();
        }
    }

    public void setComment(String path, String comment) {
        if (setCommentInternal(path, comment)) {
            dirty = true;
        }
    }

    public String getComment(String path) {
        return commentMap.get(path);
    }

    private String formatComment(String comment) {
        if (comment == null || comment.trim().isEmpty()) {
            return "";
        }

        StringBuilder formatted = new StringBuilder();
        for (String line : comment.split("\n")) {
            if (!line.trim().startsWith("#")) {
                formatted.append("# ");
            }
            formatted.append(line).append("\n");
        }
        return formatted.toString().trim();
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
            Object nested = currentMap.get(keys[i]);
            if (!(nested instanceof Map<?, ?>)) {
                nested = new LinkedHashMap<String, Object>();
                currentMap.put(keys[i], nested);
            }
            currentMap = (Map<String, Object>) nested;
        }
        currentMap.put(keys[keys.length - 1], value);
    }

    private boolean setCommentInternal(String path, String comment) {
        String formattedComment = formatComment(comment);
        String existingComment = commentMap.get(path);
        if (Objects.equals(existingComment, formattedComment)) {
            return false;
        }
        commentMap.put(path, formattedComment);
        return true;
    }

    private Map<String, Object> normalizeMap(Map<?, ?> source) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            normalized.put(String.valueOf(entry.getKey()), normalizeValue(entry.getValue()));
        }
        return normalized;
    }

    private Object normalizeValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            return normalizeMap(map);
        }
        if (value instanceof List<?> list) {
            List<Object> normalized = new ArrayList<>(list.size());
            for (Object item : list) {
                normalized.add(normalizeValue(item));
            }
            return normalized;
        }
        return value;
    }

    private String buildPath(Deque<PathEntry> pathStack) {
        StringBuilder builder = new StringBuilder();
        for (PathEntry entry : pathStack) {
            if (builder.length() > 0) {
                builder.append('.');
            }
            builder.append(entry.key);
        }
        return builder.toString();
    }

    private static final class PathEntry {
        private final int indent;
        private final String key;

        private PathEntry(int indent, String key) {
            this.indent = indent;
            this.key = key;
        }
    }
}
