import java.util.Map;

public class Move {
    private final Position source;
    private final Position destination;
    private final Integer type;
    private final boolean flip;
    private final long turnStartTime;

    public Move(Position source, Position destination, Integer type, boolean flip, long turnStartTime) {
        this.source = source;
        this.destination = destination;
        this.type = type;
        this.flip = flip;
        this.turnStartTime = turnStartTime;
    }

    public Move(Position source, Position destination, boolean flip) {
        this(source, destination, null, flip, 0L);
    }

    public static Move fromJson(Map<String, String> json) {
        String sourceText = valueOrPair(json, "source", "fromX", "fromY");
        String destText = valueOrPair(json, "destination", "toX", "toY");
        Integer type = null;
        if (json.containsKey("type") && json.get("type").length() > 0) {
            type = Integer.valueOf(json.get("type"));
        }
        boolean flip = "true".equalsIgnoreCase(json.get("isFlip")) || sourceText.equals(destText);
        long time = System.currentTimeMillis();
        if (json.containsKey("turnStartTime")) {
            try {
                time = Long.parseLong(json.get("turnStartTime"));
            } catch (NumberFormatException ignored) {
                time = System.currentTimeMillis();
            }
        }
        return new Move(Position.fromText(sourceText), Position.fromText(destText), type, flip, time);
    }

    public static Move fromTopDownJson(Map<String, String> json) {
        Position source = topDownPosition(json, "fromX", "fromY");
        Position destination = topDownPosition(json, "toX", "toY");
        Integer type = null;
        if (json.containsKey("type") && json.get("type").length() > 0) {
            type = Integer.valueOf(json.get("type"));
        }
        boolean flip = "true".equalsIgnoreCase(json.get("isFlip")) || source.equals(destination);
        long time = System.currentTimeMillis();
        if (json.containsKey("turnStartTime")) {
            try {
                time = Long.parseLong(json.get("turnStartTime"));
            } catch (NumberFormatException ignored) {
                time = System.currentTimeMillis();
            }
        }
        return new Move(source, destination, type, flip, time);
    }

    public static boolean hasOnlyPairCoordinates(Map<String, String> json) {
        return !json.containsKey("source") && !json.containsKey("destination")
                && json.containsKey("fromX") && json.containsKey("fromY")
                && json.containsKey("toX") && json.containsKey("toY");
    }

    private static Position topDownPosition(Map<String, String> json, String xKey, String yKey) {
        String x = json.get(xKey);
        String y = json.get(yKey);
        if (x == null || y == null) {
            throw new IllegalArgumentException("missing pair coordinate fields");
        }
        return new Position(Character.toLowerCase(x.charAt(0)) - 'a', Integer.parseInt(y));
    }

    private static String valueOrPair(Map<String, String> json, String direct, String xKey, String yKey) {
        if (json.containsKey(direct) && json.get(direct) != null && json.get(direct).length() > 0) {
            return json.get(direct);
        }
        String x = json.get(xKey);
        String y = json.get(yKey);
        if (x == null || y == null) {
            throw new IllegalArgumentException("missing position fields: " + direct + " or " + xKey + "/" + yKey);
        }
        return x + y;
    }

    public Position getSource() {
        return source;
    }

    public Position getDestination() {
        return destination;
    }

    public Integer getType() {
        return type;
    }

    public boolean isFlip() {
        return flip;
    }

    public long getTurnStartTime() {
        return turnStartTime;
    }

    public String toRecord(PieceType flipResult) {
        StringBuilder builder = new StringBuilder();
        builder.append(source.toText()).append("->").append(destination.toText());
        if (flipResult != null) {
            builder.append(" reveal=").append(flipResult.getProtocolName());
        }
        return builder.toString();
    }
}
