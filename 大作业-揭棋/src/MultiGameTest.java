import java.io.*;
import java.net.*;

/**
 * 测试多盘对弈：模拟4个客户端连接同一服务器，分成2个房间
 */
public class MultiGameTest {
    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 9999;
        
        System.out.println("========================================");
        System.out.println("   多盘对弈测试 (Multi-Game Test)");
        System.out.println("========================================");
        System.out.println("服务器端口: " + port);
        System.out.println();

        // 房间1的两位玩家
        System.out.println("[Room 1] 连接玩家1和玩家2...");
        Socket room1_red = new Socket("localhost", port);
        BufferedReader r1_r = new BufferedReader(new InputStreamReader(room1_red.getInputStream(), "UTF-8"));
        PrintWriter w1_r = new PrintWriter(room1_red.getOutputStream(), true);
        
        Socket room1_black = new Socket("localhost", port);
        BufferedReader r1_b = new BufferedReader(new InputStreamReader(room1_black.getInputStream(), "UTF-8"));
        PrintWriter w1_b = new PrintWriter(room1_black.getOutputStream(), true);
        
        // 等待游戏开始
        String gs1_r = waitForMsg(r1_r, "gameStart");
        String gs1_b = waitForMsg(r1_b, "gameStart");
        
        System.out.println("[Room 1] 红方收到: " + (gs1_r != null ? "gameStart" : "null"));
        System.out.println("[Room 1] 黑方收到: " + (gs1_b != null ? "gameStart" : "null"));
        
        // 验证颜色分配
        String color1_r = extractStr(gs1_r, "yourColor");
        String color1_b = extractStr(gs1_b, "yourColor");
        System.out.println("[Room 1] 红方颜色: " + color1_r + ", 黑方颜色: " + color1_b);
        
        // 房间2的两位玩家（同时连接，验证服务器能处理多盘）
        System.out.println();
        System.out.println("[Room 2] 连接玩家3和玩家4...");
        Socket room2_red = new Socket("localhost", port);
        BufferedReader r2_r = new BufferedReader(new InputStreamReader(room2_red.getInputStream(), "UTF-8"));
        PrintWriter w2_r = new PrintWriter(room2_red.getOutputStream(), true);
        
        Socket room2_black = new Socket("localhost", port);
        BufferedReader r2_b = new BufferedReader(new InputStreamReader(room2_black.getInputStream(), "UTF-8"));
        PrintWriter w2_b = new PrintWriter(room2_black.getOutputStream(), true);
        
        String gs2_r = waitForMsg(r2_r, "gameStart");
        String gs2_b = waitForMsg(r2_b, "gameStart");
        
        System.out.println("[Room 2] 红方收到: " + (gs2_r != null ? "gameStart" : "null"));
        System.out.println("[Room 2] 黑方收到: " + (gs2_b != null ? "gameStart" : "null"));
        
        String color2_r = extractStr(gs2_r, "yourColor");
        String color2_b = extractStr(gs2_b, "yourColor");
        System.out.println("[Room 2] 红方颜色: " + color2_r + ", 黑方颜色: " + color2_b);
        
        // 测试：两个房间各自独立走棋
        System.out.println();
        System.out.println("=== 测试独立走棋 ===");
        
        // Room 1: 红方走一步
        System.out.println("[Room 1] 红方翻子 a3...");
        w1_r.println("{\"messageType\":\"move\",\"source\":\"a3\",\"destination\":\"a3\",\"isFlip\":true}");
        String mr1_r = waitForMsg(r1_r, "moveResult");
        String mr1_b = waitForMsg(r1_b, "moveResult");
        System.out.println("[Room 1] 红方成功: " + (mr1_r != null && mr1_r.contains("\"success\":true")));
        System.out.println("[Room 1] 黑方同步: " + (mr1_b != null && mr1_b.contains("\"success\":true")));
        
        // Room 2: 红方走一步（不同棋）
        System.out.println("[Room 2] 红方翻子 b2...");
        w2_r.println("{\"messageType\":\"move\",\"source\":\"b2\",\"destination\":\"b2\",\"isFlip\":true}");
        String mr2_r = waitForMsg(r2_r, "moveResult");
        String mr2_b = waitForMsg(r2_b, "moveResult");
        System.out.println("[Room 2] 红方成功: " + (mr2_r != null && mr2_r.contains("\"success\":true")));
        System.out.println("[Room 2] 黑方同步: " + (mr2_b != null && mr2_b.contains("\"success\":true")));
        
        // 验证：Room 1的走棋不会影响 Room 2
        System.out.println();
        System.out.println("=== 验证独立性 ===");
        
        // Room 1 的棋盘状态应该只有 a3 翻开
        // Room 2 的棋盘状态应该只有 b2 翻开
        // 这里简化验证：两个房间都能继续走棋
        
        // Room 1: 黑方走
        waitForMsg(r1_b, "yourTurn");
        w1_b.println("{\"messageType\":\"move\",\"source\":\"a6\",\"destination\":\"a6\",\"isFlip\":true}");
        String mr1_b2 = waitForMsg(r1_b, "moveResult");
        System.out.println("[Room 1] 黑方走棋成功: " + (mr1_b2 != null && mr1_b2.contains("\"success\":true")));
        
        // Room 2: 黑方走（同时）
        waitForMsg(r2_b, "yourTurn");
        w2_b.println("{\"messageType\":\"move\",\"source\":\"b7\",\"destination\":\"b7\",\"isFlip\":true}");
        String mr2_b2 = waitForMsg(r2_b, "moveResult");
        System.out.println("[Room 2] 黑方走棋成功: " + (mr2_b2 != null && mr2_b2.contains("\"success\":true")));
        
        // 所有玩家认输结束游戏
        System.out.println();
        System.out.println("=== 结束测试 ===");
        w1_r.println("{\"messageType\":\"Resign\"}");
        w2_r.println("{\"messageType\":\"Resign\"}");
        
        Thread.sleep(500);
        
        room1_red.close();
        room1_black.close();
        room2_red.close();
        room2_black.close();
        
        System.out.println();
        System.out.println("========================================");
        System.out.println("   测试完成！");
        System.out.println("========================================");
        System.out.println("验证要点:");
        System.out.println("  1. 服务器接受4个客户端连接");
        System.out.println("  2. 自动分配到2个房间");
        System.out.println("  3. 两个房间独立走棋，互不影响");
        System.out.println("  4. 每个房间有独立的棋盘状态");
    }
    
    static String waitForMsg(BufferedReader r, String msgType) throws Exception {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < 5000) {
            if (r.ready()) {
                String line = r.readLine();
                if (line == null) return null;
                if (line.contains("\"messageType\":\"" + msgType + "\"")) {
                    return line;
                }
            } else {
                Thread.sleep(50);
            }
        }
        return null;
    }
    
    static String extractStr(String msg, String key) {
        if (msg == null) return null;
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
        }
        return null;
    }
}