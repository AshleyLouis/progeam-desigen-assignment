import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Map;

public class ClientHandler extends Thread {
    private final Socket socket;
    private final Color color;
    private Game game;
    private final BufferedReader reader;
    private final PrintWriter writer;
    private ClientHandler opponent;
    private volatile boolean restartRequested = false;
    private volatile boolean running = true;

    public ClientHandler(Socket socket, Color color, Game game) throws IOException {
        super("client-" + color.protocolName());
        this.socket = socket;
        this.color = color;
        this.game = game;
        this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
        this.writer = new PrintWriter(socket.getOutputStream(), true);
    }

    public void setOpponent(ClientHandler opponent) {
        this.opponent = opponent;
    }

    public void sendGameStart() {
        String boardArray = game.boardArrayFor(color);
        send(JsonUtil.object("messageType", "gameStart",
                "yourColor", color.protocolName(),
                "firstHand", color == Color.RED,
                "redPlayerId", "red",
                "blackPlayerId", "black",
                "board", boardArray,
                "initialBoard", game.boardJsonFor(color)));
        if (color == Color.RED) {
            send(JsonUtil.object("messageType", "yourTurn", "turnStartTime", game.getTurnStartTime()));
        }
    }

    @Override
    public void run() {
        try {
            String line;
            while (running && (line = reader.readLine()) != null) {
                handle(line);
            }
        } catch (Exception ex) {
            running = false;
            try {
                send(JsonUtil.object("messageType", "error", "code", 4001, "message", ex.getMessage()));
            } catch (Exception ignored) {}
        } finally {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    private void handle(String line) {
        Map<String, String> json = JsonUtil.parseObject(line);
        String type = json.get("messageType");
        if ("Login".equalsIgnoreCase(type) || "register".equalsIgnoreCase(type)) {
            send(JsonUtil.object("messageType", "loginResult",
                    "success", true,
                    "message", "guest mode",
                    "userId", color.protocolName()));
            return;
        }
        if ("startMatch".equalsIgnoreCase(type) || "Ready".equalsIgnoreCase(type)
                || "requestFirstHand".equalsIgnoreCase(type) || "cancelMatch".equalsIgnoreCase(type)) {
            send(JsonUtil.object("messageType", "roomInfo",
                    "opponentReady", opponent != null,
                    "message", "matched by socket order"));
            return;
        }
        if ("ping".equalsIgnoreCase(type)) {
            send(JsonUtil.object("messageType", "pong", "timestamp", json.get("timestamp")));
            return;
        }
        if ("Resign".equalsIgnoreCase(type)) {
            System.out.println("\n========== " + color.protocolName().toUpperCase() + " 认输 ==========");
            System.out.println("胜者: " + color.opposite().protocolName().toUpperCase());
            System.out.println("====================================");
            String msg = JsonUtil.object("messageType", "gameOver",
                    "winner", color.opposite().protocolName(),
                    "winnerId", color.opposite().protocolName(),
                    "reason", "resign");
            game.setOver(color.opposite(), "resign");
            String recordFile = game.saveRecordToFile("records");
            if (recordFile != null) {
                System.out.println("棋谱已保存: " + recordFile);
            }
            send(msg);
            opponent.send(msg);
            return;
        }
        if ("restart".equalsIgnoreCase(type)) {
            if (!game.isOver()) {
                send(JsonUtil.object("messageType", "error", "code", 4002, "message", "game not over"));
                return;
            }
            restartRequested = true;
            send(JsonUtil.object("messageType", "restartRequested", "from", color.protocolName()));
            opponent.send(JsonUtil.object("messageType", "restartRequested", "from", color.protocolName()));
            System.out.println(color.protocolName().toUpperCase() + " 请求重新开始");

            if (opponent.restartRequested) {
                System.out.println("双方同意，重新开始游戏");
                restartRequested = false;
                opponent.restartRequested = false;
                game.reset(null);
                send(JsonUtil.object("messageType", "restartAccepted"));
                opponent.send(JsonUtil.object("messageType", "restartAccepted"));
                sendGameStart();
                opponent.sendGameStart();
            }
            return;
        }
        if ("restartCancel".equalsIgnoreCase(type)) {
            restartRequested = false;
            send(JsonUtil.object("messageType", "restartCancelled", "from", color.protocolName()));
            opponent.send(JsonUtil.object("messageType", "restartCancelled", "from", color.protocolName()));
            System.out.println(color.protocolName().toUpperCase() + " 取消重新开始");
            return;
        }
        if (!game.isOver() && !"move".equalsIgnoreCase(type)) {
            send(JsonUtil.object("messageType", "error", "code", 4001, "message", "unknown messageType"));
            return;
        }

        Move move = Move.fromJson(json);
        MoveResult result = game.applyMove(color, move);
        if (!result.isSuccess() && Move.hasOnlyPairCoordinates(json)) {
            Move alternative = Move.fromTopDownJson(json);
            MoveResult alternativeResult = game.applyMove(color, alternative);
            if (alternativeResult.isSuccess()) {
                move = alternative;
                result = alternativeResult;
            }
        }
        if (!result.isSuccess()) {
            send(JsonUtil.object("messageType", "moveResult",
                    "success", false,
                    "valid", false,
                    "message", result.getMessage()));
            return;
        }

        String moveJson = ProtocolUtil.moveJson(move);
        String response = JsonUtil.object("messageType", "moveResult",
                "success", true,
                "valid", true,
                "move", moveJson,
                "source", move.getSource().toText(),
                "destination", move.getDestination().toText(),
                "fromX", String.valueOf((char) ('a' + move.getSource().getX())),
                "fromY", move.getSource().getY(),
                "toX", String.valueOf((char) ('a' + move.getDestination().getX())),
                "toY", move.getDestination().getY(),
                "isFlip", move.isFlip(),
                "flipResult", ProtocolUtil.pieceTypeName(result.getFlipResult()),
                "captured", capturedText(result.getCaptured(), true),
                "board", game.boardJsonFor(color),
                "boardArray", game.boardArrayFor(color));
        send(response);
        opponent.send(responseForOpponent(result, move));

        String stepDesc = color.protocolName().toUpperCase() + " ";
        if (move.isFlip() && move.getSource().equals(move.getDestination())) {
            stepDesc += "翻子 " + move.getSource().toText();
        } else {
            stepDesc += "走子 " + move.getSource().toText() + " → " + move.getDestination().toText();
        }
        if (result.getFlipResult() != null) {
            stepDesc += " (翻出 " + result.getFlipResult().getProtocolName() + ")";
        }
        if (result.getCaptured() != null) {
            Piece cap = result.getCaptured();
            stepDesc += " 吃掉 " + (cap.isVisible() ? cap.getRealType().getProtocolName() : "暗子");
        }
        System.out.println("\n--- 第 " + game.getMoveCount() + " 步: " + stepDesc + " ---");
        game.printBoard();

        if (result.isGameOver()) {
            System.out.println("\n========== 游戏结束 ==========");
            System.out.println("胜者: " + (result.getWinner() == null ? "和棋" : result.getWinner().protocolName().toUpperCase()));
            System.out.println("原因: " + result.getReason());
            System.out.println("================================");
            String recordFile = game.saveRecordToFile("records");
            if (recordFile != null) {
                System.out.println("棋谱已保存: " + recordFile);
            }
            String gameOver = JsonUtil.object("messageType", "gameOver",
                    "winner", result.getWinner() == null ? "draw" : result.getWinner().protocolName(),
                    "winnerId", result.getWinner() == null ? "draw" : result.getWinner().protocolName(),
                    "reason", result.getReason());
            send(gameOver);
            opponent.send(gameOver);
        } else {
            opponent.send(JsonUtil.object("messageType", "yourTurn", "turnStartTime", game.getTurnStartTime()));
        }
    }

    private String responseForOpponent(MoveResult result, Move move) {
        String moveJson = ProtocolUtil.moveJson(move);
        return JsonUtil.object("messageType", "moveResult",
                "success", true,
                "valid", true,
                "move", moveJson,
                "source", move.getSource().toText(),
                "destination", move.getDestination().toText(),
                "fromX", String.valueOf((char) ('a' + move.getSource().getX())),
                "fromY", move.getSource().getY(),
                "toX", String.valueOf((char) ('a' + move.getDestination().getX())),
                "toY", move.getDestination().getY(),
                "isFlip", move.isFlip(),
                "flipResult", ProtocolUtil.pieceTypeName(result.getFlipResult()),
                "captured", capturedText(result.getCaptured(), false),
                "board", game.boardJsonFor(opponent.color),
                "boardArray", game.boardArrayFor(opponent.color));
    }

    private String capturedText(Piece piece, boolean isEater) {
        if (piece == null) {
            return "";
        }
        if (isEater) {
            return piece.getRealType().getProtocolName();
        }
        return piece.isVisible() ? piece.getRealType().getProtocolName() : "hidden";
    }

    public synchronized void send(String message) {
        writer.println(message);
    }

    public boolean isRunning() {
        return running;
    }

    public void close() {
        try {
            socket.close();
        } catch (Exception ignored) {
        }
    }
}
