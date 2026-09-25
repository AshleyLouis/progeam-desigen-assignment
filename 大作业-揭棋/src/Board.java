import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
public class Board {
    private final Piece[][] grid = new Piece[10][9];
    private final List<String> records = new ArrayList<String>();
    private int noCaptureSteps;
    private Long randomSeed;

    public Board() {
        initialize();
    }

    public Board(Long seed) {
        this.randomSeed = seed;
        initialize();
    }

    public Board(boolean empty) {
    }

    public void reset(Long seed) {
        this.randomSeed = seed;
        records.clear();
        noCaptureSteps = 0;
        for (int y = 0; y < 10; y++) {
            for (int x = 0; x < 9; x++) {
                grid[y][x] = null;
            }
        }
        initialize();
    }

    private void initialize() {
        setupSide(Color.BLACK, 0, 2, 3);
        setupSide(Color.RED, 9, 7, 6);
    }

    private void setupSide(Color color, int backRank, int cannonRank, int pawnRank) {
        List<PieceType> hiddenTypes = shuffledHiddenTypes(color);
        int index = 0;
        PieceType[] back = {
                PieceType.ROOK, PieceType.KNIGHT, PieceType.BISHOP, PieceType.GUARD, PieceType.KING,
                PieceType.GUARD, PieceType.BISHOP, PieceType.KNIGHT, PieceType.ROOK
        };
        for (int x = 0; x < back.length; x++) {
            if (back[x] == PieceType.KING) {
                grid[backRank][x] = new Piece(color, PieceType.KING, PieceType.KING, true);
            } else {
                grid[backRank][x] = new Piece(color, back[x], hiddenTypes.get(index++), false);
            }
        }
        grid[cannonRank][1] = new Piece(color, PieceType.CANNON, hiddenTypes.get(index++), false);
        grid[cannonRank][7] = new Piece(color, PieceType.CANNON, hiddenTypes.get(index++), false);
        for (int x = 0; x <= 8; x += 2) {
            grid[pawnRank][x] = new Piece(color, PieceType.PAWN, hiddenTypes.get(index++), false);
        }
    }

    private List<PieceType> shuffledHiddenTypes(Color color) {
        List<PieceType> types = new ArrayList<PieceType>();
        add(types, PieceType.ROOK, 2);
        add(types, PieceType.KNIGHT, 2);
        add(types, PieceType.CANNON, 2);
        add(types, PieceType.BISHOP, 2);
        add(types, PieceType.GUARD, 2);
        add(types, PieceType.PAWN, 5);
        Random rand = randomSeed != null 
            ? new Random(randomSeed + color.ordinal()) 
            : new Random(System.nanoTime() + color.ordinal());
        Collections.shuffle(types, rand);
        return types;
    }

    private void add(List<PieceType> types, PieceType type, int count) {
        for (int i = 0; i < count; i++) {
            types.add(type);
        }
    }

    public Piece get(Position position) {
        return grid[position.getY()][position.getX()];
    }

    public void set(Position position, Piece piece) {
        grid[position.getY()][position.getX()] = piece;
    }

    public Piece move(Move move) {
        Piece moving = get(move.getSource());
        Piece captured = get(move.getDestination());
        set(move.getDestination(), moving);
        set(move.getSource(), null);
        noCaptureSteps = captured == null ? noCaptureSteps + 1 : 0;
        return captured;
    }

    public void record(String record) {
        records.add(record);
    }

    public int getNoCaptureSteps() {
        return noCaptureSteps;
    }

    public boolean hasKing(Color color) {
        return findKing(color) != null;
    }

    public Position findKing(Color color) {
        for (int y = 0; y < 10; y++) {
            for (int x = 0; x < 9; x++) {
                Piece piece = grid[y][x];
                if (piece != null && piece.getColor() == color && piece.isVisible()
                        && piece.getRealType() == PieceType.KING) {
                    return new Position(x, y);
                }
            }
        }
        return null;
    }

    public List<Position> getPiecesOfColor(Color color) {
        List<Position> result = new ArrayList<Position>();
        for (int y = 0; y < 10; y++) {
            for (int x = 0; x < 9; x++) {
                Piece piece = grid[y][x];
                if (piece != null && piece.getColor() == color) {
                    result.add(new Position(x, y));
                }
            }
        }
        return result;
    }

    public String boardJsonFor(Color viewer) {
        StringBuilder builder = new StringBuilder();
        builder.append("[");
        boolean first = true;
        for (int y = 0; y < 10; y++) {
            for (int x = 0; x < 9; x++) {
                if (!first) {
                    builder.append(",");
                }
                first = false;
                Piece piece = grid[y][x];
                builder.append("{\"x\":\"").append((char) ('a' + x)).append("\",\"y\":").append(y);
                if (piece == null) {
                    builder.append(",\"piece\":\"empty\",\"visible\":true}");
                } else {
                    PieceType visibleType = piece.isVisible() ? piece.getRealType() : piece.getOriginalType();
                    builder.append(",\"color\":\"").append(piece.getColor().protocolName()).append("\"");
                    builder.append(",\"piece\":\"").append(visibleType.getProtocolName()).append("\"");
                    builder.append(",\"visible\":").append(piece.isVisible()).append("}");
                }
            }
        }
        builder.append("]");
        return builder.toString();
    }

    public void printToConsole() {
        System.out.println("    a  b  c  d  e  f  g  h  i");
        for (int y = 0; y < 10; y++) {
            StringBuilder line = new StringBuilder();
            line.append(y).append("  ");
            for (int x = 0; x < 9; x++) {
                Piece piece = grid[y][x];
                line.append(piece == null ? ".." : piece.display()).append(" ");
            }
            System.out.println(line);
        }
    }

    public List<String> getRecords() {
        return records;
    }

    public String arrayFor(Color viewer) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int y = 0; y < 10; y++) {
            if (y > 0) sb.append(",");
            sb.append("[");
            for (int x = 0; x < 9; x++) {
                if (x > 0) sb.append(",");
                Piece piece = grid[y][x];
                if (piece == null) {
                    sb.append("null");
                } else {
                    String colorChar = piece.getColor() == Color.RED ? "r" : "b";
                    PieceType showType = piece.isVisible() ? piece.getRealType() : piece.getOriginalType();
                    String typeChar = showType.getProtocolName();
                    sb.append("\"").append(colorChar).append(typeChar).append("\"");
                }
            }
            sb.append("]");
        }
        sb.append("]");
        return sb.toString();
    }

    public Board copy() {
        Board copy = new Board(true);
        for (int y = 0; y < 10; y++) {
            for (int x = 0; x < 9; x++) {
                Piece piece = grid[y][x];
                if (piece != null) {
                    copy.grid[y][x] = new Piece(
                            piece.getColor(),
                            piece.getOriginalType(),
                            piece.getRealType(),
                            piece.isVisible());
                }
            }
        }
        copy.noCaptureSteps = this.noCaptureSteps;
        return copy;
    }
}
