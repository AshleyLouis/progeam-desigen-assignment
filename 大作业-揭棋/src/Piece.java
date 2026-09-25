public class Piece {
    private final Color color;
    private final PieceType originalType;
    private PieceType realType;
    private boolean visible;

    public Piece(Color color, PieceType originalType, PieceType realType, boolean visible) {
        this.color = color;
        this.originalType = originalType;
        this.realType = realType;
        this.visible = visible;
    }

    public Color getColor() {
        return color;
    }

    public PieceType getOriginalType() {
        return originalType;
    }

    public PieceType getRealType() {
        return realType;
    }

    public PieceType getMoveType() {
        return visible ? realType : originalType;
    }

    public boolean isVisible() {
        return visible;
    }

    public void reveal(PieceType type) {
        this.realType = type;
        this.visible = true;
    }

    public String display() {
        if (!visible) {
            return color == Color.RED ? "r?" : "b?";
        }
        String colorStr = color == Color.RED ? "r" : "b";
        String typeStr;
        switch (realType) {
            case KING: typeStr = "K"; break;
            case ROOK: typeStr = "R"; break;
            case KNIGHT: typeStr = "N"; break;
            case CANNON: typeStr = "C"; break;
            case PAWN: typeStr = "P"; break;
            case GUARD: typeStr = "G"; break;
            case BISHOP: typeStr = "B"; break;
            default: typeStr = "?";
        }
        return colorStr + typeStr;
    }
}
