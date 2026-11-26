package com.airtribe.taskmanager.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A very small JSON reader and writer, so the project needs no external library.
 *
 * <p>Reading turns JSON text into plain Java values:
 * object -> {@code Map<String, Object>} (keeps key order), array -> {@code List<Object>},
 * string -> {@code String}, whole number -> {@code Long}, other number -> {@code Double},
 * true/false -> {@code Boolean}, null -> {@code null}.
 *
 * <p>Writing does the reverse. It also accepts any {@link Iterable}, any {@link Number}
 * and enums (written as their name).
 */
public final class SimpleJson {

    private SimpleJson() {
    }

    /** Thrown when the text is not valid JSON. */
    public static final class JsonException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public JsonException(String message) {
            super(message);
        }
    }

    // ------------------------------------------------------------------ reading

    /** Parses any JSON value. */
    public static Object parse(String text) {
        if (text == null) {
            throw new JsonException("JSON text is null");
        }
        Reader reader = new Reader(text);
        reader.skipWhitespace();
        Object value = reader.readValue();
        reader.skipWhitespace();
        if (!reader.atEnd()) {
            throw reader.error("unexpected text after the JSON value");
        }
        return value;
    }

    /** Parses text that must be a JSON object. */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseObject(String text) {
        Object value = parse(text);
        if (!(value instanceof Map)) {
            throw new JsonException("expected a JSON object");
        }
        return (Map<String, Object>) value;
    }

    private static final class Reader {
        private final String text;
        private int pos;

        Reader(String text) {
            this.text = text;
        }

        boolean atEnd() {
            return pos >= text.length();
        }

        void skipWhitespace() {
            while (!atEnd() && Character.isWhitespace(text.charAt(pos))) {
                pos++;
            }
        }

        JsonException error(String message) {
            return new JsonException(message + " at position " + pos);
        }

        Object readValue() {
            if (atEnd()) {
                throw error("unexpected end of JSON");
            }
            char c = text.charAt(pos);
            switch (c) {
                case '{':
                    return readObject();
                case '[':
                    return readArray();
                case '"':
                    return readString();
                case 't':
                    return readLiteral("true", Boolean.TRUE);
                case 'f':
                    return readLiteral("false", Boolean.FALSE);
                case 'n':
                    return readLiteral("null", null);
                default:
                    if (c == '-' || (c >= '0' && c <= '9')) {
                        return readNumber();
                    }
                    throw error("unexpected character '" + c + "'");
            }
        }

        private Map<String, Object> readObject() {
            Map<String, Object> map = new LinkedHashMap<>();
            pos++; // '{'
            skipWhitespace();
            if (!atEnd() && text.charAt(pos) == '}') {
                pos++;
                return map;
            }
            while (true) {
                skipWhitespace();
                if (atEnd() || text.charAt(pos) != '"') {
                    throw error("expected a string key");
                }
                String key = readString();
                skipWhitespace();
                expect(':');
                skipWhitespace();
                map.put(key, readValue());
                skipWhitespace();
                if (atEnd()) {
                    throw error("unterminated object");
                }
                char c = text.charAt(pos++);
                if (c == '}') {
                    return map;
                }
                if (c != ',') {
                    throw error("expected ',' or '}'");
                }
            }
        }

        private List<Object> readArray() {
            List<Object> list = new ArrayList<>();
            pos++; // '['
            skipWhitespace();
            if (!atEnd() && text.charAt(pos) == ']') {
                pos++;
                return list;
            }
            while (true) {
                skipWhitespace();
                list.add(readValue());
                skipWhitespace();
                if (atEnd()) {
                    throw error("unterminated array");
                }
                char c = text.charAt(pos++);
                if (c == ']') {
                    return list;
                }
                if (c != ',') {
                    throw error("expected ',' or ']'");
                }
            }
        }

        private String readString() {
            pos++; // opening quote
            StringBuilder sb = new StringBuilder();
            while (true) {
                if (atEnd()) {
                    throw error("unterminated string");
                }
                char c = text.charAt(pos++);
                if (c == '"') {
                    return sb.toString();
                }
                if (c != '\\') {
                    sb.append(c);
                    continue;
                }
                if (atEnd()) {
                    throw error("unterminated escape");
                }
                char e = text.charAt(pos++);
                switch (e) {
                    case '"': sb.append('"'); break;
                    case '\\': sb.append('\\'); break;
                    case '/': sb.append('/'); break;
                    case 'b': sb.append('\b'); break;
                    case 'f': sb.append('\f'); break;
                    case 'n': sb.append('\n'); break;
                    case 'r': sb.append('\r'); break;
                    case 't': sb.append('\t'); break;
                    case 'u':
                        if (pos + 4 > text.length()) {
                            throw error("bad unicode escape");
                        }
                        try {
                            sb.append((char) Integer.parseInt(text.substring(pos, pos + 4), 16));
                        } catch (NumberFormatException ex) {
                            throw error("bad unicode escape");
                        }
                        pos += 4;
                        break;
                    default:
                        throw error("bad escape '\\" + e + "'");
                }
            }
        }

        private Object readNumber() {
            int start = pos;
            if (text.charAt(pos) == '-') {
                pos++;
            }
            boolean decimal = false;
            while (!atEnd()) {
                char c = text.charAt(pos);
                if (c >= '0' && c <= '9') {
                    pos++;
                } else if (c == '.' || c == 'e' || c == 'E' || c == '+' || c == '-') {
                    decimal = true;
                    pos++;
                } else {
                    break;
                }
            }
            String number = text.substring(start, pos);
            try {
                if (!decimal) {
                    return Long.parseLong(number);
                }
                return Double.parseDouble(number);
            } catch (NumberFormatException ex) {
                throw error("bad number '" + number + "'");
            }
        }

        private Object readLiteral(String word, Object value) {
            if (!text.startsWith(word, pos)) {
                throw error("unexpected text");
            }
            pos += word.length();
            return value;
        }

        private void expect(char c) {
            if (atEnd() || text.charAt(pos) != c) {
                throw error("expected '" + c + "'");
            }
            pos++;
        }
    }

    // ------------------------------------------------------------------ writing

    /** Writes a value as compact JSON on one line. */
    public static String write(Object value) {
        StringBuilder sb = new StringBuilder();
        writeValue(sb, value, false, 0);
        return sb.toString();
    }

    /** Writes a value as indented, multi-line JSON. */
    public static String writePretty(Object value) {
        StringBuilder sb = new StringBuilder();
        writeValue(sb, value, true, 0);
        return sb.toString();
    }

    private static void writeValue(StringBuilder sb, Object value, boolean pretty, int indent) {
        if (value == null) {
            sb.append("null");
        } else if (value instanceof String) {
            writeString(sb, (String) value);
        } else if (value instanceof Boolean || value instanceof Integer || value instanceof Long
                || value instanceof Short || value instanceof Byte) {
            sb.append(value);
        } else if (value instanceof Number) {
            double d = ((Number) value).doubleValue();
            sb.append(Double.isFinite(d) ? value.toString() : "null");
        } else if (value instanceof Enum) {
            writeString(sb, ((Enum<?>) value).name());
        } else if (value instanceof Map) {
            writeObject(sb, (Map<?, ?>) value, pretty, indent);
        } else if (value instanceof Iterable) {
            writeArray(sb, (Iterable<?>) value, pretty, indent);
        } else {
            writeString(sb, value.toString());
        }
    }

    private static void writeObject(StringBuilder sb, Map<?, ?> map, boolean pretty, int indent) {
        if (map.isEmpty()) {
            sb.append("{}");
            return;
        }
        sb.append('{');
        boolean first = true;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!first) {
                sb.append(',');
            }
            first = false;
            newLine(sb, pretty, indent + 1);
            writeString(sb, String.valueOf(entry.getKey()));
            sb.append(pretty ? ": " : ":");
            writeValue(sb, entry.getValue(), pretty, indent + 1);
        }
        newLine(sb, pretty, indent);
        sb.append('}');
    }

    private static void writeArray(StringBuilder sb, Iterable<?> items, boolean pretty, int indent) {
        if (!items.iterator().hasNext()) {
            sb.append("[]");
            return;
        }
        sb.append('[');
        boolean first = true;
        for (Object item : items) {
            if (!first) {
                sb.append(',');
            }
            first = false;
            newLine(sb, pretty, indent + 1);
            writeValue(sb, item, pretty, indent + 1);
        }
        newLine(sb, pretty, indent);
        sb.append(']');
    }

    private static void newLine(StringBuilder sb, boolean pretty, int indent) {
        if (!pretty) {
            return;
        }
        sb.append('\n');
        for (int i = 0; i < indent; i++) {
            sb.append("  ");
        }
    }

    private static void writeString(StringBuilder sb, String s) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append('"');
    }
}
