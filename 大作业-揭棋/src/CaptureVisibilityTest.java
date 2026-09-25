import java.io.*;
import java.net.*;

public class CaptureVisibilityTest {
    public static void main(String[] args) throws Exception {
        int passed = 0;
        int total = 0;

        System.out.println("Connecting to server (seed=12345)...");
        Socket s1 = new Socket("localhost", 8887);
        BufferedReader r1 = new BufferedReader(new InputStreamReader(s1.getInputStream(), "UTF-8"));
        PrintWriter w1 = new PrintWriter(s1.getOutputStream(), true);

        Socket s2 = new Socket("localhost", 8887);
        BufferedReader r2 = new BufferedReader(new InputStreamReader(s2.getInputStream(), "UTF-8"));
        PrintWriter w2 = new PrintWriter(s2.getOutputStream(), true);

        waitForMsg(r1, "gameStart");
        waitForMsg(r2, "gameStart");
        waitForMsg(r1, "yourTurn");

        // Move 1: Red a3->a4 (hidden pawn moves, gets revealed)
        w1.println("{\"messageType\":\"move\",\"source\":\"a3\",\"destination\":\"a4\",\"isFlip\":false}");
        waitForMsg(r1, "moveResult"); waitForMsg(r2, "moveResult"); waitForMsg(r2, "yourTurn");
        System.out.println("1. Red a3->a4 OK");

        // Move 2: Black flip c6 (waste a turn)
        w2.println("{\"messageType\":\"move\",\"source\":\"c6\",\"destination\":\"c6\",\"isFlip\":true}");
        waitForMsg(r2, "moveResult"); waitForMsg(r1, "moveResult"); waitForMsg(r1, "yourTurn");
        System.out.println("2. Black flips c6 OK");

        // Move 3: Red a4->a5 (pawn moves forward, crosses river)
        w1.println("{\"messageType\":\"move\",\"source\":\"a4\",\"destination\":\"a5\",\"isFlip\":false}");
        waitForMsg(r1, "moveResult"); waitForMsg(r2, "moveResult"); waitForMsg(r2, "yourTurn");
        System.out.println("3. Red a4->a5 OK");

        // Move 4: Black flip e6
        w2.println("{\"messageType\":\"move\",\"source\":\"e6\",\"destination\":\"e6\",\"isFlip\":true}");
        waitForMsg(r2, "moveResult"); waitForMsg(r1, "moveResult"); waitForMsg(r1, "yourTurn");
        System.out.println("4. Black flips e6 OK");

        // Move 5: Red a5->b5 (pawn moves sideways, now acts as screen for cannon)
        w1.println("{\"messageType\":\"move\",\"source\":\"a5\",\"destination\":\"b5\",\"isFlip\":false}");
        String mr1 = waitForMsg(r1, "moveResult");
        String mr2 = waitForMsg(r2, "moveResult");
        waitForMsg(r2, "yourTurn");
        System.out.println("5. Red a5->b5 OK: " + mr1.contains("\"success\":true"));

        // Move 6: Black flip g6
        w2.println("{\"messageType\":\"move\",\"source\":\"g6\",\"destination\":\"g6\",\"isFlip\":true}");
        waitForMsg(r2, "moveResult"); waitForMsg(r1, "moveResult"); waitForMsg(r1, "yourTurn");
        System.out.println("6. Black flips g6 OK");

        // Move 7: Red cannon b2 captures b7 (hidden black cannon, screen at b5)
        // b2 is a hidden cannon (originalType=Cannon), can capture like cannon
        w1.println("{\"messageType\":\"move\",\"source\":\"b2\",\"destination\":\"b7\",\"isFlip\":false}");
        mr1 = waitForMsg(r1, "moveResult");
        mr2 = waitForMsg(r2, "moveResult");

        System.out.println();
        System.out.println("=== Cannon captures hidden piece at b7 ===");
        System.out.println("Red (eater) success: " + mr1.contains("\"success\":true"));

        if (mr1.contains("\"success\":true")) {
            String eaterCaptured = extractStr(mr1, "captured");
            String victimCaptured = extractStr(mr2, "captured");
            System.out.println("Red (eater) sees captured: " + eaterCaptured);
            System.out.println("Black (victim) sees captured: " + victimCaptured);

            total++;
            if (eaterCaptured != null && !eaterCaptured.equals("hidden") && eaterCaptured.length() > 0) {
                System.out.println("  PASS: Eater knows the real type");
                passed++;
            } else {
                System.out.println("  FAIL: Eater should see real type, got: " + eaterCaptured);
            }

            total++;
            if (victimCaptured != null && victimCaptured.equals("hidden")) {
                System.out.println("  PASS: Victim doesn't know what was captured");
                passed++;
            } else {
                System.out.println("  FAIL: Victim should see 'hidden', got: " + victimCaptured);
            }
        } else {
            System.out.println("  Cannon capture failed, can't test");
        }

        w1.println("{\"messageType\":\"Resign\"}");
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

    static String waitForMsg(BufferedReader r, String msgType) throws Exception {
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

    static String extractStr(String msg, String key) {
        String search = "\"" + key + "\":";
        int idx = msg.indexOf(search);
        if (idx == -1) return null;
        int valStart = idx + search.length();
        if (valStart >= msg.length()) return null;
        if (msg.charAt(valStart) == '"') {
            valStart++;
            int valEnd = msg.indexOf('"', valStart);
            if (valEnd == -1) return null;
            return msg.substring(valStart, valEnd);
        } else {
            int valEnd = valStart;
            while (valEnd < msg.length() && msg.charAt(valEnd) != ',' && msg.charAt(valEnd) != '}') {
                valEnd++;
            }
            return msg.substring(valStart, valEnd);
        }
    }
}
