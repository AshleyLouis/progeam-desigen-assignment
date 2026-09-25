import java.io.*;
import java.net.*;

public class BoardPositionTest {
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
        System.out.println("=== Initial Board (Red perspective) ===");
        total++;
        String[][] board = parseBoard(msg1);
        if (board == null) {
            System.out.println("FAIL: Could not parse board");
        } else {
            printBoard(board);

            boolean ok = true;
            if (!"bRook".equals(board[0][0])) { System.out.println("  FAIL: [0][0] should be bRook, got " + board[0][0]); ok = false; }
            if (!"bKing".equals(board[0][4])) { System.out.println("  FAIL: [0][4] should be bKing, got " + board[0][4]); ok = false; }
            if (!"bRook".equals(board[0][8])) { System.out.println("  FAIL: [0][8] should be bRook, got " + board[0][8]); ok = false; }
            if (!"bCannon".equals(board[2][1])) { System.out.println("  FAIL: [2][1] should be bCannon, got " + board[2][1]); ok = false; }
            if (!"bPawn".equals(board[3][0])) { System.out.println("  FAIL: [3][0] should be bPawn, got " + board[3][0]); ok = false; }
            if (!"rPawn".equals(board[6][0])) { System.out.println("  FAIL: [6][0] should be rPawn, got " + board[6][0]); ok = false; }
            if (!"rCannon".equals(board[7][1])) { System.out.println("  FAIL: [7][1] should be rCannon, got " + board[7][1]); ok = false; }
            if (!"rKing".equals(board[9][4])) { System.out.println("  FAIL: [9][4] should be rKing, got " + board[9][4]); ok = false; }
            if (!"rRook".equals(board[9][8])) { System.out.println("  FAIL: [9][8] should be rRook, got " + board[9][8]); ok = false; }

            if (ok) {
                System.out.println("  PASS: All pieces at correct positions");
                passed++;
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

    static String[][] parseBoard(String msg) {
        int boardIdx = msg.indexOf("\"board\":");
        if (boardIdx == -1) return null;
        int start = msg.indexOf("[[", boardIdx);
        if (start == -1) return null;
        int end = msg.indexOf("]]", start);
        if (end == -1) return null;

        String inner = msg.substring(start + 2, end);

        java.util.List<String> rows = new java.util.ArrayList<String>();
        int searchFrom = 0;
        int rowStart = 0;
        while (true) {
            int idx = inner.indexOf("],[", searchFrom);
            if (idx == -1) {
                rows.add(inner.substring(rowStart));
                break;
            }
            rows.add(inner.substring(rowStart, idx));
            rowStart = idx + 3;
            searchFrom = idx + 3;
        }

        String[][] board = new String[rows.size()][];
        for (int i = 0; i < rows.size(); i++) {
            String row = rows.get(i);
            java.util.List<String> cells = new java.util.ArrayList<String>();
            int cStart = 0;
            boolean inQuote = false;
            for (int j = 0; j < row.length(); j++) {
                char c = row.charAt(j);
                if (c == '"') inQuote = !inQuote;
                else if (c == ',' && !inQuote) {
                    cells.add(row.substring(cStart, j).replace("\"", "").trim());
                    cStart = j + 1;
                }
            }
            cells.add(row.substring(cStart).replace("\"", "").trim());
            board[i] = cells.toArray(new String[0]);
        }
        return board;
    }

    static void printBoard(String[][] board) {
        System.out.println("    a   b   c   d   e   f   g   h   i");
        for (int y = 0; y < board.length && y < 10; y++) {
            System.out.print(y + "  ");
            for (int x = 0; x < board[y].length && x < 9; x++) {
                String p = board[y][x];
                if (p == null || p.equals("null") || p.isEmpty()) {
                    System.out.print("..  ");
                } else {
                    String s = p.length() > 4 ? p.substring(0, 4) : p;
                    System.out.print(s + " ");
                    if (s.length() < 4) System.out.print(" ");
                }
            }
            System.out.println();
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
