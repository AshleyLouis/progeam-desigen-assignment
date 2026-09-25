import java.io.*;
import java.net.*;

public class DebugMsgTest {
    public static void main(String[] args) throws Exception {
        Socket s1 = new Socket("localhost", 8887);
        BufferedReader r1 = new BufferedReader(new InputStreamReader(s1.getInputStream(), "UTF-8"));
        PrintWriter w1 = new PrintWriter(s1.getOutputStream(), true);

        Socket s2 = new Socket("localhost", 8887);
        BufferedReader r2 = new BufferedReader(new InputStreamReader(s2.getInputStream(), "UTF-8"));

        String gameStart = r1.readLine();
        r2.readLine();

        System.out.println("=== gameStart (first 300 chars) ===");
        System.out.println(gameStart.substring(0, Math.min(300, gameStart.length())));
        System.out.println();
        System.out.println("=== gameStart length: " + gameStart.length());

        int boardIdx = gameStart.indexOf("\"board\":");
        System.out.println("\"board\" at index: " + boardIdx);
        if (boardIdx >= 0) {
            System.out.println("board field (first 100 chars): " + gameStart.substring(boardIdx, Math.min(boardIdx + 100, gameStart.length())));
        }

        int initIdx = gameStart.indexOf("\"initialBoard\":");
        System.out.println("\"initialBoard\" at index: " + initIdx);

        System.out.println();
        System.out.println("=== Now sending flip move ===");
        w1.println("{\"messageType\":\"move\",\"source\":\"a6\",\"destination\":\"a6\",\"isFlip\":true}");

        for (int k = 0; k < 3; k++) {
            String line = r1.readLine();
            if (line == null) break;
            System.out.println();
            System.out.println("--- Red msg " + k + " (len=" + line.length() + ") ---");
            if (line.length() > 300) {
                System.out.println(line.substring(0, 300) + "...");
            } else {
                System.out.println(line);
            }
            if (line.contains("\"messageType\":\"moveResult\"")) {
                int bIdx = line.indexOf("\"board\":");
                if (bIdx >= 0) {
                    System.out.println("  board at " + bIdx + ": " + line.substring(bIdx, Math.min(bIdx + 100, line.length())));
                }
            }
        }

        w1.println("{\"messageType\":\"Resign\"}");
        s1.close();
        s2.close();
    }
}
