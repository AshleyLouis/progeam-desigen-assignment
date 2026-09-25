import java.util.ArrayList;
import java.util.List;

/**
 * Alpha-Beta剪枝搜索器：搜索最佳走法。
 * 对暗子的翻面使用期望值评估，对确定走法使用正常搜索。
 */
public class AISearcher {
    private final AIEvaluator evaluator;
    private final MoveValidator validator;
    private final Color perspective;
    private final int maxDepth;

    public AISearcher(Color perspective, int maxDepth) {
        this.perspective = perspective;
        this.maxDepth = maxDepth;
        this.evaluator = new AIEvaluator(perspective);
        this.validator = new MoveValidator();
    }

    /**
     * 搜索最佳走法。
     * @param board 当前棋盘
     * @param currentTurn 当前轮到谁
     * @return 最佳Move，或null如果无合法走法
     */
    public Move searchBestMove(Board board, Color currentTurn) {
        List<Move> moves = generateMoves(board, currentTurn);
        if (moves.isEmpty()) return null;

        Move bestMove = moves.get(0);
        double bestScore = currentTurn == perspective ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;

        for (Move move : moves) {
            Board copy = cloneBoard(board);
            applyMoveToBoard(copy, move, currentTurn);

            double score = alphaBeta(copy, currentTurn.opposite(), maxDepth - 1,
                    Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);

            if (currentTurn == perspective) {
                if (score > bestScore) {
                    bestScore = score;
                    bestMove = move;
                }
            } else {
                if (score < bestScore) {
                    bestScore = score;
                    bestMove = move;
                }
            }
        }

        return bestMove;
    }

    /**
     * Alpha-Beta递归搜索。
     */
    private double alphaBeta(Board board, Color currentTurn, int depth, double alpha, double beta) {
        // 终止条件：到达深度限制或游戏结束
        if (depth == 0) {
            return evaluator.evaluate(board, currentTurn);
        }

        // 检查是否游戏结束（将不在了）
        if (!board.hasKing(perspective)) return -100000;
        if (!board.hasKing(perspective.opposite())) return 100000;

        List<Move> moves = generateMoves(board, currentTurn);
        if (moves.isEmpty()) {
            // 无合法走法，用评估函数
            return evaluator.evaluate(board, currentTurn);
        }

        if (currentTurn == perspective) {
            // 最大化方
            double maxScore = Double.NEGATIVE_INFINITY;
            for (Move move : moves) {
                Board copy = cloneBoard(board);
                applyMoveToBoard(copy, move, currentTurn);
                double score = alphaBeta(copy, currentTurn.opposite(), depth - 1, alpha, beta);
                maxScore = Math.max(maxScore, score);
                alpha = Math.max(alpha, score);
                if (beta <= alpha) {
                    break; // Beta剪枝
                }
            }
            return maxScore;
        } else {
            // 最小化方
            double minScore = Double.POSITIVE_INFINITY;
            for (Move move : moves) {
                Board copy = cloneBoard(board);
                applyMoveToBoard(copy, move, currentTurn);
                double score = alphaBeta(copy, currentTurn.opposite(), depth - 1, alpha, beta);
                minScore = Math.min(minScore, score);
                beta = Math.min(beta, score);
                if (beta <= alpha) {
                    break; // Alpha剪枝
                }
            }
            return minScore;
        }
    }

    /**
     * 生成某个颜色的所有合法走法。
     */
    public List<Move> generateMoves(Board board, Color color) {
        List<Move> moves = new ArrayList<Move>();
        List<Position> pieces = board.getPiecesOfColor(color);

        for (Position pos : pieces) {
            Piece piece = board.get(pos);
            if (piece == null) continue;

            // 翻面走法（暗子可以翻面）
            if (!piece.isVisible()) {
                Move flipMove = new Move(pos, pos, true);
                String err = validator.validate(board, color, flipMove);
                if (err == null) {
                    moves.add(flipMove);
                }
            }

            // 移动走法：尝试所有目标位置
            PieceType moveType = piece.getMoveType();
            List<Position> targets = generateTargets(board, pos, piece, moveType);
            for (Position target : targets) {
                Move move = new Move(pos, target, false);
                String err = validator.validate(board, color, move);
                if (err == null) {
                    moves.add(move);
                }
            }
        }

        return moves;
    }

    /**
     * 根据棋子类型生成可能的目标位置。
     */
    private List<Position> generateTargets(Board board, Position from, Piece piece, PieceType type) {
        List<Position> targets = new ArrayList<Position>();
        int x = from.getX();
        int y = from.getY();

        switch (type) {
            case ROOK:
                // 车：直线移动
                addLineTargets(targets, board, from, 1, 0);
                addLineTargets(targets, board, from, -1, 0);
                addLineTargets(targets, board, from, 0, 1);
                addLineTargets(targets, board, from, 0, -1);
                break;
            case CANNON:
                // 炮：直线移动，可跳一个吃子
                addCannonTargets(targets, board, from, 1, 0);
                addCannonTargets(targets, board, from, -1, 0);
                addCannonTargets(targets, board, from, 0, 1);
                addCannonTargets(targets, board, from, 0, -1);
                break;
            case KNIGHT:
                // 马：日字走法
                addKnightTargets(targets, from);
                break;
            case BISHOP:
                // 相/象：田字走法
                addBishopTargets(targets, from);
                break;
            case GUARD:
                // 士/仕：斜走一格
                addGuardTargets(targets, from);
                break;
            case KING:
                // 将/帅：直走一格
                addKingTargets(targets, from);
                break;
            case PAWN:
                // 兵/卒：前进或过河后横走
                addPawnTargets(targets, from, piece.getColor());
                break;
        }
        return targets;
    }

