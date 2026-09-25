public class CheckmateTest {
    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) throws Exception {
        System.out.println("========== 将军/将死/困毙 测试 ==========\n");

        test1_initialPositionNoCheck();
        test2_simpleRookCheck();
        test3_simpleCannonCheck();
        test4_checkmateDetection();
        test5_stalemateDetection();
        test6_knightCheck();
        test7_pawnCheck();

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

    static void test1_initialPositionNoCheck() {
        System.out.println("----- 测试1: 初始局面无将军 -----");
        Board board = new Board(42L);
        MoveValidator validator = new MoveValidator();
        test("初始局面红方不被将军", !validator.isInCheck(board, Color.RED));
        test("初始局面黑方不被将军", !validator.isInCheck(board, Color.BLACK));
        test("初始局面红方有合法走法", validator.hasAnyLegalMove(board, Color.RED));
        test("初始局面黑方有合法走法", validator.hasAnyLegalMove(board, Color.BLACK));
        System.out.println();
    }

    static void test2_simpleRookCheck() {
        System.out.println("----- 测试2: 车将军 -----");
        Board board = createEmptyBoard();
        board.set(new Position(4, 0), new Piece(Color.BLACK, PieceType.KING, PieceType.KING, true));
        board.set(new Position(4, 9), new Piece(Color.RED, PieceType.KING, PieceType.KING, true));
        board.set(new Position(4, 3), new Piece(Color.RED, PieceType.ROOK, PieceType.ROOK, true));
        MoveValidator validator = new MoveValidator();
        test("红车在e列黑将被将军", validator.isInCheck(board, Color.BLACK));
        test("红方不被将军", !validator.isInCheck(board, Color.RED));
        System.out.println();
    }

    static void test3_simpleCannonCheck() {
        System.out.println("----- 测试3: 炮将军 -----");
        Board board = createEmptyBoard();
        board.set(new Position(4, 0), new Piece(Color.BLACK, PieceType.KING, PieceType.KING, true));
        board.set(new Position(4, 9), new Piece(Color.RED, PieceType.KING, PieceType.KING, true));
        board.set(new Position(4, 2), new Piece(Color.RED, PieceType.PAWN, PieceType.PAWN, true));
        board.set(new Position(4, 7), new Piece(Color.RED, PieceType.CANNON, PieceType.CANNON, true));
        MoveValidator validator = new MoveValidator();
        test("红炮隔兵将军黑将", validator.isInCheck(board, Color.BLACK));
        System.out.println();
    }

    static void test4_checkmateDetection() {
        System.out.println("----- 测试4: 将死/困毙逻辑 -----");
        Board board = createEmptyBoard();
        board.set(new Position(4, 0), new Piece(Color.BLACK, PieceType.KING, PieceType.KING, true));
        board.set(new Position(0, 9), new Piece(Color.RED, PieceType.KING, PieceType.KING, true));
        board.set(new Position(4, 2), new Piece(Color.RED, PieceType.ROOK, PieceType.ROOK, true));
        MoveValidator validator = new MoveValidator();
        test("黑将被车将军", validator.isInCheck(board, Color.BLACK));
        test("黑方有合法走法（可以躲开）", validator.hasAnyLegalMove(board, Color.BLACK));
        test("黑方没有被将死（还能躲）", !validator.isCheckmate(board, Color.BLACK));
        test("黑方不是困毙（被将军了）", !validator.isStalemate(board, Color.BLACK));

        Board board2 = createEmptyBoard();
        board2.set(new Position(4, 0), new Piece(Color.BLACK, PieceType.KING, PieceType.KING, true));
        board2.set(new Position(0, 9), new Piece(Color.RED, PieceType.KING, PieceType.KING, true));
        test("黑方没被将军时也不是困毙（还能走）", !validator.isStalemate(board2, Color.BLACK));
        test("黑方没被将军时也不是将死", !validator.isCheckmate(board2, Color.BLACK));
        System.out.println();
    }

    static void test5_stalemateDetection() {
        System.out.println("----- 测试5: 初始局面验证 -----");
        Board board = new Board(42L);
        MoveValidator validator = new MoveValidator();
        test("初始局面红方不被将军", !validator.isInCheck(board, Color.RED));
        test("初始局面黑方不被将军", !validator.isInCheck(board, Color.BLACK));
        test("初始局面红方有合法走法", validator.hasAnyLegalMove(board, Color.RED));
        test("初始局面黑方有合法走法", validator.hasAnyLegalMove(board, Color.BLACK));
        test("初始局面红方不是将死", !validator.isCheckmate(board, Color.RED));
        test("初始局面黑方不是将死", !validator.isCheckmate(board, Color.BLACK));
        test("初始局面红方不是困毙", !validator.isStalemate(board, Color.RED));
        test("初始局面黑方不是困毙", !validator.isStalemate(board, Color.BLACK));
        System.out.println();
    }

    static void test6_knightCheck() {
        System.out.println("----- 测试6: 马将军 -----");
        Board board = createEmptyBoard();
        board.set(new Position(4, 0), new Piece(Color.BLACK, PieceType.KING, PieceType.KING, true));
        board.set(new Position(4, 9), new Piece(Color.RED, PieceType.KING, PieceType.KING, true));
        board.set(new Position(3, 2), new Piece(Color.RED, PieceType.KNIGHT, PieceType.KNIGHT, true));
        MoveValidator validator = new MoveValidator();
        test("马在c2将军黑将", validator.isInCheck(board, Color.BLACK));
        System.out.println();
    }

    static void test7_pawnCheck() {
        System.out.println("----- 测试7: 兵/卒将军 -----");
        Board board = createEmptyBoard();
        board.set(new Position(4, 0), new Piece(Color.BLACK, PieceType.KING, PieceType.KING, true));
        board.set(new Position(4, 9), new Piece(Color.RED, PieceType.KING, PieceType.KING, true));
        board.set(new Position(4, 1), new Piece(Color.RED, PieceType.PAWN, PieceType.PAWN, true));
        MoveValidator validator = new MoveValidator();
        test("红兵在e1将军黑将", validator.isInCheck(board, Color.BLACK));

        Board board2 = createEmptyBoard();
        board2.set(new Position(4, 9), new Piece(Color.RED, PieceType.KING, PieceType.KING, true));
        board2.set(new Position(4, 8), new Piece(Color.BLACK, PieceType.PAWN, PieceType.PAWN, true));
        test("黑卒在e8将军红帅", validator.isInCheck(board2, Color.RED));
        System.out.println();
    }

    static Board createEmptyBoard() {
        return new Board(0L) {
            {
                for (int y = 0; y < 10; y++) {
                    for (int x = 0; x < 9; x++) {
                        set(new Position(x, y), null);
                    }
                }
            }
        };
    }
}
