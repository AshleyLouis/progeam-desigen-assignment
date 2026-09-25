import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Map;

public class AutoDemo {
    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8887;
        boolean autoMode = args.length > 1 && "auto".equalsIgnoreCase(args[1]);

        System.out.println("========== 揭棋自动演示 ==========\n");

        System.out.println("启动服务器...");
        Process serverProcess = Runtime.getRuntime().exec(
            "java -cp bin GameServer " + port
        );
        Thread.sleep(1500);
        System.out.println("✅ 服务器已启动 (端口 " + port + ")\n");

        DemoClient red = new DemoClient("localhost", port, "红方");
        DemoClient black = new DemoClient("localhost", port, "黑方");
        Thread.sleep(500);
        System.out.println("✅ 双方客户端已连接\n");

        String[][] demoMoves = {
            {"red", "move", "a6", "a5"},
            {"black", "move", "a3", "a4"},
            {"red", "flip", "b9"},
            {"black", "flip", "b0"},
            {"red", "move", "c6", "c5"},
            {"black", "move", "c3", "c4"},
            {"red", "move", "e6", "e5"},
            {"black", "flip", "d0"},
            {"red", "flip", "d9"},
            {"black", "move", "e3", "e4"},
            {"red", "move", "g6", "g5"},
            {"black", "move", "g3", "g4"},
            {"red", "move", "i6", "i5"},
            {"black", "move", "i3", "i4"},
        };

        System.out.println("========== 开始演示对弈 ==========\n");
        DemoClient currentPlayer = red;
        for (int i = 0; i < demoMoves.length; i++) {
            String[] step = demoMoves[i];
            String expectedPlayer = step[0];
            DemoClient stepPlayer = "red".equals(expectedPlayer) ? red : black;
            String cmd = step[1];
            String desc;

            if (stepPlayer != currentPlayer) {
                System.out.println("  ⚠️  注意：当前应该是 " + currentPlayer.name + " 的回合，跳过此步");
                continue;
            }

            if ("move".equals(cmd)) {
                desc = "走子 " + step[2] + " → " + step[3];
                currentPlayer.sendMove(step[2], step[3], false);
            } else {
                desc = "翻子 " + step[2];
                currentPlayer.sendFlip(step[2]);
            }

            System.out.println("第 " + (i + 1) + " 步 - " + currentPlayer.name + " " + desc);
            Thread.sleep(400);

            String result = currentPlayer.getLastMoveResult();
            boolean success = result != null && result.contains("\"valid\":true");
            if (success) {
                System.out.println("  ✅ 成功");
                String flip = getJsonField(result, "flipResult");
                if (flip != null && flip.length() > 0) {
                    System.out.println("  🎴 翻出: " + flip);
                }
                String captured = getJsonField(result, "captured");
                if (captured != null && captured.length() > 0 && !"hidden".equals(captured)) {
                    System.out.println("  ⚔️  吃掉: " + captured);
                }
                currentPlayer = currentPlayer == red ? black : red;
            } else if (result != null) {
                String msg = getJsonField(result, "message");
                System.out.println("  ❌ 失败: " + msg);
            }
            System.out.println();

            if (!autoMode) {
                System.out.print("按回车继续...");
                new BufferedReader(new InputStreamReader(System.in)).readLine();
            }
        }

        System.out.println("\n========== 演示结束 ==========");
        System.out.println("\n你可以继续手动测试，或按回车退出演示。");
        System.out.print("按回车退出...");
        new BufferedReader(new InputStreamReader(System.in)).readLine();

        serverProcess.destroy();
        red.close();
        black.close();
        System.out.println("\n演示程序已退出。");
    }

    static String getJsonField(String json, String field) {
        String search = "\"" + field + "\":";
        int idx = json.indexOf(search);
        if (idx < 0) return null;
        int start = idx + search.length();
        if (start < json.length() && json.charAt(start) == '"') {
            start++;
            int end = json.indexOf("\"", start);
            if (end > 0) return json.substring(start, end);
        } else {
            int end = json.indexOf(",", start);
            if (end < 0) end = json.indexOf("}", start);
            if (end > 0) return json.substring(start, end).trim();
        }
        return null;
    }

    static class DemoClient {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private StringBuilder messages = new StringBuilder();
        private Thread readThread;
        String name;

        public DemoClient(String host, int port, String name) throws Exception {
            this.name = name;
            socket = new Socket(host, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            readThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        String line;
                        while ((line = in.readLine()) != null) {
                            synchronized (messages) {
                                messages.append(line).append("\n");
                            }
                        }
                    } catch (Exception e) {
                    }
                }
            });
            readThread.setDaemon(true);
            readThread.start();
        }

        public void sendMove(String src, String dst, boolean flip) {
            Position source = Position.fromText(src);
            Position destination = Position.fromText(dst);
            out.println(JsonUtil.object("messageType", "move",
                    "source", src, "destination", dst,
                    "fromX", String.valueOf((char) ('a' + source.getX())),
                    "fromY", source.getY(),
                    "toX", String.valueOf((char) ('a' + destination.getX())),
                    "toY", destination.getY(),
                    "isFlip", flip, "turnStartTime", System.currentTimeMillis()));
        }

        public void sendFlip(String pos) {
            sendMove(pos, pos, true);
        }

        public String getLastMoveResult() {
            synchronized (messages) {
                String all = messages.toString();
                String[] lines = all.split("\n");
                for (int i = lines.length - 1; i >= 0; i--) {
                    if (lines[i].contains("\"messageType\":\"moveResult\"")) {
                        return lines[i];
                    }
                }
                return null;
            }
        }

        public void close() throws Exception {
            socket.close();
        }
    }
}
