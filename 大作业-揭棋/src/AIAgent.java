import java.io.*;
import java.net.*;

/**
 * AI Agent：连接揭棋服务器，自动进行对弈。
 * 使用Alpha-Beta剪枝搜索和暗子期望值评估来决定走法。
 *
 * 用法：java -cp bin AIAgent [host] [port] [searchDepth]
 */
public class AIAgent {
    private final String host;
    private final int port;
    private final int searchDepth;

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    private Color myColor;
    private Board board;
    private final MoveValidator validator = new MoveValidator();
    private boolean gameStarted = false;

    public AIAgent(String host, int port, int searchDepth) {
        this.host = host;
        this.port = port;
        this.searchDepth = searchDepth;
    }

    public void start() throws Exception {
        System.out.println("AI Agent connecting to " + host + ":" + port + "...");
        socket = new Socket(host, port);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
        out = new PrintWriter(socket.getOutputStream(), true);
        System.out.println("Connected!");

        String msg;
        while ((msg = in.readLine()) != null) {
            handleMessage(msg);
        }
        System.out.println("Connection closed.");
    }

    private void handleMessage(String msg) {
        if (msg.contains("\"messageType\":\"gameStart\"")) {
            handleGameStart(msg);
        } else if (msg.contains("\"messageType\":\"yourTurn\"")) {
            handleYourTurn();
        } else if (msg.contains("\"messageType\":\"moveResult\"")) {
            handleMoveResult(msg);
        } else if (msg.contains("\"messageType\":\"gameOver\"")) {
            handleGameOver(msg);
        }
    }

    private void handleGameStart(String msg) {
        String colorStr = extractStr(msg, "yourColor");
        myColor = "red".equals(colorStr) ? Color.RED : Color.BLACK;
        board = parseBoardFromMessage(msg);
        gameStarted = true;
        System.out.println("Game started. AI is " + myColor.protocolName().toUpperCase());
    }

    private void handleYourTurn() {
        if (!gameStarted || board == null) return;
        System.out.println("AI thinking...");

        long startTime = System.currentTimeMillis();
        AISearcher searcher = new AISearcher(myColor, searchDepth);
        Move bestMove = searcher.searchBestMove(board, myColor);
        long elapsed = System.currentTimeMillis() - startTime;

        if (bestMove != null) {
            String moveJson = JsonUtil.object("messageType", "move",
                    "source", bestMove.getSource().toText(),
                    "destination", bestMove.getDestination().toText(),
                    "fromX", String.valueOf((char) ('a' + bestMove.getSource().getX())),
                    "fromY", bestMove.getSource().getY(),
                    "toX", String.valueOf((char) ('a' + bestMove.getDestination().getX())),
                    "toY", bestMove.getDestination().getY(),
                    "isFlip", bestMove.isFlip());
            out.println(moveJson);
            System.out.println("AI move: " + bestMove.getSource().toText()
                    + (bestMove.isFlip() ? " (flip)" : " -> " + bestMove.getDestination().toText())
                    + " [" + elapsed + "ms]");
        } else {
            // 无合法走法，认输
            out.println("{\"messageType\":\"Resign\"}");
            System.out.println("AI has no legal moves, resigning.");
        }
    }

    private void handleMoveResult(String msg) {
        String success = extractStr(msg, "success");
        if (!"true".equals(success)) return;

        // 更新棋盘
        board = parseBoardFromMessage(msg);

        String src = firstNonEmpty(extractNestedStr(msg, "move", "source"),
                extractStr(msg, "source"), pairPosition(msg, "fromX", "fromY"));
        String dst = firstNonEmpty(extractNestedStr(msg, "move", "destination"),
                extractStr(msg, "destination"), pairPosition(msg, "toX", "toY"));
        String isFlip = firstNonEmpty(extractNestedStr(msg, "move", "isFlip"),
                extractStr(msg, "isFlip"));
        String captured = extractStr(msg, "captured");

        StringBuilder sb = new StringBuilder();
        if ("true".equals(isFlip)) {
            sb.append("Flip ").append(src);
        } else {
            sb.append("Move ").append(src).append(" -> ").append(dst);
        }
        if (captured != null && captured.length() > 0) {
            sb.append(" captured: ").append(captured);
        }
        System.out.println(sb.toString());
    }

    private void handleGameOver(String msg) {
        String winner = extractStr(msg, "winner");
        String reason = extractStr(msg, "reason");
        String result;
        if ("draw".equals(winner)) {
            result = "Draw";
        } else if (winner != null && winner.equals(myColor.protocolName())) {
            result = "AI Wins!";
        } else {
            result = "AI Loses";
        }
        System.out.println("Game Over - " + result + " (" + reason + ")");
    }

