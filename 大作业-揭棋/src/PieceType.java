public enum PieceType {
    KING(0, "King", "将帅"),
    ROOK(1, "Rook", "车"),
    KNIGHT(2, "Knight", "马"),
    CANNON(3, "Cannon", "炮"),
    PAWN(4, "Pawn", "兵卒"),
    GUARD(5, "Guard", "士仕"),
    BISHOP(6, "Bishop", "象相");

    private final int code;
    private final String protocolName;
    private final String chineseName;

    PieceType(int code, String protocolName, String chineseName) {
        this.code = code;
        this.protocolName = protocolName;
        this.chineseName = chineseName;
    }

    public int getCode() {
        return code;
    }

    public String getProtocolName() {
        return protocolName;
    }

    public String getChineseName() {
        return chineseName;
    }

    public static PieceType fromCode(int code) {
        for (PieceType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        throw new IllegalArgumentException("unknown piece type code: " + code);
    }

    public static PieceType fromProtocolName(String name) {
        if (name == null) {
            return null;
        }
        for (PieceType type : values()) {
            if (type.protocolName.equalsIgnoreCase(name) || type.name().equalsIgnoreCase(name)) {
                return type;
            }
        }
        throw new IllegalArgumentException("unknown piece type: " + name);
    }
}
