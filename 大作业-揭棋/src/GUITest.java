import java.io.*;
import java.net.*;

public class GUITest {
    public static void main(String[] args) throws Exception {
        int passed = 0;
        int total = 0;

        total++;
        System.out.println("[Test " + total + "] Connect two clients...");
        Socket s1 = new Socket("localhost", 8887);
        BufferedReader r1 = new BufferedReader(new InputStreamReader(s1.getInputStream(), "UTF-8"));
        PrintWriter w1 = new PrintWriter(s1.getOutputStream(), true);

        Socket s2 = new Socket("localhost", 8887);
        BufferedReader r2 = new BufferedReader(new InputStreamReader(s2.getInputStream(), "UTF-8"));
        PrintWriter w2 = new PrintWriter(s2.getOutputStream(), true);

        String msg1 = r1.readLine();
        String msg2 = r2.readLine();
        System.out.println("  Client1 (red) got gameStart: " + (msg1 != null && msg1.contains("gameStart")));
        System.out.println("  Client2 (black) got gameStart: " + (msg2 != null && msg2.contains("gameStart")));

        if (msg1 != null && msg1.contains("\"messageType\":\"gameStart\"") &&
            msg2 != null && msg2.contains("\"messageType\":\"gameStart\"")) {
            System.out.println("  PASS");
            passed++;
        } else {
            System.out.println("  FAIL");
        }

        total++;
        System.out.println("[Test " + total + "] Check board field...");
        if (msg1.contains("\"board\":[[") && msg2.contains("\"board\":[[")) {
            System.out.println("  PASS: board array present");
            passed++;
        } else {
            System.out.println("  FAIL: board array missing");
        }

        total++;
        System.out.println("[Test " + total + "] Check piece format in board...");
        if (msg1.contains("\"bRook\"") && msg1.contains("\"rPawn\"")) {
            System.out.println("  PASS: pieces in bRook/rPawn format");
            passed++;
        } else {
            System.out.println("  FAIL: unexpected piece format");
        }

        total++;
        System.out.println("[Test " + total + "] Check yourTurn for red...");
        String turn1 = r1.readLine();
        System.out.println("  Red yourTurn: " + (turn1 != null && turn1.contains("yourTurn")));
        if (turn1 != null && turn1.contains("\"messageType\":\"yourTurn\"")) {
            System.out.println("  PASS");
            passed++;
        } else {
            System.out.println("  FAIL");
        }

        total++;
        System.out.println("[Test " + total + "] Red flips a pawn at a6...");
        w1.println("{\"messageType\":\"move\",\"source\":\"a6\",\"destination\":\"a6\",\"isFlip\":true}");
        String moveResult = r1.readLine();
        boolean success = moveResult != null && moveResult.contains("\"success\":true");
        System.out.println("  Move result success: " + success);
        if (success) {
            System.out.println("  PASS");
            passed++;
        } else {
            System.out.println("  FAIL: " + moveResult);
        }

        total++;
        System.out.println("[Test " + total + "] Black gets moveResult...");
        String oppMove = r2.readLine();
        boolean oppGot = oppMove != null && oppMove.contains("\"messageType\":\"moveResult\"");
        System.out.println("  Black got moveResult: " + oppGot);
        if (oppGot) {
            System.out.println("  PASS");
            passed++;
        } else {
            System.out.println("  FAIL: " + oppMove);
        }

        total++;
        System.out.println("[Test " + total + "] Black gets yourTurn...");
        String oppTurn = r2.readLine();
        boolean oppTurnOk = oppTurn != null && oppTurn.contains("\"messageType\":\"yourTurn\"");
        System.out.println("  Black got yourTurn: " + oppTurnOk);
        if (oppTurnOk) {
            System.out.println("  PASS");
            passed++;
        } else {
            System.out.println("  FAIL: " + oppTurn);
        }

        total++;
        System.out.println("[Test " + total + "] Resign test...");
        w2.println("{\"messageType\":\"Resign\"}");
        String gameOver1 = r1.readLine();
        String gameOver2 = r2.readLine();
        boolean go1 = gameOver1 != null && gameOver1.contains("\"messageType\":\"gameOver\"");
        boolean go2 = gameOver2 != null && gameOver2.contains("\"messageType\":\"gameOver\"");
        System.out.println("  Red gameOver: " + go1);
        System.out.println("  Black gameOver: " + go2);
        if (go1 && go2) {
            System.out.println("  PASS");
            passed++;
        } else {
            System.out.println("  FAIL");
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
}
