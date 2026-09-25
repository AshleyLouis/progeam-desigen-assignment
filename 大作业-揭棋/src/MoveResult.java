public class MoveResult {
    private final boolean success;
    private final String message;
    private final Move move;
    private final PieceType flipResult;
    private final Piece captured;
    private final boolean gameOver;
    private final Color winner;
    private final String reason;

    private MoveResult(boolean success, String message, Move move, PieceType flipResult,
                       Piece captured, boolean gameOver, Color winner, String reason) {
        this.success = success;
        this.message = message;
        this.move = move;
        this.flipResult = flipResult;
        this.captured = captured;
        this.gameOver = gameOver;
        this.winner = winner;
        this.reason = reason;
    }

    public static MoveResult invalid(String message) {
        return new MoveResult(false, message, null, null, null, false, null, null);
    }

    public static MoveResult gameOver(boolean success, String message, Color winner, String reason) {
        return new MoveResult(success, message, null, null, null, true, winner, reason);
    }

    public static MoveResult success(Move move, PieceType flipResult, Piece captured,
                                     boolean gameOver, Color winner, String reason) {
        return new MoveResult(true, "ok", move, flipResult, captured, gameOver, winner, reason);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public Move getMove() {
        return move;
    }

    public PieceType getFlipResult() {
        return flipResult;
    }

    public Piece getCaptured() {
        return captured;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public Color getWinner() {
        return winner;
    }

    public String getReason() {
        return reason;
    }
}
