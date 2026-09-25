import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.io.*;
import java.net.*;

public class JieQiGUI extends JFrame {
    private static final int CELL_SIZE = 58;
    private static final int BOARD_MARGIN = 30;

    private BoardPanel boardPanel;
    private JTextArea logArea;
    private JLabel statusLabel;
    private JButton connectBtn, resignBtn, restartBtn;
    private JTextField hostField, portField;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private boolean connected = false;
    private boolean myTurn = false;
    private String myColor = null;

    private PieceInfo[][] board = new PieceInfo[10][9];
    private Position selectedPos = null;

    static class PieceInfo {
        String color;
        String pieceType;
        boolean visible;
        PieceInfo(String color, String pieceType, boolean visible) {
            this.color = color;
            this.pieceType = pieceType;
            this.visible = visible;
        }
    }

    static class Position {
        int x, y;
        Position(int x, int y) { this.x = x; this.y = y; }
    }

    public JieQiGUI() {
        setTitle("JieQi - Chinese Dark Chess");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        initBoard();
        initUI();
        pack();
        setLocationRelativeTo(null);
    }

    public JieQiGUI(String host, String port) {
        this();
        if (host != null && host.length() > 0) {
            hostField.setText(host);
        }
        if (port != null && port.length() > 0) {
            portField.setText(port);
        }
    }

