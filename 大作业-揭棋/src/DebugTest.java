import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class DebugTest {
    public static void main(String[] args) throws Exception {
        Process serverProcess = Runtime.getRuntime().exec("java -cp bin GameServer 9997");
        Thread.sleep(1500);

        Socket redSocket = new Socket("localhost", 9997);
        PrintWriter redOut = new PrintWriter(redSocket.getOutputStream(), true);
        BufferedReader redIn = new BufferedReader(new InputStreamReader(redSocket.getInputStream(), "UTF-8"));

        Socket blackSocket = new Socket("localhost", 9997);
        PrintWriter blackOut = new PrintWriter(blackSocket.getOutputStream(), true);
        BufferedReader blackIn = new BufferedReader(new InputStreamReader(blackSocket.getInputStream(), "UTF-8"));

        Thread.sleep(500);

        System.out.println("=== 红方初始消息 ===");
        while (redIn.ready()) {
            System.out.println(redIn.readLine());
        }

        System.out.println("\n=== 黑方初始消息 ===");
        while (blackIn.ready()) {
            System.out.println(blackIn.readLine());
        }

        System.out.println("\n=== 红方移动 a6->a5 ===");
        redOut.println("{\"messageType\":\"move\",\"source\":\"a6\",\"destination\":\"a5\",\"isFlip\":false,\"turnStartTime\":" + System.currentTimeMillis() + "}");
        Thread.sleep(1000);

        System.out.println("红方收到:");
        while (redIn.ready()) {
            System.out.println("  " + redIn.readLine());
        }
        System.out.println("黑方收到:");
        while (blackIn.ready()) {
            System.out.println("  " + blackIn.readLine());
        }

        System.out.println("\n=== 黑方原地翻子 a3 ===");
        blackOut.println("{\"messageType\":\"move\",\"source\":\"a3\",\"destination\":\"a3\",\"isFlip\":true,\"turnStartTime\":" + System.currentTimeMillis() + "}");
        Thread.sleep(1000);

        System.out.println("黑方收到:");
        while (blackIn.ready()) {
            System.out.println("  " + blackIn.readLine());
        }
        System.out.println("红方收到:");
        while (redIn.ready()) {
            System.out.println("  " + redIn.readLine());
        }

        serverProcess.destroy();
        redSocket.close();
        blackSocket.close();
    }
}
