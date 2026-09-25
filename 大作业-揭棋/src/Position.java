public class Position {
    private final int x;
    private final int y;

    public Position(int x, int y) {
        if (x < 0 || x > 8 || y < 0 || y > 9) {
            throw new IllegalArgumentException("position out of board");
        }
        this.x = x;
        this.y = y;
    }

    public static Position fromText(String text) {
        if (text == null || text.length() != 2) {
            throw new IllegalArgumentException("position must like b3");
        }
        char file = Character.toLowerCase(text.charAt(0));
        char rank = text.charAt(1);
        return new Position(file - 'a', 9 - (rank - '0'));
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public String toText() {
        return String.valueOf((char) ('a' + x)) + (9 - y);
    }

    public int dx(Position other) {
        return Math.abs(x - other.x);
    }

    public int dy(Position other) {
        return Math.abs(y - other.y);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Position)) {
            return false;
        }
        Position other = (Position) obj;
        return x == other.x && y == other.y;
    }

    @Override
    public int hashCode() {
        return x * 31 + y;
    }

    @Override
    public String toString() {
        return toText();
    }
}
