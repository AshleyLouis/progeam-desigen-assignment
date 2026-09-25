import java.util.HashMap;
import java.util.Map;

/**
 * AI局面评估器：评估棋盘局面，包括暗子的数学期望值。
 */
public class AIEvaluator {
    // 棋子基础价值
    private static final Map<PieceType, Integer> PIECE_VALUES = new HashMap<PieceType, Integer>();
    static {
        PIECE_VALUES.put(PieceType.KING, 10000);
        PIECE_VALUES.put(PieceType.ROOK, 900);
        PIECE_VALUES.put(PieceType.CANNON, 450);
        PIECE_VALUES.put(PieceType.KNIGHT, 400);
        PIECE_VALUES.put(PieceType.BISHOP, 200);
        PIECE_VALUES.put(PieceType.GUARD, 200);
        PIECE_VALUES.put(PieceType.PAWN, 100);
    }

    // 每方各类型暗子的初始数量（不含将，将始终可见）
    private static final int[] INITIAL_COUNTS = new int[7];
    static {
        INITIAL_COUNTS[PieceType.ROOK.ordinal()] = 2;
        INITIAL_COUNTS[PieceType.KNIGHT.ordinal()] = 2;
        INITIAL_COUNTS[PieceType.CANNON.ordinal()] = 2;
        INITIAL_COUNTS[PieceType.BISHOP.ordinal()] = 2;
        INITIAL_COUNTS[PieceType.GUARD.ordinal()] = 2;
        INITIAL_COUNTS[PieceType.PAWN.ordinal()] = 5;
    }
    private static final int TOTAL_HIDDEN = 15; // 每方15个暗子

    private final Color perspective;
    private final int[] redRevealed = new int[7]; // 红方已翻开的各类型数量
    private final int[] blackRevealed = new int[7]; // 黑方已翻开的各类型数量

    public AIEvaluator(Color perspective) {
        this.perspective = perspective;
    }

    /**
     * 根据棋盘状态更新已翻开棋子的统计信息。
     */
    public void updateRevealedCount(Board board) {
        int[] red = new int[7];
        int[] black = new int[7];
        for (int y = 0; y < 10; y++) {
            for (int x = 0; x < 9; x++) {
                Piece p = board.get(new Position(x, y));
                if (p != null && p.isVisible() && p.getRealType() != PieceType.KING) {
                    if (p.getColor() == Color.RED) {
                        red[p.getRealType().ordinal()]++;
                    } else {
                        black[p.getRealType().ordinal()]++;
                    }
                }
            }
        }
        System.arraycopy(red, 0, redRevealed, 0, 7);
        System.arraycopy(black, 0, blackRevealed, 0, 7);
    }

    /**
     * 计算某个颜色暗子的期望价值。
     * 基于剩余未翻开的各类型数量计算概率分布。
     */
    public double expectedHiddenValue(Color color) {
        int[] revealed = (color == Color.RED) ? redRevealed : blackRevealed;
        int remaining = 0;
        double totalValue = 0;
        for (PieceType t : PieceType.values()) {
            if (t == PieceType.KING) continue;
            int left = INITIAL_COUNTS[t.ordinal()] - revealed[t.ordinal()];
            if (left > 0) {
                remaining += left;
                totalValue += left * PIECE_VALUES.get(t);
            }
        }
        if (remaining == 0) return 0;
        return totalValue / remaining;
    }

    /**
     * 计算单个暗子的期望价值（基于该颜色的概率分布）。
     */
    public double singleHiddenValue(Color color) {
        return expectedHiddenValue(color);
    }

    /**
     * 评估棋盘局面，返回当前视角的得分。
     * 正数对自己有利，负数对对手有利。
     */
    public double evaluate(Board board, Color currentTurn) {
        updateRevealedCount(board);

        double myScore = 0;
        double oppScore = 0;
        Color opp = perspective.opposite();
        boolean myKingAlive = false;
        boolean oppKingAlive = false;

        for (int y = 0; y < 10; y++) {
            for (int x = 0; x < 9; x++) {
                Piece p = board.get(new Position(x, y));
                if (p == null) continue;

                double value;
                if (p.isVisible()) {
                    value = PIECE_VALUES.get(p.getRealType());
                    if (p.getRealType() == PieceType.KING) {
                        if (p.getColor() == perspective) myKingAlive = true;
                        else oppKingAlive = true;
                    }
                } else {
                    value = singleHiddenValue(p.getColor());
                }

                if (p.getColor() == perspective) {
                    myScore += value;
                } else {
                    oppScore += value;
                }
            }
        }

        // 将帅被吃 = 极端分数
        if (!myKingAlive) return -100000;
        if (!oppKingAlive) return 100000;

        // 位置加成：中心控制
        double positionalScore = evaluatePositional(board);

        double score = myScore - oppScore + positionalScore * 0.1;

        return score;
    }

    /**
     * 简单的位置评估：靠近对方将帅有加成，控制中心有加成。
     */
    private double evaluatePositional(Board board) {
        double score = 0;
        Position oppKing = board.findKing(perspective.opposite());
        Position myKing = board.findKing(perspective);

        for (int y = 0; y < 10; y++) {
            for (int x = 0; x < 9; x++) {
                Piece p = board.get(new Position(x, y));
                if (p == null) continue;
                // 中心控制：x在3-5之间有加成
                double centerBonus = 0;
                if (x >= 3 && x <= 5) centerBonus = 5;

                if (p.getColor() == perspective) {
                    score += centerBonus;
                    // 靠近对方将帅有进攻加成
                    if (oppKing != null && p.isVisible()) {
                        int dist = Math.abs(x - oppKing.getX()) + Math.abs(y - oppKing.getY());
                        score += Math.max(0, 10 - dist);
                    }
                } else {
                    score -= centerBonus;
                    if (myKing != null && p.isVisible()) {
                        int dist = Math.abs(x - myKing.getX()) + Math.abs(y - myKing.getY());
                        score -= Math.max(0, 10 - dist);
                    }
                }
            }
        }
        return score;
    }

    public int getPieceValue(PieceType type) {
        return PIECE_VALUES.get(type);
    }
}
