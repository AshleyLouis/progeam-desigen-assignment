import java.util.List;

public class MoveValidator {
    public String validate(Board board, Color currentColor, Move move) {
        Piece piece = board.get(move.getSource());
        if (piece == null) {
            return "source has no piece";
        }
        if (piece.getColor() != currentColor) {
            return "cannot move opponent piece";
        }
        if (move.isFlip() && move.getSource().equals(move.getDestination())) {
            if (piece.isVisible()) {
                return "piece is already visible";
            }
            return null;
        }
        if (move.getSource().equals(move.getDestination())) {
            return "source and destination are the same";
        }
        Piece target = board.get(move.getDestination());
        if (target != null && target.getColor() == currentColor) {
            return "cannot capture own piece";
        }

        PieceType type = piece.getMoveType();
        boolean valid;
        switch (type) {
            case KING:
                valid = validKing(move);
                break;
            case ROOK:
                valid = validRook(board, move);
                break;
            case KNIGHT:
                valid = validKnight(board, move);
                break;
            case CANNON:
                valid = validCannon(board, move, target != null);
                break;
            case PAWN:
                valid = validPawn(piece.getColor(), move);
                break;
            case GUARD:
                valid = move.getSource().dx(move.getDestination()) == 1
                        && move.getSource().dy(move.getDestination()) == 1;
                break;
            case BISHOP:
                valid = validBishop(board, move);
                break;
            default:
                valid = false;
        }
        return valid ? null : "illegal " + type.getProtocolName() + " move";
    }

    private boolean validKing(Move move) {
        Position from = move.getSource();
        Position to = move.getDestination();
        boolean inPalace = to.getX() >= 3 && to.getX() <= 5
                && ((to.getY() >= 0 && to.getY() <= 2) || (to.getY() >= 7 && to.getY() <= 9));
        return inPalace && from.dx(to) + from.dy(to) == 1;
    }

    private boolean validRook(Board board, Move move) {
        return sameLine(move) && countBetween(board, move.getSource(), move.getDestination()) == 0;
    }

    private boolean validKnight(Board board, Move move) {
        Position from = move.getSource();
        Position to = move.getDestination();
        int dx = to.getX() - from.getX();
        int dy = to.getY() - from.getY();
        if (!((Math.abs(dx) == 1 && Math.abs(dy) == 2) || (Math.abs(dx) == 2 && Math.abs(dy) == 1))) {
            return false;
        }
        Position leg = Math.abs(dx) == 2
                ? new Position(from.getX() + dx / 2, from.getY())
                : new Position(from.getX(), from.getY() + dy / 2);
        return board.get(leg) == null;
    }

    private boolean validCannon(Board board, Move move, boolean capturing) {
        if (!sameLine(move)) {
            return false;
        }
        int between = countBetween(board, move.getSource(), move.getDestination());
        return capturing ? between == 1 : between == 0;
    }

    private boolean validPawn(Color color, Move move) {
        Position from = move.getSource();
        Position to = move.getDestination();
        int dx = Math.abs(to.getX() - from.getX());
        int dy = to.getY() - from.getY();
        if (color == Color.RED) {
            if (from.getY() >= 5) {
                return dx == 0 && dy == -1;
            }
            return (dx == 0 && dy == -1) || (dx == 1 && dy == 0);
        } else {
            if (from.getY() <= 4) {
                return dx == 0 && dy == 1;
            }
            return (dx == 0 && dy == 1) || (dx == 1 && dy == 0);
        }
    }

    private boolean validBishop(Board board, Move move) {
        Position from = move.getSource();
        Position to = move.getDestination();
        if (from.dx(to) != 2 || from.dy(to) != 2) {
            return false;
        }
        Position eye = new Position((from.getX() + to.getX()) / 2, (from.getY() + to.getY()) / 2);
        return board.get(eye) == null;
    }

    private boolean sameLine(Move move) {
        return move.getSource().getX() == move.getDestination().getX()
                || move.getSource().getY() == move.getDestination().getY();
    }

    private int countBetween(Board board, Position from, Position to) {
        int count = 0;
        int dx = Integer.compare(to.getX(), from.getX());
        int dy = Integer.compare(to.getY(), from.getY());
        int x = from.getX() + dx;
        int y = from.getY() + dy;
        while (x != to.getX() || y != to.getY()) {
            if (board.get(new Position(x, y)) != null) {
                count++;
            }
            x += dx;
            y += dy;
        }
        return count;
    }

    public boolean isInCheck(Board board, Color color) {
        Position kingPos = board.findKing(color);
        if (kingPos == null) {
            return false;
        }
        Color opponent = color.opposite();
        List<Position> opponentPieces = board.getPiecesOfColor(opponent);
        for (Position pos : opponentPieces) {
            Piece piece = board.get(pos);
            if (!piece.isVisible()) {
                continue;
            }
            Move testMove = new Move(pos, kingPos, false);
            Piece target = board.get(kingPos);
            PieceType type = piece.getRealType();
            boolean canAttack = false;
            switch (type) {
                case KING:
                    canAttack = validKing(testMove);
                    break;
                case ROOK:
                    canAttack = validRook(board, testMove);
                    break;
                case KNIGHT:
                    canAttack = validKnight(board, testMove);
                    break;
                case CANNON:
                    canAttack = validCannon(board, testMove, target != null);
                    break;
                case PAWN:
                    canAttack = validPawn(opponent, testMove);
                    break;
                case GUARD:
                    canAttack = pos.dx(kingPos) == 1 && pos.dy(kingPos) == 1;
                    break;
                case BISHOP:
                    canAttack = validBishop(board, testMove);
                    break;
            }
            if (canAttack) {
                return true;
            }
        }
        return false;
    }

    public boolean hasAnyLegalMove(Board board, Color color) {
        List<Position> myPieces = board.getPiecesOfColor(color);
        for (Position from : myPieces) {
            Piece piece = board.get(from);
            if (piece == null) continue;

            if (!piece.isVisible()) {
                if (validate(board, color, new Move(from, from, true)) == null) {
                    return true;
                }
            }

            for (int y = 0; y < 10; y++) {
                for (int x = 0; x < 9; x++) {
                    Position to = new Position(x, y);
                    if (from.equals(to)) continue;
                    Move move = new Move(from, to, false);
                    if (validate(board, color, move) == null) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean isCheckmate(Board board, Color color) {
        return isInCheck(board, color) && !hasAnyLegalMove(board, color);
    }

    public boolean isStalemate(Board board, Color color) {
        return !isInCheck(board, color) && !hasAnyLegalMove(board, color);
    }
}
