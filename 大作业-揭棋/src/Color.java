public enum Color {
    RED, BLACK;

    public Color opposite() {
        return this == RED ? BLACK : RED;
    }

    public String protocolName() {
        return this == RED ? "red" : "black";
    }
}
