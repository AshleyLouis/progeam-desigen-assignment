import java.util.ArrayList;
import java.util.List;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Game {
    public static final long TURN_LIMIT_MILLIS = 65_000L;
    public static final int LONG_CHECK_LIMIT = 6;
    public static final int LONG_CAPTURE_LIMIT = 6;

    private final Board board;
    private final MoveValidator validator = new MoveValidator();
    private Color currentTurn = Color.RED;
    private long turnStartTime = System.currentTimeMillis();
    private boolean over;
    private String overReason;
    private Color winner;
    private int redConsecutiveChecks = 0;
    private int blackConsecutiveChecks = 0;
    private int redConsecutiveCaptures = 0;
    private int blackConsecutiveCaptures = 0;

    public Game() {
        this.board = new Board();
    }

    public Game(Long seed) {
        this.board = seed != null ? new Board(seed) : new Board();
    }

    public synchronized MoveResult applyMove(Color playerColor, Move move) {
        if (over) {
            return MoveResult.invalid("game already over");
        }
        if (playerColor != currentTurn) {
            return MoveResult.invalid("not your turn");
        }
        if (System.currentTimeMillis() - turnStartTime > TURN_LIMIT_MILLIS) {
            over = true;
            winner = currentTurn.opposite();
            overReason = "timeout";
            return MoveResult.gameOver(false, "timeout", winner, overReason);
        }

        String error = validator.validate(board, currentTurn, move);
        if (error != null) {
            return MoveResult.invalid(error);
        }

        Piece piece = board.get(move.getSource());
        Piece captured = null;
        PieceType flipResult = null;
        if (move.isFlip() && move.getSource().equals(move.getDestination())) {
            flipResult = piece.getRealType();
            piece.reveal(flipResult);
        } else {
            captured = board.move(move);
            if (!piece.isVisible()) {
                flipResult = piece.getRealType();
                piece.reveal(flipResult);
            }
        }
        board.record(currentTurn.protocolName() + " " + move.toRecord(flipResult));

        boolean isCheck = validator.isInCheck(board, currentTurn.opposite());
        boolean isCapture = captured != null;

        if (currentTurn == Color.RED) {
            if (isCheck) {
                redConsecutiveChecks++;
            } else {
                redConsecutiveChecks = 0;
            }
            blackConsecutiveChecks = 0;
            if (isCapture) {
                redConsecutiveCaptures++;
            } else {
                redConsecutiveCaptures = 0;
            }
            blackConsecutiveCaptures = 0;
        } else {
            if (isCheck) {
                blackConsecutiveChecks++;
            } else {
                blackConsecutiveChecks = 0;
            }
            redConsecutiveChecks = 0;
            if (isCapture) {
                blackConsecutiveCaptures++;
            } else {
                blackConsecutiveCaptures = 0;
            }
            redConsecutiveCaptures = 0;
        }

        int currentConsecutiveChecks = currentTurn == Color.RED ? redConsecutiveChecks : blackConsecutiveChecks;
        int currentConsecutiveCaptures = currentTurn == Color.RED ? redConsecutiveCaptures : blackConsecutiveCaptures;

        boolean isPawnMove = piece.getMoveType() == PieceType.PAWN;

        if (captured != null && captured.isVisible() && captured.getRealType() == PieceType.KING) {
            over = true;
            winner = currentTurn;
            overReason = "kingCaptured";
        } else if (!board.hasKing(currentTurn.opposite())) {
            over = true;
            winner = currentTurn;
            overReason = "kingCaptured";
        } else if (board.getNoCaptureSteps() >= 80) {
            over = true;
            winner = null;
            overReason = "drawByNoCapture";
        } else if (validator.isCheckmate(board, currentTurn.opposite())) {
            over = true;
            winner = currentTurn;
            overReason = "checkmate";
        } else if (validator.isStalemate(board, currentTurn.opposite())) {
            over = true;
            winner = currentTurn;
            overReason = "stalemate";
        } else if (currentConsecutiveChecks >= LONG_CHECK_LIMIT) {
            over = true;
            winner = currentTurn.opposite();
            overReason = "longCheck";
        } else if (currentConsecutiveCaptures >= LONG_CAPTURE_LIMIT && !isPawnMove) {
            over = true;
            winner = currentTurn.opposite();
            overReason = "longCapture";
        } else if (currentConsecutiveCaptures >= LONG_CAPTURE_LIMIT && isPawnMove) {
            over = true;
            winner = null;
            overReason = "drawByPawnLongCapture";
        } else {
            currentTurn = currentTurn.opposite();
            turnStartTime = System.currentTimeMillis();
        }
        return MoveResult.success(move, flipResult, captured, over, winner, overReason);
    }

    public synchronized Color getCurrentTurn() {
        return currentTurn;
    }

    public synchronized long getTurnStartTime() {
        return turnStartTime;
    }

    public synchronized boolean isOver() {
        return over;
    }

    public synchronized void setOver(Color winner, String reason) {
        this.over = true;
        this.winner = winner;
        this.overReason = reason;
    }

    public synchronized void reset(Long seed) {
        board.reset(seed);
        currentTurn = Color.RED;
        turnStartTime = System.currentTimeMillis();
        over = false;
        overReason = null;
        winner = null;
        redConsecutiveChecks = 0;
        blackConsecutiveChecks = 0;
        redConsecutiveCaptures = 0;
        blackConsecutiveCaptures = 0;
    }

    public synchronized Color forceTimeoutIfNeeded() {
        if (!over && System.currentTimeMillis() - turnStartTime > TURN_LIMIT_MILLIS) {
            over = true;
            winner = currentTurn.opposite();
            overReason = "timeout";
            return winner;
        }
        return over && "timeout".equals(overReason) ? winner : null;
    }

    public synchronized String boardJsonFor(Color viewer) {
        return board.boardJsonFor(viewer);
    }

    public synchronized void printBoard() {
        board.printToConsole();
    }

    public synchronized List<String> getRecords() {
        return new ArrayList<String>(board.getRecords());
    }

    public synchronized int getMoveCount() {
        return board.getRecords().size();
    }

    public synchronized String getWinner() {
        return winner == null ? "draw" : winner.protocolName();
    }

    public synchronized String getOverReason() {
        return overReason;
    }

    public synchronized String boardArrayFor(Color viewer) {
        return board.arrayFor(viewer);
    }

    public synchronized String saveRecordToFile(String folder) {
        try {
            java.io.File dir = new java.io.File(folder);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String fileName = folder + "/jieqi_" + timestamp + ".txt";
            FileWriter writer = new FileWriter(fileName, true);
            writer.write("========== 揭棋棋谱 ==========\n");
            writer.write("时间: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "\n");
            writer.write("结果: ");
            if (winner == null) {
                writer.write("和棋");
            } else {
                writer.write(winner == Color.RED ? "红方" : "黑方");
                writer.write("胜 (");
                writer.write(overReason == null ? "" : overReason);
                writer.write(")");
            }
            writer.write("\n步数: " + getMoveCount() + "\n\n");
            writer.write("----- 棋谱 -----\n");
            List<String> recs = board.getRecords();
            for (int i = 0; i < recs.size(); i++) {
                writer.write(String.format("%3d. %s\n", i + 1, recs.get(i)));
            }
            writer.write("\n==============================\n");
            writer.close();
            return fileName;
        } catch (IOException e) {
            return null;
        }
    }
}