    private void initBoard() {
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 9; j++) {
                board[i][j] = null;
            }
        }
    }

    private void initUI() {
        setLayout(new BorderLayout());

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        topPanel.add(new JLabel("Server:"));
        hostField = new JTextField("localhost", 10);
        topPanel.add(hostField);
        topPanel.add(new JLabel("Port:"));
        portField = new JTextField("8887", 5);
        topPanel.add(portField);
        connectBtn = new JButton("Connect");
        connectBtn.addActionListener(e -> connect());
        topPanel.add(connectBtn);
        resignBtn = new JButton("Resign");
        resignBtn.setEnabled(false);
        resignBtn.addActionListener(e -> resign());
        topPanel.add(resignBtn);
        restartBtn = new JButton("Restart");
        restartBtn.setEnabled(false);
        restartBtn.addActionListener(e -> requestRestart());
        topPanel.add(restartBtn);
        statusLabel = new JLabel("Not connected");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 0));
        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        topPanel.add(statusLabel);
        add(topPanel, BorderLayout.NORTH);

        boardPanel = new BoardPanel();
        add(boardPanel, BorderLayout.CENTER);

        logArea = new JTextArea(10, 45);
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Game Log"));
        add(scrollPane, BorderLayout.SOUTH);
    }

    private void connect() {
        if (connected) {
            disconnect();
            return;
        }
        String host = hostField.getText().trim();
        int port;
        try {
            port = Integer.parseInt(portField.getText().trim());
        } catch (NumberFormatException ex) {
            appendLog("Invalid port number");
            return;
        }

        try {
            appendLog("Connecting to " + host + ":" + port + "...");
            socket = new Socket(host, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));

            connected = true;
            connectBtn.setText("Disconnect");
            statusLabel.setText("Connected, waiting for color...");
            appendLog("Connected. Waiting for server assignment...");

            new Thread(this::readMessages).start();
        } catch (Exception ex) {
            appendLog("Connection failed: " + ex.getMessage());
            statusLabel.setText("Connection failed");
        }
    }

    private void disconnect() {
        try {
            if (socket != null) socket.close();
        } catch (IOException e) {}
        connected = false;
        myTurn = false;
        myColor = null;
        connectBtn.setText("Connect");
        statusLabel.setText("Not connected");
        resignBtn.setEnabled(false);
        selectedPos = null;
        initBoard();
        boardPanel.repaint();
        appendLog("Disconnected");
    }

    private void resign() {
        if (connected && out != null && JOptionPane.showConfirmDialog(this,
                "Are you sure you want to resign?", "Confirm Resign",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            out.println("{\"messageType\":\"Resign\"}");
            appendLog("Resignation sent");
        }
    }

    private void readMessages() {
        try {
            String msg;
            while ((msg = in.readLine()) != null) {
                final String message = msg;
                SwingUtilities.invokeLater(() -> handleMessage(message));
            }
        } catch (IOException e) {
            SwingUtilities.invokeLater(() -> {
                if (connected) {
                    appendLog("Connection lost: " + e.getMessage());
                    disconnect();
                }
            });
        }
    }

    private void handleMessage(String msg) {
        if (msg.contains("\"messageType\":\"gameStart\"")) {
            parseGameStart(msg);
        } else if (msg.contains("\"messageType\":\"yourTurn\"")) {
            myTurn = true;
            updateStatus();
            appendLog("Your turn");
        } else if (msg.contains("\"messageType\":\"moveResult\"")) {
            parseMoveResult(msg);
        } else if (msg.contains("\"messageType\":\"gameOver\"")) {
            parseGameOver(msg);
        } else if (msg.contains("\"messageType\":\"restartRequested\"")) {
            String from = extractString(msg, "from");
            appendLog(from + " requested restart");
            restartBtn.setEnabled(true);
        } else if (msg.contains("\"messageType\":\"restartCancelled\"")) {
            String from = extractString(msg, "from");
            appendLog(from + " cancelled restart");
            restartBtn.setText("Restart");
        } else if (msg.contains("\"messageType\":\"restartAccepted\"")) {
            appendLog("Restart accepted! New game starting...");
            restartBtn.setText("Restart");
            restartBtn.setEnabled(false);
            resignBtn.setEnabled(true);
        } else if (msg.contains("\"messageType\":\"waiting\"")) {
            String waitMsg = extractString(msg, "message");
            appendLog(waitMsg != null ? waitMsg : "Waiting for opponent...");
            updateStatus();
        } else if (msg.contains("\"messageType\":\"error\"")) {
            appendLog("Error: " + msg);
        }
    }

    private void parseGameStart(String msg) {
        String colorStr = extractString(msg, "yourColor");
        if ("red".equals(colorStr)) {
            myColor = "red";
            myTurn = true;
            appendLog("You are RED (first to move)");
        } else {
            myColor = "black";
            myTurn = false;
            appendLog("You are BLACK (wait for red to move)");
        }
        resignBtn.setEnabled(true);
        restartBtn.setEnabled(false);
        restartBtn.setText("Restart");
        parseBoardFromMessage(msg);
        updateStatus();
    }

    private void parseMoveResult(String msg) {
        String success = extractString(msg, "success");
        if (!"true".equals(success)) {
            String message = extractString(msg, "message");
            appendLog("Invalid move: " + message);
            myTurn = true;
            updateStatus();
            return;
        }
        parseBoardFromMessage(msg);

        String src = firstNonEmpty(extractNestedString(msg, "move", "source"),
                extractString(msg, "source"), pairPosition(msg, "fromX", "fromY"));
        String dst = firstNonEmpty(extractNestedString(msg, "move", "destination"),
                extractString(msg, "destination"), pairPosition(msg, "toX", "toY"));
        String isFlip = firstNonEmpty(extractNestedString(msg, "move", "isFlip"),
                extractString(msg, "isFlip"));
        String flipResult = extractString(msg, "flipResult");
        String captured = extractString(msg, "captured");

        StringBuilder sb = new StringBuilder();
        if ("true".equals(isFlip)) {
            sb.append("Flip ").append(src);
        } else {
            sb.append("Move ").append(src).append(" -> ").append(dst);
        }
        if (flipResult != null && flipResult.length() > 0) {
            sb.append(" (revealed: ").append(flipResult).append(")");
        }
        if (captured != null && captured.length() > 0) {
            sb.append(" captured: ").append(captured);
        }
        appendLog(sb.toString());
    }

    private void parseGameOver(String msg) {
        String winner = extractString(msg, "winner");
        String reason = extractString(msg, "reason");

        StringBuilder sb = new StringBuilder("Game Over - ");
        if ("draw".equals(winner)) {
            sb.append("Draw");
        } else if ("red".equals(winner)) {
            sb.append("Red wins");
        } else {
            sb.append("Black wins");
        }
        sb.append(" (").append(reason).append(")");
        appendLog(sb.toString());

        JOptionPane.showMessageDialog(this, sb.toString(), "Game Over",
                JOptionPane.INFORMATION_MESSAGE);

        myTurn = false;
        resignBtn.setEnabled(false);
        restartBtn.setEnabled(true);
        updateStatus();
    }

    private void requestRestart() {
        if (restartBtn.getText().equals("Cancel Restart")) {
            out.println("{\"messageType\":\"restartCancel\"}");
            appendLog("Cancelled restart request");
            restartBtn.setText("Restart");
            return;
        }
        out.println("{\"messageType\":\"restart\"}");
        appendLog("Restart request sent, waiting for opponent...");
        restartBtn.setText("Cancel Restart");
    }

    private String extractString(String msg, String key) {
        String pattern = "\"" + key + "\":";
        int idx = msg.indexOf(pattern);
        if (idx == -1) return null;
        int start = idx + pattern.length();
        if (start >= msg.length()) return null;
        if (msg.charAt(start) == '"') {
            int end = msg.indexOf('"', start + 1);
            if (end == -1) return null;
            return msg.substring(start + 1, end);
        } else {
            int end = start;
            while (end < msg.length() && msg.charAt(end) != ',' && msg.charAt(end) != '}') {
                end++;
            }
            return msg.substring(start, end);
        }
    }

    private String extractNestedString(String msg, String outerKey, String innerKey) {
        String outerPattern = "\"" + outerKey + "\":";
        int outerIdx = msg.indexOf(outerPattern);
        if (outerIdx == -1) return null;
        int objStart = msg.indexOf('{', outerIdx);
        int objEnd = findMatchingBrace(msg, objStart);
        if (objEnd == -1) return null;
        String inner = msg.substring(objStart, objEnd + 1);
        return extractString(inner, innerKey);
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
        String x = extractString(msg, xKey);
        String y = extractString(msg, yKey);
        if (x == null || y == null) {
            return null;
        }
        return x + y;
    }

    private int findMatchingBrace(String str, int start) {
        int count = 0;
        for (int i = start; i < str.length(); i++) {
            if (str.charAt(i) == '{') count++;
            else if (str.charAt(i) == '}') {
                count--;
                if (count == 0) return i;
            }
        }
        return -1;
    }

    private void parseBoardFromMessage(String msg) {
        String boardData = null;
        boolean isObjectArray = false;

        int initialIdx = msg.indexOf("\"initialBoard\":");
        if (initialIdx != -1) {
            int arrStart = msg.indexOf("[{\"x\":", initialIdx);
            if (arrStart != -1) {
                int arrEnd = findObjectArrayEnd(msg, arrStart);
                if (arrEnd != -1) {
                    boardData = msg.substring(arrStart, arrEnd + 1);
                    isObjectArray = true;
                }
            }
        }

        if (boardData == null) {
            int boardIdx = msg.indexOf("\"board\":");
            if (boardIdx == -1) return;

            int objArrStart = msg.indexOf("[{\"x\":", boardIdx);
            int arr2dStart = msg.indexOf("[[", boardIdx);

            if (objArrStart != -1 && (arr2dStart == -1 || objArrStart < arr2dStart)) {
                int arrEnd = findObjectArrayEnd(msg, objArrStart);
                if (arrEnd != -1) {
                    boardData = msg.substring(objArrStart, arrEnd + 1);
                    isObjectArray = true;
                }
            } else if (arr2dStart != -1) {
                int arrEnd = msg.indexOf("]]", arr2dStart);
                if (arrEnd != -1) {
                    boardData = msg.substring(arr2dStart, arrEnd + 2);
                    isObjectArray = false;
                }
            }
        }

        if (boardData == null) return;

        initBoard();

        if (isObjectArray) {
            parseObjectArrayBoard(boardData);
        } else {
            parse2DArrayBoard(boardData);
        }

        boardPanel.repaint();
    }

    private int findObjectArrayEnd(String msg, int start) {
        int depth = 0;
        boolean inQuote = false;
        for (int i = start; i < msg.length(); i++) {
            char c = msg.charAt(i);
            if (c == '"') {
                inQuote = !inQuote;
            } else if (!inQuote) {
                if (c == '{' || c == '[') depth++;
                else if (c == '}' || c == ']') {
                    depth--;
                    if (depth == 0) return i;
                }
            }
        }
        return -1;
    }

    private void parseObjectArrayBoard(String data) {
        int i = 0;
        while (i < data.length()) {
            int objStart = data.indexOf('{', i);
            if (objStart == -1) break;
            int objEnd = data.indexOf('}', objStart);
            if (objEnd == -1) break;

            String obj = data.substring(objStart, objEnd + 1);
            String xStr = extractString(obj, "x");
            String yStr = extractString(obj, "y");
            String color = extractString(obj, "color");
            String piece = extractString(obj, "piece");
            String visible = extractString(obj, "visible");

            if (xStr != null && yStr != null && piece != null && !"empty".equals(piece)) {
                int x = xStr.charAt(0) - 'a';
                int y = Integer.parseInt(yStr);
                boolean vis = "true".equals(visible);
                if (x >= 0 && x < 9 && y >= 0 && y < 10) {
                    board[y][x] = new PieceInfo(color, piece, vis);
                }
            }

            i = objEnd + 1;
        }
    }

    private void parse2DArrayBoard(String data) {
        String inner = data.substring(2, data.length() - 2);

        java.util.List<String> rows = new java.util.ArrayList<String>();
        int searchFrom = 0;
        int rowStart = 0;
        while (true) {
            int idx = inner.indexOf("],[", searchFrom);
            if (idx == -1) {
                rows.add(inner.substring(rowStart));
                break;
            }
            rows.add(inner.substring(rowStart, idx));
            rowStart = idx + 3;
            searchFrom = idx + 3;
        }

        for (int i = 0; i < rows.size() && i < 10; i++) {
            String row = rows.get(i);
            String[] cells = splitRow(row);
            for (int j = 0; j < cells.length && j < 9; j++) {
                String cell = cells[j].trim();
                if ("null".equals(cell) || cell.isEmpty()) {
                    board[i][j] = null;
                } else {
                    cell = cell.replace("\"", "");
                    board[i][j] = parsePiece(cell);
                }
            }
        }
    }

    private String[] splitRow(String row) {
        java.util.List<String> cells = new java.util.ArrayList<String>();
        int start = 0;
        boolean inQuote = false;
        for (int i = 0; i < row.length(); i++) {
            char c = row.charAt(i);
            if (c == '"') {
                inQuote = !inQuote;
            } else if (c == ',' && !inQuote) {
                cells.add(row.substring(start, i));
                start = i + 1;
            }
        }
        cells.add(row.substring(start));
        return cells.toArray(new String[0]);
    }

    private PieceInfo parsePiece(String s) {
        if (s == null || s.length() < 2) return null;
        String color;
        String type;
        if (s.startsWith("r") || s.startsWith("R")) {
            color = "red";
            type = s.substring(1);
        } else if (s.startsWith("b") || s.startsWith("B")) {
            color = "black";
            type = s.substring(1);
        } else {
            return null;
        }
        return new PieceInfo(color, type, false);
    }

    private String[] splitRows(String boardStr) {
        java.util.List<String> rows = new java.util.ArrayList<String>();
        int depth = 0;
        int start = 0;
        for (int i = 0; i < boardStr.length(); i++) {
            char c = boardStr.charAt(i);
            if (c == '[') depth++;
            else if (c == ']') depth--;
            else if (c == ',' && depth == 0) {
                rows.add(boardStr.substring(start, i));
                start = i + 1;
            }
        }
        if (start < boardStr.length()) {
            rows.add(boardStr.substring(start));
        }
        return rows.toArray(new String[0]);
    }

    private int findMatchingBracket(String str, int start) {
        int count = 0;
        for (int i = start; i < str.length(); i++) {
            if (str.charAt(i) == '[') count++;
            else if (str.charAt(i) == ']') {
                count--;
                if (count == 0) return i;
            }
        }
        return -1;
    }

    private void updateStatus() {
        if (myColor == null) {
            statusLabel.setText("Not connected");
            statusLabel.setForeground(java.awt.Color.GRAY);
        } else if (!connected) {
            statusLabel.setText("Disconnected");
            statusLabel.setForeground(java.awt.Color.RED);
        } else if (myTurn) {
            statusLabel.setText("Your turn (" + myColor + ")");
            statusLabel.setForeground(new java.awt.Color(0, 150, 0));
        } else {
            statusLabel.setText("Opponent's turn (" + myColor + ")");
            statusLabel.setForeground(java.awt.Color.ORANGE);
        }
    }

    private void appendLog(String text) {
        logArea.append(text + "\n");
        logArea.setCaretPosition(logArea.getText().length());
    }

    private void sendMove(int fromX, int fromY, int toX, int toY, boolean flip) {
        if (!connected || !myTurn) return;

        // 转换成外部坐标格式 (a-i列, 9-0行): 内部y -> 外部(9-y)
        String src = String.valueOf((char)('a' + fromX)) + (9 - fromY);
        String dst = String.valueOf((char)('a' + toX)) + (9 - toY);

        String move = JsonUtil.object("messageType", "move",
                "source", src,
                "destination", dst,
                "fromX", String.valueOf((char) ('a' + fromX)),
                "fromY", 9 - fromY,
                "toX", String.valueOf((char) ('a' + toX)),
                "toY", 9 - toY,
                "isFlip", flip,
                "turnStartTime", System.currentTimeMillis());
        out.println(move);
        myTurn = false;
        updateStatus();
        appendLog("Sent: " + src + " -> " + dst + (flip ? " (flip)" : ""));
    }

    private int viewX(int boardX) {
        if ("black".equals(myColor)) {
            return 8 - boardX;
        }
        return boardX;
    }

    private int viewY(int boardY) {
        if ("black".equals(myColor)) {
            return 9 - boardY;
        }
        return boardY;
    }

    private int boardX(int viewX) {
        if ("black".equals(myColor)) {
            return 8 - viewX;
        }
        return viewX;
    }

    private int boardY(int viewY) {
        if ("black".equals(myColor)) {
            return 9 - viewY;
        }
        return viewY;
    }

    private class BoardPanel extends JPanel {
        public BoardPanel() {
            int w = CELL_SIZE * 8 + BOARD_MARGIN * 2;
            int h = CELL_SIZE * 9 + BOARD_MARGIN * 2;
            setPreferredSize(new Dimension(w, h));
            setBackground(new java.awt.Color(240, 220, 180));

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (!myTurn || myColor == null) return;

                    int vx = (e.getX() - BOARD_MARGIN + CELL_SIZE / 2) / CELL_SIZE;
                    int vy = (e.getY() - BOARD_MARGIN + CELL_SIZE / 2) / CELL_SIZE;

                    if (vx < 0 || vx > 8 || vy < 0 || vy > 9) return;

                    int bx = boardX(vx);
                    int by = boardY(vy);

                    PieceInfo clicked = board[by][bx];

                    if (selectedPos == null) {
                        if (clicked != null && myColor.equals(clicked.color)) {
                            selectedPos = new Position(bx, by);
                            repaint();
                        }
                    } else {
                        if (bx == selectedPos.x && by == selectedPos.y) {
                            PieceInfo sel = board[selectedPos.y][selectedPos.x];
                            if (sel != null && !sel.visible) {
                                sendMove(selectedPos.x, selectedPos.y, bx, by, true);
                            }
                            selectedPos = null;
                            repaint();
                            return;
                        }

                        if (clicked != null && myColor.equals(clicked.color)) {
                            selectedPos = new Position(bx, by);
                            repaint();
                            return;
                        }

                        sendMove(selectedPos.x, selectedPos.y, bx, by, false);
                        selectedPos = null;
                        repaint();
                    }
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(new java.awt.Color(240, 220, 180));
            g2.fillRect(0, 0, getWidth(), getHeight());

            g2.setColor(new java.awt.Color(120, 80, 40));
            g2.setStroke(new BasicStroke(3));
            g2.drawRect(BOARD_MARGIN - 5, BOARD_MARGIN - 5,
                CELL_SIZE * 8 + 10, CELL_SIZE * 9 + 10);

            g2.setColor(java.awt.Color.BLACK);
            g2.setStroke(new BasicStroke(2));

            for (int i = 0; i < 10; i++) {
                int y = BOARD_MARGIN + i * CELL_SIZE;
                g2.drawLine(BOARD_MARGIN, y, BOARD_MARGIN + 8 * CELL_SIZE, y);
            }
            for (int i = 0; i < 9; i++) {
                int x = BOARD_MARGIN + i * CELL_SIZE;
                if (i == 0 || i == 8) {
                    g2.drawLine(x, BOARD_MARGIN, x, BOARD_MARGIN + 9 * CELL_SIZE);
                } else {
                    g2.drawLine(x, BOARD_MARGIN, x, BOARD_MARGIN + 4 * CELL_SIZE);
                    g2.drawLine(x, BOARD_MARGIN + 5 * CELL_SIZE, x, BOARD_MARGIN + 9 * CELL_SIZE);
                }
            }

            g2.drawLine(BOARD_MARGIN + 3 * CELL_SIZE, BOARD_MARGIN,
                BOARD_MARGIN + 5 * CELL_SIZE, BOARD_MARGIN + 2 * CELL_SIZE);
            g2.drawLine(BOARD_MARGIN + 5 * CELL_SIZE, BOARD_MARGIN,
                BOARD_MARGIN + 3 * CELL_SIZE, BOARD_MARGIN + 2 * CELL_SIZE);
            g2.drawLine(BOARD_MARGIN + 3 * CELL_SIZE, BOARD_MARGIN + 7 * CELL_SIZE,
                BOARD_MARGIN + 5 * CELL_SIZE, BOARD_MARGIN + 9 * CELL_SIZE);
            g2.drawLine(BOARD_MARGIN + 5 * CELL_SIZE, BOARD_MARGIN + 7 * CELL_SIZE,
                BOARD_MARGIN + 3 * CELL_SIZE, BOARD_MARGIN + 9 * CELL_SIZE);

            g2.setColor(new java.awt.Color(180, 140, 80));
            g2.setFont(new Font("Serif", Font.BOLD, 14));
            for (int i = 0; i < 9; i++) {
                int bx = "black".equals(myColor) ? 8 - i : i;
                String label = String.valueOf((char)('a' + bx));
                int x = BOARD_MARGIN + i * CELL_SIZE;
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(label, x - fm.stringWidth(label) / 2, BOARD_MARGIN - 10);
                g2.drawString(label, x - fm.stringWidth(label) / 2,
                    BOARD_MARGIN + 9 * CELL_SIZE + fm.getAscent() + 5);
            }
            for (int i = 0; i < 10; i++) {
                int by = "black".equals(myColor) ? 9 - i : i;
                String label = String.valueOf(by);
                int y = BOARD_MARGIN + i * CELL_SIZE;
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(label, 8, y + fm.getAscent() / 2 - 2);
                g2.drawString(label, BOARD_MARGIN + 8 * CELL_SIZE + 8,
                    y + fm.getAscent() / 2 - 2);
            }

            for (int i = 0; i < 10; i++) {
                for (int j = 0; j < 9; j++) {
                    if (board[i][j] != null) {
                        int vx = viewX(j);
                        int vy = viewY(i);
                        drawPiece(g2, vx, vy, board[i][j]);
                    }
                }
            }

            if (selectedPos != null) {
                int vx = viewX(selectedPos.x);
                int vy = viewY(selectedPos.y);
                int sx = BOARD_MARGIN + vx * CELL_SIZE;
                int sy = BOARD_MARGIN + vy * CELL_SIZE;
                g2.setColor(new java.awt.Color(0, 200, 0, 80));
                int r = CELL_SIZE / 2 - 3;
                g2.fillOval(sx - r, sy - r, r * 2, r * 2);
                g2.setColor(java.awt.Color.GREEN);
                g2.setStroke(new BasicStroke(3));
                g2.drawOval(sx - r, sy - r, r * 2, r * 2);
            }
        }

        private void drawPiece(Graphics2D g2, int col, int row, PieceInfo piece) {
            int cx = BOARD_MARGIN + col * CELL_SIZE;
            int cy = BOARD_MARGIN + row * CELL_SIZE;
            int r = CELL_SIZE / 2 - 4;

            g2.setColor(new java.awt.Color(180, 140, 80));
            g2.fillOval(cx - r + 2, cy - r + 2, r * 2, r * 2);

            if (!piece.visible) {
                java.awt.Color bgColor = new java.awt.Color(139, 90, 43);
                g2.setColor(bgColor);
                g2.fillOval(cx - r, cy - r, r * 2, r * 2);

                g2.setColor(new java.awt.Color(200, 160, 100));
                g2.setStroke(new BasicStroke(2));
                g2.drawOval(cx - r + 1, cy - r + 1, r * 2 - 2, r * 2 - 2);
                g2.drawOval(cx - r + 4, cy - r + 4, r * 2 - 8, r * 2 - 8);

                g2.setColor(new java.awt.Color(210, 170, 110));
                g2.setFont(new Font("SimHei", Font.BOLD, 20));
                FontMetrics fm = g2.getFontMetrics();
                String backChar = "揭";
                int textX = cx - fm.stringWidth(backChar) / 2;
                int textY = cy + fm.getAscent() / 2 - 3;
                g2.drawString(backChar, textX, textY);
                return;
            }

            boolean isRed = "red".equals(piece.color);

            java.awt.Color bgColor = isRed ? new java.awt.Color(255, 245, 230) : new java.awt.Color(70, 70, 75);
            g2.setColor(bgColor);
            g2.fillOval(cx - r, cy - r, r * 2, r * 2);

            g2.setColor(isRed ? new java.awt.Color(200, 30, 30) : new java.awt.Color(230, 230, 230));
            g2.setStroke(new BasicStroke(2));
            g2.drawOval(cx - r + 1, cy - r + 1, r * 2 - 2, r * 2 - 2);
            g2.drawOval(cx - r + 4, cy - r + 4, r * 2 - 8, r * 2 - 8);

            String display = getDisplayChar(piece);
            g2.setFont(new Font("SimHei", Font.BOLD, 22));
            FontMetrics fm = g2.getFontMetrics();
            int textX = cx - fm.stringWidth(display) / 2;
            int textY = cy + fm.getAscent() / 2 - 3;

            g2.setColor(isRed ? new java.awt.Color(200, 0, 0) : new java.awt.Color(240, 240, 240));
            g2.drawString(display, textX, textY);
        }

        private String getDisplayChar(PieceInfo piece) {
            String type = piece.pieceType;
            boolean isRed = "red".equals(piece.color);

            if (type == null || type.isEmpty()) return "?";

            String t = type.toLowerCase();
            if (isRed) {
                if (t.equals("king")) return "帅";
                if (t.equals("rook")) return "车";
                if (t.equals("knight")) return "马";
                if (t.equals("cannon")) return "炮";
                if (t.equals("pawn")) return "兵";
                if (t.equals("guard")) return "士";
                if (t.equals("bishop")) return "相";
                return "?";
            } else {
                if (t.equals("king")) return "将";
                if (t.equals("rook")) return "車";
                if (t.equals("knight")) return "馬";
                if (t.equals("cannon")) return "砲";
                if (t.equals("pawn")) return "卒";
                if (t.equals("guard")) return "仕";
                if (t.equals("bishop")) return "象";
                return "?";
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
            String host = args.length > 0 ? args[0] : "localhost";
            String port = args.length > 1 ? args[1] : "8887";
            boolean autoConnect = args.length > 2 && "connect".equalsIgnoreCase(args[2]);
            JieQiGUI gui = new JieQiGUI(host, port);
            gui.setVisible(true);
            if (autoConnect) {
                gui.connect();
            }
        });
    }
}
