import java.io.*;
import java.net.*;

public class RestartTest {
    public static void main(String[] args) throws Exception {
        int passed = 0;
        int total = 0;

        System.out.println("Connecting to server...");
        Socket s1 = new Socket("localhost", 8887);
        BufferedReader r1 = new BufferedReader(new InputStreamReader(s1.getInputStream(), "UTF-8"));
        PrintWriter w1 = new PrintWriter(s1.getOutputStream(), true);

        Socket s2 = new Socket("localhost", 8887);
        BufferedReader r2 = new BufferedReader(new InputStreamReader(s2.getInputStream(), "UTF-8"));
        PrintWriter w2 = new PrintWriter(s2.getOutputStream(), true);

        String g1 = waitForMessage(r1, "gameStart");
        String g2 = waitForMessage(r2, "gameStart");
        if (g1.contains("yourTurn")) { r2.readLine(); } else { r1.readLine(); }

        System.out.println();
        System.out.println("[Test 1] Game starts properly...");
        total++;
        if (g1.contains("\"messageType\":\"gameStart\"") && g2.contains("\"messageType\":\"gameStart\"")) {
            System.out.println("  PASS");
            passed++;
        } else {
            System.out.println("  FAIL");
        }

        System.out.println();
        System.out.println("[Test 2] Red resigns, game over...");
        total++;
        w1.println("{\"messageType\":\"Resign\"}");
        String go1 = waitForMessage(r1, "gameOver");
        String go2 = waitForMessage(r2, "gameOver");
        if (go1 != null && go2 != null) {
            System.out.println("  PASS: Both got gameOver");
            passed++;
        } else {
            System.out.println("  FAIL");
        }

        System.out.println();
        System.out.println("[Test 3] Red requests restart, black also requests...");
        total++;
        w1.println("{\"messageType\":\"restart\"}");
        String rr1 = waitForMessage(r1, "restartRequested");
        String rr2 = waitForMessage(r2, "restartRequested");
        if (rr1 != null && rr2 != null) {
            System.out.println("  PASS: Both got restartRequested from red");
        } else {
            System.out.println("  FAIL: did not get restartRequested");
        }

        w2.println("{\"messageType\":\"restart\"}");
        String ra1 = waitForMessage(r1, "restartAccepted");
        String ra2 = waitForMessage(r2, "restartAccepted");
        String gs1 = waitForMessage(r1, "gameStart");
        String gs2 = waitForMessage(r2, "gameStart");
        if (ra1 != null && ra2 != null && gs1 != null && gs2 != null) {
            System.out.println("  PASS: Both got restartAccepted and new gameStart");
            passed++;
        } else {
            System.out.println("  FAIL: ra1=" + (ra1 != null) + " ra2=" + (ra2 != null) + " gs1=" + (gs1 != null) + " gs2=" + (gs2 != null));
        }

        System.out.println();
        System.out.println("[Test 4] New game can play moves...");
        total++;
        String yt = waitForMessage(r1, "yourTurn");
        if (yt == null) yt = waitForMessage(r2, "yourTurn");
        if (yt != null) {
            w1.println("{\"messageType\":\"move\",\"source\":\"a3\",\"destination\":\"a3\",\"isFlip\":true}");
            String mr = waitForMessage(r1, "moveResult");
            if (mr != null && mr.contains("\"success\":true")) {
                System.out.println("  PASS: Move works in new game");
                passed++;
            } else {
                System.out.println("  FAIL: moveResult invalid");
            }
        } else {
            System.out.println("  FAIL: no yourTurn in new game");
        }

        s1.close();
        s2.close();

        System.out.println();
        System.out.println("========== RESULT ==========");
        System.out.println("Passed: " + passed + "/" + total);
        if (passed == total) {
            System.out.println("ALL TESTS PASSED!");
        } else {
            System.out.println("SOME TESTS FAILED!");
            System.exit(1);
        }
    }

    static String waitForMessage(BufferedReader r, String msgType) throws Exception {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < 5000) {
            if (r.ready()) {
                String line = r.readLine();
                if (line == null) return null;
                if (line.contains("\"messageType\":\"" + msgType + "\"")) {
                    return line;
                }
            } else {
                Thread.sleep(50);
            }
        }
        return null;
    }
}
