import java.io.File;
import java.util.List;

public class RecordTest {
    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) throws Exception {
        System.out.println("========== 棋谱保存 测试 ==========\n");

        test1_recordExists();
        test2_saveRecordFile();
        test3_recordFormat();

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

    static void test1_recordExists() {
        System.out.println("----- 测试1: 棋谱记录存在 -----");
        Game game = new Game(42L);
        test("游戏开始时棋谱为空", game.getRecords().size() == 0);

        Move move1 = new Move(Position.fromText("a3"), Position.fromText("a4"), false);
        game.applyMove(Color.RED, move1);
        test("走一步后棋谱有1条记录", game.getRecords().size() == 1);
        test("记录包含红方", game.getRecords().get(0).contains("red"));
        test("记录包含a3->a4", game.getRecords().get(0).contains("a3->a4"));

        Move move2 = new Move(Position.fromText("a6"), Position.fromText("a5"), false);
        game.applyMove(Color.BLACK, move2);
        test("走两步后棋谱有2条记录", game.getRecords().size() == 2);
        test("第二条记录包含黑方", game.getRecords().get(1).contains("black"));
        System.out.println();
    }

    static void test2_saveRecordFile() {
        System.out.println("----- 测试2: 保存棋谱到文件 -----");
        Game game = new Game(42L);
        Move move1 = new Move(Position.fromText("a3"), Position.fromText("a4"), false);
        game.applyMove(Color.RED, move1);
        Move move2 = new Move(Position.fromText("a6"), Position.fromText("a5"), false);
        game.applyMove(Color.BLACK, move2);
        Move move3 = new Move(Position.fromText("b0"), Position.fromText("b0"), true);
        game.applyMove(Color.RED, move3);

        String testFolder = "test_records";
        String fileName = game.saveRecordToFile(testFolder);
        test("保存成功，返回文件名", fileName != null);
        test("文件名包含jieqi", fileName != null && fileName.contains("jieqi"));
        test("文件存在", fileName != null && new File(fileName).exists());
        test("文件大小>0", fileName != null && new File(fileName).length() > 0);

        if (fileName != null) {
            new File(fileName).delete();
            File dir = new File(testFolder);
            if (dir.listFiles() != null && dir.listFiles().length == 0) {
                dir.delete();
            }
        }
        System.out.println();
    }

    static void test3_recordFormat() {
        System.out.println("----- 测试3: 翻子也会被记录 -----");
        Game game = new Game(42L);
        List<String> before = game.getRecords();
        test("初始棋谱为空", before.isEmpty());

        Move flipMove = new Move(Position.fromText("b0"), Position.fromText("b0"), true);
        game.applyMove(Color.RED, flipMove);
        List<String> after = game.getRecords();
        test("翻子后有1条记录", after.size() == 1);
        test("记录包含reveal", after.get(0).contains("reveal"));
        System.out.println();
    }
}
