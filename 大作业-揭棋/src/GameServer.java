import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class GameServer {
    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8887;
        Long seed = args.length > 1 ? Long.parseLong(args[1]) : null;
        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("========================================");
        System.out.println("   JieQi Server Started");
        System.out.println("========================================");
        System.out.println("Port: " + port);
        System.out.println();
        System.out.println("Available IP addresses for other groups to connect:");
        try {
            java.util.Enumeration<java.net.NetworkInterface> interfaces =
                    java.net.NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                java.net.NetworkInterface iface = interfaces.nextElement();
                if (!iface.isUp() || iface.isLoopback()) continue;
                java.util.Enumeration<java.net.InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    java.net.InetAddress addr = addresses.nextElement();
                    if (addr instanceof java.net.Inet4Address) {
                        System.out.println("  " + iface.getDisplayName() + ": " + addr.getHostAddress()
                                + ":" + port);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("  (Unable to enumerate network interfaces)");
        }
        System.out.println("  localhost:" + port);
        System.out.println();
        System.out.println("Waiting for clients... (supports multiple concurrent games)");
        System.out.println("========================================");

        int roomNumber = 1;
        while (true) {
            Socket redSocket = serverSocket.accept();
            System.out.println("\n[Room " + roomNumber + "] Red client connected: "
                    + redSocket.getRemoteSocketAddress());

            Game game = new Game(seed);
            ClientHandler red = new ClientHandler(redSocket, Color.RED, game);
            red.send(JsonUtil.object("messageType", "waiting",
                    "message", "Waiting for opponent to connect..."));

            Socket blackSocket = serverSocket.accept();
            System.out.println("[Room " + roomNumber + "] Black client connected: "
                    + blackSocket.getRemoteSocketAddress());

            ClientHandler black = new ClientHandler(blackSocket, Color.BLACK, game);
            red.setOpponent(black);
            black.setOpponent(red);
            red.start();
            black.start();
            red.sendGameStart();
            black.sendGameStart();

            Thread timeoutThread = new Thread(
                    new TimeoutWatcher(game, red, black, roomNumber),
                    "timeout-watcher-" + roomNumber);
            timeoutThread.setDaemon(true);
            timeoutThread.start();

            System.out.println("[Room " + roomNumber + "] Game started!");
            roomNumber++;
        }
    }
}

class TimeoutWatcher implements Runnable {
    private final Game game;
    private final ClientHandler red;
    private final ClientHandler black;
    private final int roomNumber;

    public TimeoutWatcher(Game game, ClientHandler red, ClientHandler black, int roomNumber) {
        this.game = game;
        this.red = red;
        this.black = black;
        this.roomNumber = roomNumber;
    }

    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return;
            }
            if (!red.isRunning() && !black.isRunning()) {
                System.out.println("[Room " + roomNumber + "] Both clients disconnected, stopping watcher.");
                return;
            }
            if (!game.isOver() && System.currentTimeMillis() - game.getTurnStartTime() > Game.TURN_LIMIT_MILLIS) {
                Color loser = game.getCurrentTurn();
                Color winner = game.forceTimeoutIfNeeded();
                if (winner == null) {
                    winner = loser.opposite();
                }
                String msg = JsonUtil.object("messageType", "gameOver",
                        "winner", winner.protocolName(),
                        "winnerId", winner.protocolName(),
                        "loserId", loser.protocolName(),
                        "reason", "timeout");
                red.send(msg);
                black.send(msg);
                System.out.println("[Room " + roomNumber + "] timeout: " + loser.protocolName() + " loses");
            }
        }
    }
}
