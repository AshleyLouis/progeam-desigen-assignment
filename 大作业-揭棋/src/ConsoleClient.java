import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ConsoleClient {
    public static void main(String[] args) throws Exception {
        String host = args.length > 0 ? args[0] : "localhost";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 8887;
        Socket socket = new Socket(host, port);
        final BufferedReader server = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
        final PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader console = new BufferedReader(new InputStreamReader(System.in, "UTF-8"));

        Thread readThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String line;
                    while ((line = server.readLine()) != null) {
                        System.out.println("[server] " + line);
                        if (line.contains("\"messageType\":\"gameOver\"")) {
                            System.out.println("\n游戏结束！按回车键退出...");
                            System.exit(0);
                        }
                    }
                } catch (Exception ex) {
                    System.out.println("server connection closed");
                    System.exit(0);
                }
            }
        });
        readThread.setDaemon(true);
        readThread.start();

        System.out.println("Connected. Commands:");
        System.out.println("  move b3 b4");
        System.out.println("  flip b3");
        System.out.println("  resign");
        System.out.println("  ping");
        while (true) {
            System.out.print("> ");
            String input = console.readLine();
            if (input == null) {
                break;
            }
            input = input.trim();
            if (input.length() == 0) {
                continue;
            }
            String[] parts = input.split("\\s+");
            if ("move".equalsIgnoreCase(parts[0]) && parts.length == 3) {
                Position source = Position.fromText(parts[1]);
                Position destination = Position.fromText(parts[2]);
                writer.println(JsonUtil.object("messageType", "move",
                        "source", parts[1],
                        "destination", parts[2],
                        "fromX", String.valueOf((char) ('a' + source.getX())),
                        "fromY", source.getY(),
                        "toX", String.valueOf((char) ('a' + destination.getX())),
                        "toY", destination.getY(),
                        "isFlip", false,
                        "turnStartTime", System.currentTimeMillis()));
            } else if ("flip".equalsIgnoreCase(parts[0]) && parts.length == 2) {
                Position position = Position.fromText(parts[1]);
                writer.println(JsonUtil.object("messageType", "move",
                        "source", parts[1],
                        "destination", parts[1],
                        "fromX", String.valueOf((char) ('a' + position.getX())),
                        "fromY", position.getY(),
                        "toX", String.valueOf((char) ('a' + position.getX())),
                        "toY", position.getY(),
                        "isFlip", true,
                        "turnStartTime", System.currentTimeMillis()));
            } else if ("resign".equalsIgnoreCase(parts[0])) {
                writer.println(JsonUtil.object("messageType", "Resign"));
                break;
            } else if ("ping".equalsIgnoreCase(parts[0])) {
                writer.println(JsonUtil.object("messageType", "ping", "timestamp", System.currentTimeMillis()));
            } else {
                System.out.println("unknown command");
            }
        }
        socket.close();
    }
}
