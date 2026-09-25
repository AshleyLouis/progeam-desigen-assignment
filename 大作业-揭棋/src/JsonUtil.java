import java.util.LinkedHashMap;
import java.util.Map;

public class JsonUtil {
    public static Map<String, String> parseObject(String json) {
        Map<String, String> map = new LinkedHashMap<String, String>();
        if (json == null) {
            return map;
        }
        String text = json.trim();
        if (text.startsWith("{")) {
            text = text.substring(1);
        }
        if (text.endsWith("}")) {
            text = text.substring(0, text.length() - 1);
        }
        for (String part : splitTopLevel(text)) {
            int colon = part.indexOf(':');
            if (colon <= 0) {
                continue;
            }
            String key = unquote(part.substring(0, colon).trim());
            String value = unquote(part.substring(colon + 1).trim());
            map.put(key, value);
        }
        return map;
    }

    private static String[] splitTopLevel(String text) {
        java.util.List<String> parts = new java.util.ArrayList<String>();
        StringBuilder current = new StringBuilder();
        boolean inString = false;
        int objectDepth = 0;
        int arrayDepth = 0;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == '"' && (i == 0 || text.charAt(i - 1) != '\\')) {
                inString = !inString;
            }
            if (!inString) {
                if (ch == '{') {
                    objectDepth++;
                } else if (ch == '}') {
                    objectDepth--;
                } else if (ch == '[') {
                    arrayDepth++;
                } else if (ch == ']') {
                    arrayDepth--;
                }
            }
            if (ch == ',' && !inString && objectDepth == 0 && arrayDepth == 0) {
                parts.add(current.toString());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        if (current.length() > 0) {
            parts.add(current.toString());
        }
        return parts.toArray(new String[parts.size()]);
    }

    private static String unquote(String text) {
        if (text.startsWith("\"") && text.endsWith("\"")) {
            text = text.substring(1, text.length() - 1);
        }
        return text.replace("\\\"", "\"").replace("\\\\", "\\");
    }

    public static String object(Object... keyValues) {
        StringBuilder builder = new StringBuilder();
        builder.append("{");
        for (int i = 0; i < keyValues.length; i += 2) {
            if (i > 0) {
                builder.append(",");
            }
            builder.append("\"").append(escape(String.valueOf(keyValues[i]))).append("\":");
            Object value = keyValues[i + 1];
            if (value == null) {
                builder.append("null");
            } else if (value instanceof Number || value instanceof Boolean || String.valueOf(value).startsWith("[")
                    || String.valueOf(value).startsWith("{") || "null".equals(String.valueOf(value))) {
                builder.append(value);
            } else {
                builder.append("\"").append(escape(String.valueOf(value))).append("\"");
            }
        }
        builder.append("}");
        return builder.toString();
    }

    public static String escape(String text) {
        return text.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public static String getMessageType(String json) {
        return parseObject(json).get("messageType");
    }
}