    /**
     * 从服务器消息解析棋盘状态。
     */
    private Board parseBoardFromMessage(String msg) {
        Board newBoard = new Board(true);

        // 优先使用 boardJsonFor 格式（对象数组）
        int boardIdx = msg.indexOf("\"board\":");
        if (boardIdx == -1) {
            boardIdx = msg.indexOf("\"initialBoard\":");
        }
        if (boardIdx == -1) return newBoard;

        int objStart = msg.indexOf("[{\"x\":", boardIdx);
        if (objStart != -1) {
            int depth = 0;
            boolean inQuote = false;
            int arrEnd = -1;
            for (int i = objStart; i < msg.length(); i++) {
                char c = msg.charAt(i);
                if (c == '"') inQuote = !inQuote;
                else if (!inQuote) {
                    if (c == '{' || c == '[') depth++;
                    else if (c == '}' || c == ']') {
                        depth--;
                        if (depth == 0) { arrEnd = i; break; }
                    }
                }
            }
            if (arrEnd != -1) {
                String data = msg.substring(objStart, arrEnd + 1);
                int i = 0;
                while (i < data.length()) {
                    int os = data.indexOf('{', i);
                    if (os == -1) break;
                    int oe = data.indexOf('}', os);
                    if (oe == -1) break;
                    String obj = data.substring(os, oe + 1);

                    String xStr = extractStr(obj, "x");
                    String yStr = extractStr(obj, "y");
                    String color = extractStr(obj, "color");
                    String piece = extractStr(obj, "piece");
                    String visible = extractStr(obj, "visible");

                    if (xStr != null && yStr != null && piece != null && !"empty".equals(piece)) {
                        int x = xStr.charAt(0) - 'a';
                        int y = Integer.parseInt(yStr);
                        boolean vis = "true".equals(visible);
                        Color c = "red".equals(color) ? Color.RED : Color.BLACK;
                        PieceType type = PieceType.fromProtocolName(piece);
                        // 可见棋子：realType = 显示的类型；暗子：realType = originalType（AI不知道真实类型）
                        Piece p = new Piece(c, type, type, vis);
                        if (x >= 0 && x < 9 && y >= 0 && y < 10) {
                            newBoard.set(new Position(x, y), p);
                        }
                    }
                    i = oe + 1;
                }
            }
        }

        return newBoard;
    }

    private String extractStr(String msg, String key) {
        String search = "\"" + key + "\":";
        int idx = msg.indexOf(search);
        if (idx == -1) return null;
        int valStart = idx + search.length();
        if (valStart >= msg.length()) return null;
        if (msg.charAt(valStart) == '"') {
            valStart++;
            int valEnd = msg.indexOf('"', valStart);
            if (valEnd == -1) return null;
            return msg.substring(valStart, valEnd);
        } else {
            int valEnd = valStart;
            while (valEnd < msg.length() && msg.charAt(valEnd) != ',' && msg.charAt(valEnd) != '}') {
                valEnd++;
            }
            return msg.substring(valStart, valEnd);
        }
    }

    private String extractNestedStr(String msg, String parent, String key) {
        String parentSearch = "\"" + parent + "\":";
        int pIdx = msg.indexOf(parentSearch);
        if (pIdx == -1) return null;
        String keySearch = "\"" + key + "\":";
        int kIdx = msg.indexOf(keySearch, pIdx);
        if (kIdx == -1) return null;
        int valStart = kIdx + keySearch.length();
        if (valStart >= msg.length()) return null;
        if (msg.charAt(valStart) == '"') {
            valStart++;
            int valEnd = msg.indexOf('"', valStart);
            if (valEnd == -1) return null;
            return msg.substring(valStart, valEnd);
        } else {
            int valEnd = valStart;
            while (valEnd < msg.length() && msg.charAt(valEnd) != ',' && msg.charAt(valEnd) != '}') {
                valEnd++;
            }
            return msg.substring(valStart, valEnd);
        }
    }

    private String firstNonEmpty(String... values) {
        for (String value : values) {
            if (value != null && value.length() > 0 && !"null".equals(value)) {
                return value;
            }
        }
        return null;
    }

    private String pairPosition(String msg, String xKey, String yKey) {
        String x = extractStr(msg, xKey);
        String y = extractStr(msg, yKey);
        if (x == null || y == null) {
            return null;
        }
        return x + y;
    }

    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : "localhost";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 8887;
        int depth = args.length > 2 ? Integer.parseInt(args[2]) : 3;

        AIAgent agent = new AIAgent(host, port, depth);
        try {
            agent.start();
        } catch (Exception e) {
            System.err.println("AI Agent error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
