public class ProtocolUtil {
    private ProtocolUtil() {
    }

    public static String moveJson(Move move) {
        return JsonUtil.object(
                "source", move.getSource().toText(),
                "destination", move.getDestination().toText(),
                "fromX", xText(move.getSource()),
                "fromY", move.getSource().getY(),
                "toX", xText(move.getDestination()),
                "toY", move.getDestination().getY(),
                "isFlip", move.isFlip());
    }

    public static Object[] moveFields(Move move) {
        return new Object[]{
                "source", move.getSource().toText(),
                "destination", move.getDestination().toText(),
                "fromX", xText(move.getSource()),
                "fromY", move.getSource().getY(),
                "toX", xText(move.getDestination()),
                "toY", move.getDestination().getY(),
                "isFlip", move.isFlip()
        };
    }

    public static String pieceTypeName(PieceType type) {
        if (type == null) {
            return "";
        }
        return type.getProtocolName();
    }

    private static String xText(Position position) {
        return String.valueOf((char) ('a' + position.getX()));
    }
}
