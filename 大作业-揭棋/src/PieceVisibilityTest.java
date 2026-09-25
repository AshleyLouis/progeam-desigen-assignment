import java.io.*;
import java.net.*;

public class PieceVisibilityTest {
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

        String msg1 = waitForMsg(r1, "gameStart");
        waitForMsg(r2, "gameStart");

        System.out.println();
        System.out.println("[Test 1] Check initialBoard field present...");
        total++;
        if (msg1.contains("\"initialBoard\":")) {
            System.out.println("  PASS");
            passed++;
        } else {
            System.out.println("  FAIL: no initialBoard field");
        }

        System.out.println();
        System.out.println("[Test 2] Parse initialBoard and check visibility...");
        total++;
        int initIdx = msg1.indexOf("\"initialBoard\":");
        int arrStart = msg1.indexOf("[{\"x\":", initIdx);
        if (arrStart == -1) {
            System.out.println("  FAIL: can't find object array");
        } else {
            int depth = 0;
            boolean inQuote = false;
            int arrEnd = -1;
            for (int i = arrStart; i < msg1.length(); i++) {
                char c = msg1.charAt(i);
                if (c == '"') inQuote = !inQuote;
                else if (!inQuote) {
                    if (c == '{' || c == '[') depth++;
                    else if (c == '}' || c == ']') {
                        depth--;
                        if (depth == 0) { arrEnd = i; break; }
                    }
                }
            }
            if (arrEnd == -1) {
                System.out.println("  FAIL: can't find array end");
            } else {
                String data = msg1.substring(arrStart, arrEnd + 1);
                int visibleCount = 0;
                int hiddenCount = 0;
                int emptyCount = 0;
                int kingCount = 0;
                int knightCount = 0;

                int i = 0;
                while (i < data.length()) {
                    int os = data.indexOf('{', i);
                    if (os == -1) break;
                    int oe = data.indexOf('}', os);
                    if (oe == -1) break;
                    String obj = data.substring(os, oe + 1);

                    String piece = extractStr(obj, "piece");
                    String vis = extractStr(obj, "visible");

                    if ("empty".equals(piece)) {
                        emptyCount++;
                    } else {
                        if ("true".equals(vis)) visibleCount++;
                        else hiddenCount++;

                        if ("King".equals(piece)) kingCount++;
                        if ("Knight".equals(piece)) knightCount++;
                    }
                    i = oe + 1;
                }

                System.out.println("  Visible: " + visibleCount + ", Hidden: " + hiddenCount + ", Empty: " + emptyCount);
                System.out.println("  King count: " + kingCount + ", Knight count: " + knightCount);

                if (visibleCount == 2 && hiddenCount == 30) {
                    System.out.println("  PASS: 2 Kings visible, 30 hidden pieces at start");
                    passed++;
                } else {
                    System.out.println("  FAIL: Expected 2 visible, 30 hidden, got " + visibleCount + "/" + hiddenCount);
                }
            }
        }

        System.out.println();
        System.out.println("[Test 3] Flip a piece and check visibility changes...");
        total++;
        w1.println("{\"messageType\":\"move\",\"source\":\"a3\",\"destination\":\"a3\",\"isFlip\":true}");
        String moveResult = waitForMsg(r1, "moveResult");
        waitForMsg(r2, "moveResult");

        int boardIdx = moveResult.indexOf("\"board\":");
        int objArrStart = moveResult.indexOf("[{\"x\":", boardIdx);
        if (objArrStart == -1) {
            System.out.println("  FAIL: moveResult board not found or not object array");
        } else {
            int depth = 0;
            boolean inQuote = false;
            int arrEnd = -1;
            for (int i = objArrStart; i < moveResult.length(); i++) {
                char c = moveResult.charAt(i);
                if (c == '"') inQuote = !inQuote;
                else if (!inQuote) {
                    if (c == '{' || c == '[') depth++;
                    else if (c == '}' || c == ']') {
                        depth--;
                        if (depth == 0) { arrEnd = i; break; }
                    }
                }
            }
            if (arrEnd == -1) {
                System.out.println("  FAIL: can't find array end");
            } else {
                String data = moveResult.substring(objArrStart, arrEnd + 1);
                int visibleCount = 0;
                int hiddenCount = 0;

                int i = 0;
                while (i < data.length()) {
                    int os = data.indexOf('{', i);
                    if (os == -1) break;
                    int oe = data.indexOf('}', os);
                    if (oe == -1) break;
                    String obj = data.substring(os, oe + 1);

                    String piece = extractStr(obj, "piece");
                    String vis = extractStr(obj, "visible");

                    if (!"empty".equals(piece)) {
                        if ("true".equals(vis)) visibleCount++;
                        else hiddenCount++;
                    }
                    i = oe + 1;
                }

                System.out.println("  After flip - Visible: " + visibleCount + ", Hidden: " + hiddenCount);
                if (visibleCount == 3 && hiddenCount == 29) {
                    System.out.println("  PASS: 3 visible (2 Kings + 1 flipped), 29 hidden after one flip");
                    passed++;
                } else {
                    System.out.println("  FAIL: Expected 3 visible, 29 hidden");
                }
            }
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

    static String extractStr(String obj, String key) {
        String search = "\"" + key + "\":";
        int idx = obj.indexOf(search);
        if (idx == -1) return null;
        int valStart = idx + search.length();
        if (valStart >= obj.length()) return null;
        if (obj.charAt(valStart) == '"') {
            valStart++;
            int valEnd = obj.indexOf('"', valStart);
            if (valEnd == -1) return null;
            return obj.substring(valStart, valEnd);
        } else {
            int valEnd = valStart;
            while (valEnd < obj.length() && obj.charAt(valEnd) != ',' && obj.charAt(valEnd) != '}') {
                valEnd++;
            }
            return obj.substring(valStart, valEnd);
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
}