    private void addLineTargets(List<Position> targets, Board board, Position from, int dx, int dy) {
        int x = from.getX() + dx;
        int y = from.getY() + dy;
        while (x >= 0 && x < 9 && y >= 0 && y < 10) {
            Position pos = new Position(x, y);
            Piece p = board.get(pos);
            if (p == null) {
                targets.add(pos);
            } else {
                if (p.getColor() != board.get(from).getColor()) {
                    targets.add(pos);
                }
                break;
            }
            x += dx;
            y += dy;
        }
    }

    private void addCannonTargets(List<Position> targets, Board board, Position from, int dx, int dy) {
        int x = from.getX() + dx;
        int y = from.getY() + dy;
        boolean jumped = false;
        while (x >= 0 && x < 9 && y >= 0 && y < 10) {
            Position pos = new Position(x, y);
            Piece p = board.get(pos);
            if (!jumped) {
                if (p == null) {
                    targets.add(pos);
                } else {
                    jumped = true;
                }
            } else {
                if (p != null) {
                    if (p.getColor() != board.get(from).getColor()) {
                        targets.add(pos);
                    }
                    break;
                }
            }
            x += dx;
            y += dy;
        }
    }

    private void addKnightTargets(List<Position> targets, Position from) {
        int[][] deltas = {{1, 2}, {-1, 2}, {1, -2}, {-1, -2}, {2, 1}, {-2, 1}, {2, -1}, {-2, -1}};
        for (int[] d : deltas) {
            int nx = from.getX() + d[0];
            int ny = from.getY() + d[1];
            if (nx >= 0 && nx < 9 && ny >= 0 && ny < 10) {
                targets.add(new Position(nx, ny));
            }
        }
    }

    private void addBishopTargets(List<Position> targets, Position from) {
        int[][] deltas = {{2, 2}, {-2, 2}, {2, -2}, {-2, -2}};
        for (int[] d : deltas) {
            int nx = from.getX() + d[0];
            int ny = from.getY() + d[1];
            if (nx >= 0 && nx < 9 && ny >= 0 && ny < 10) {
                targets.add(new Position(nx, ny));
            }
        }
    }

    private void addGuardTargets(List<Position> targets, Position from) {
        int[][] deltas = {{1, 1}, {-1, 1}, {1, -1}, {-1, -1}};
        for (int[] d : deltas) {
            int nx = from.getX() + d[0];
            int ny = from.getY() + d[1];
            if (nx >= 0 && nx < 9 && ny >= 0 && ny < 10) {
                targets.add(new Position(nx, ny));
            }
        }
    }

    private void addKingTargets(List<Position> targets, Position from) {
        int[][] deltas = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] d : deltas) {
            int nx = from.getX() + d[0];
            int ny = from.getY() + d[1];
            if (nx >= 0 && nx < 9 && ny >= 0 && ny < 10) {
                targets.add(new Position(nx, ny));
            }
        }
    }

    private void addPawnTargets(List<Position> targets, Position from, Color color) {
        int forward = (color == Color.RED) ? -1 : 1;
        // 前进
        int ny = from.getY() + forward;
        if (ny >= 0 && ny < 10) {
            targets.add(new Position(from.getX(), ny));
        }
        // 过河后可以横走
        boolean crossedRiver = (color == Color.RED) ? from.getY() <= 4 : from.getY() >= 5;
        if (crossedRiver) {
            if (from.getX() + 1 < 9) targets.add(new Position(from.getX() + 1, from.getY()));
            if (from.getX() - 1 >= 0) targets.add(new Position(from.getX() - 1, from.getY()));
        }
    }

    /**
     * 在模拟棋盘上执行走法。
     * 对于暗子移动/翻面，使用期望值处理（不实际翻面，保持暗子状态用于评估）。
     */
    private void applyMoveToBoard(Board board, Move move, Color color) {
        if (move.isFlip() && move.getSource().equals(move.getDestination())) {
            // 翻面：保持暗子状态（期望值评估会处理）
            // 在搜索中不实际翻面，因为结果不确定
            return;
        }

        Piece piece = board.get(move.getSource());
        if (piece == null) return;

        Piece captured = board.get(move.getDestination());
        board.set(move.getDestination(), piece);
        board.set(move.getSource(), null);

        // 移动后暗子会被翻开，但在搜索中我们不知道真实类型
        // 保持暗子状态，由评估函数用期望值处理
    }

    /**
     * 克隆棋盘。
     */
    private Board cloneBoard(Board original) {
        Board copy = new Board(true); // 空棋盘
        for (int y = 0; y < 10; y++) {
            for (int x = 0; x < 9; x++) {
                Piece p = original.get(new Position(x, y));
                if (p != null) {
                    copy.set(new Position(x, y), new Piece(p.getColor(), p.getOriginalType(),
                            p.getRealType(), p.isVisible()));
                }
            }
        }
        return copy;
    }
}
