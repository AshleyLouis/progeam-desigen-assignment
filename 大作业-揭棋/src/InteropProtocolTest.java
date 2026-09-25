import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class InteropProtocolTest {
    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8897;
        Thread server = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    GameServer.main(new String[]{String.valueOf(port), "12345"});
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        server.setDaemon(true);
        server.start();
        Thread.sleep(800);

        TestPeer red = new TestPeer(port);
        TestPeer black = new TestPeer(port);
        red.waitFor("gameStart");
        red.waitFor("yourTurn");
        black.waitFor("gameStart");

        red.send("{\"messageType\":\"move\",\"fromX\":\"a\",\"fromY\":6,\"toX\":\"a\",\"toY\":5,\"isFlip\":false}");
        String result = red.waitFor("moveResult");
        System.out.println("red result: " + result);
        String opponent = black.waitFor("moveResult");
        System.out.println("black result: " + opponent);
        black.waitFor("yourTurn");

        red.close();
        black.close();
        if (!result.contains("\"success\":true") || !result.contains("\"fromX\":\"a\"")
                || !opponent.contains("\"toY\":5")) {
            throw new AssertionError("公共协议字段互操作失败: " + result);
        }
        System.out.println("InteropProtocolTest PASS");
    }

    private static class TestPeer {
        private final Socket socket;
        private final BufferedReader in;
        private final PrintWriter out;

        TestPeer(int port) throws Exception {
            socket = new Socket("localhost", port);
            socket.setSoTimeout(8000);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            out = new PrintWriter(socket.getOutputStream(), true);
        }

        void send(String line) {
            out.println(line);
        }

        String waitFor(String messageType) throws Exception {
            String line;
            while ((line = in.readLine()) != null) {
                if (line.contains("\"messageType\":\"" + messageType + "\"")) {
                    return line;
                }
            }
            throw new IllegalStateException("connection closed before " + messageType);
        }

        void close() throws Exception {
            socket.close();
        }
    }
}
