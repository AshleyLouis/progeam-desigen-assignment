import java.io.*;
import java.net.*;

public class DebugRestart {
    public static void main(String[] args) throws Exception {
        Socket s1 = new Socket("localhost", 8887);
        BufferedReader r1 = new BufferedReader(new InputStreamReader(s1.getInputStream(), "UTF-8"));
        PrintWriter w1 = new PrintWriter(s1.getOutputStream(), true);

        Socket s2 = new Socket("localhost", 8887);
        BufferedReader r2 = new BufferedReader(new InputStreamReader(s2.getInputStream(), "UTF-8"));
        PrintWriter w2 = new PrintWriter(s2.getOutputStream(), true);

        drain(r1, "initial game");
        drain(r2, "initial game");

        System.out.println("=== Red resigns ===");
        w1.println("{\"messageType\":\"Resign\"}");
        Thread.sleep(200);
        drain(r1, "red after resign");
        drain(r2, "black after resign");

        System.out.println();
        System.out.println("=== Red requests restart ===");
        w1.println("{\"messageType\":\"restart\"}");
        Thread.sleep(200);
        drain(r1, "red after red restart");
        drain(r2, "black after red restart");

        System.out.println();
        System.out.println("=== Black requests restart ===");
        w2.println("{\"messageType\":\"restart\"}");
        Thread.sleep(500);
        drain(r1, "red after black restart");
        drain(r2, "black after black restart");

        s1.close();
        s2.close();
    }

    static void drain(BufferedReader r, String label) throws Exception {
        System.out.println("--- " + label + " ---");
        int count = 0;
        while (r.ready()) {
            String line = r.readLine();
            if (line == null) break;
            count++;
            if (line.length() > 100) {
                System.out.println("  " + count + ": " + line.substring(0, 100) + "...");
            } else {
                System.out.println("  " + count + ": " + line);
            }
        }
        if (count == 0) System.out.println("  (no messages)");
    }
}
