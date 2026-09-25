import java.lang.reflect.Field;

public class LongRuleTest {
    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) throws Exception {
        System.out.println("========== 长将/长捉规则 测试 ==========\n");

        test1_constants();
        test2_longCheckDetection();
        test3_longCaptureDetection();
        test4_pawnLongCaptureIsDraw();

        System.out.println("\n========== 测试结果 ==========");
        System.out.println("✅ 通过: " + passed);
        System.out.println("❌ 失败: " + failed);
        System.out.println("总计: " + (passed + failed));
        if (failed == 0) {
            System.out.println("\n🎉 所有测试通过！");
        } else {
            System.out.println("\n⚠️  有测试失败，请检查！");
            System.exit(1);
        }
    }

    static void test(String name, boolean condition) {
        if (condition) {
            System.out.println("  ✅ " + name);
            passed++;
        } else {
            System.out.println("  ❌ " + name + " —— 失败！");
            failed++;
        }
    }

    static Game createGameWithBoard() throws Exception {
        Game game = new Game();
        Board board = new Board(true);
        Field field = Game.class.getDeclaredField("board");
        field.setAccessible(true);
        field.set(game, board);

        board.set(p(4, 0), new Piece(Color.BLACK, PieceType.KING, PieceType.KING, true));
        board.set(p(4, 9), new Piece(Color.RED, PieceType.KING, PieceType.KING, true));
        board.set(p(0, 0), new Piece(Color.RED, PieceType.ROOK, PieceType.ROOK, true));
        board.set(p(0, 9), new Piece(Color.BLACK, PieceType.ROOK, PieceType.ROOK, true));
        return game;
    }

    static Board getBoard(Game game) throws Exception {
        Field field = Game.class.getDeclaredField("board");
        field.setAccessible(true);
        return (Board) field.get(game);
    }

    static void setField(Object obj, String name, Object value) throws Exception {
        Field f = obj.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(obj, value);
    }

    static void test1_constants() {
        System.out.println("----- 测试1: 常量验证 -----");
        test("长将限制为6回合", Game.LONG_CHECK_LIMIT == 6);
        test("长捉限制为6回合", Game.LONG_CAPTURE_LIMIT == 6);
        System.out.println();
    }

    static void test2_longCheckDetection() throws Exception {
        System.out.println("----- 测试2: 长将判负逻辑 -----");

        Game game = createGameWithBoard();
        Board b = getBoard(game);

        test("游戏初始未结束", !game.isOver());

        Move move = new Move(p(0, 0), p(1, 0), false);
        game.applyMove(Color.RED, move);
        test("走1步将军，游戏继续（1<6）", !game.isOver());

        game = createGameWithBoard();
        b = getBoard(game);
        setField(game, "redConsecutiveChecks", 5);
        Move move2 = new Move(p(0, 0), p(1, 0), false);
        game.applyMove(Color.RED, move2);
        test("之前已将军5次，再将军1次达到6次，触发长将判负",
            game.isOver() && "longCheck".equals(game.getOverReason()));
        if (game.isOver() && "longCheck".equals(game.getOverReason())) {
            test("长将判负，将军方(红)输，黑方胜", "black".equals(game.getWinner()));
        } else {
            test("长将判负，将军方(红)输，黑方胜", false);
        }
        System.out.println();
    }

    static void test3_longCaptureDetection() throws Exception {
        System.out.println("----- 测试3: 长捉判负逻辑 -----");

        Game game = createGameWithBoard();
        Board b = getBoard(game);

        b.set(p(1, 0), new Piece(Color.BLACK, PieceType.PAWN, PieceType.PAWN, true));
        b.set(p(2, 0), new Piece(Color.BLACK, PieceType.PAWN, PieceType.PAWN, true));
        b.set(p(3, 0), new Piece(Color.BLACK, PieceType.PAWN, PieceType.PAWN, true));
        b.set(p(5, 0), new Piece(Color.BLACK, PieceType.PAWN, PieceType.PAWN, true));
        b.set(p(6, 0), new Piece(Color.BLACK, PieceType.PAWN, PieceType.PAWN, true));
        b.set(p(7, 0), new Piece(Color.BLACK, PieceType.PAWN, PieceType.PAWN, true));
        b.set(p(8, 0), new Piece(Color.BLACK, PieceType.PAWN, PieceType.PAWN, true));

        setField(game, "redConsecutiveCaptures", 5);
        Move move = new Move(p(0, 0), p(1, 0), false);
        game.applyMove(Color.RED, move);
        test("车连续吃子6次，触发长捉判负",
            game.isOver() && "longCapture".equals(game.getOverReason()));
        if (game.isOver() && "longCapture".equals(game.getOverReason())) {
            test("长捉判负，捉子方(红)输，黑方胜", "black".equals(game.getWinner()));
        } else {
            test("长捉判负，捉子方(红)输，黑方胜", false);
            System.out.println("  实际结束原因: " + game.getOverReason());
        }
        System.out.println();
    }

    static void test4_pawnLongCaptureIsDraw() throws Exception {
        System.out.println("----- 测试4: 兵卒长捉判和 -----");

        Game game = createGameWithBoard();
        Board b = getBoard(game);

        b.set(p(4, 7), new Piece(Color.RED, PieceType.PAWN, PieceType.PAWN, true));
        b.set(p(4, 6), new Piece(Color.BLACK, PieceType.PAWN, PieceType.PAWN, true));
        b.set(p(4, 5), new Piece(Color.BLACK, PieceType.PAWN, PieceType.PAWN, true));
        b.set(p(4, 4), new Piece(Color.BLACK, PieceType.PAWN, PieceType.PAWN, true));
        b.set(p(4, 3), new Piece(Color.BLACK, PieceType.PAWN, PieceType.PAWN, true));
        b.set(p(4, 2), new Piece(Color.BLACK, PieceType.PAWN, PieceType.PAWN, true));
        b.set(p(4, 1), new Piece(Color.BLACK, PieceType.PAWN, PieceType.PAWN, true));

        setField(game, "redConsecutiveCaptures", 5);
        Move move = new Move(p(4, 7), p(4, 6), false);
        game.applyMove(Color.RED, move);

        test("兵卒连续吃子6次，判和（drawByPawnLongCapture）",
            game.isOver() && "drawByPawnLongCapture".equals(game.getOverReason()));
        if (game.isOver() && "drawByPawnLongCapture".equals(game.getOverReason())) {
            test("兵卒长捉是和棋，胜者为draw", "draw".equals(game.getWinner()));
        } else {
            test("兵卒长捉是和棋，胜者为draw", false);
            System.out.println("  实际结束原因: " + game.getOverReason());
            System.out.println("  游戏是否结束: " + game.isOver());
            System.out.println("  胜者: " + game.getWinner());
        }
        System.out.println();
    }

    static Position p(int col, int row) {
        return new Position(col, row);
    }
}
