# 揭棋对弈程序大作业

## 项目说明

本项目实现大作业中的“设计一个揭棋对弈程序”方向：

- 两个客户端通过一个服务器真人对弈。
- 服务器与客户端通过 TCP Socket 通信。
- 每条消息使用 JSON 文本，一行一个 JSON。
- 服务器负责回合控制、随机翻子、走法校验、超时判断和棋谱记录。
- 客户端使用命令行界面，便于课堂验收。

项目已配置为 Eclipse Java Project，项目名：

```text
JieQiGame
```

## 主要类

- `GameServer`：服务器入口，监听端口，等待两个客户端。
- `ConsoleClient`：客户端入口，输入命令并发送 JSON。
- `Game`：一局棋的状态控制，管理回合、胜负、超时。
- `Board`：棋盘与棋子初始化、移动、棋谱记录。
- `MoveValidator`：揭棋/象棋走法校验。
- `Move`：走子对象，包含 source、destination、type、turnStartTime。
- `Piece`、`PieceType`、`Color`、`Position`：领域模型。
- `ClientHandler`：服务器端每个客户端连接的处理线程。
- `JsonUtil`：简易 JSON 生成与解析工具。

领域类超过 5 个，符合面向对象设计要求。

## Eclipse 导入

1. 打开 Eclipse。
2. 选择 `File -> Import...`。
3. 选择 `General -> Existing Projects into Workspace`。
4. `Select root directory` 选择本文件夹：

```text
C:\Users\朱鼎\Desktop\java作业\大作业-揭棋
```

5. 勾选 `JieQiGame`。
6. 点击 `Finish`。

## 运行方式

先运行服务器：

```text
GameServer
```

或者命令行：

```powershell
javac -encoding UTF-8 src\*.java
java -cp src GameServer
```

再打开两个客户端，分别运行：

```text
ConsoleClient
```

服务器会把第一个连接的客户端设为红方，第二个连接的客户端设为黑方。红方先走。

## 客户端命令

移动棋子：

```text
move b3 b4
```

原地翻子：

```text
flip b3
```

认输：

```text
resign
```

心跳：

```text
ping
```

## JSON 协议示例

客户端发送移动：

```json
{"messageType":"move","source":"b3","destination":"b4","isFlip":false,"turnStartTime":1712345678901}
```

为满足不同小组之间服务器/客户端互操作，程序同时兼容公共接口中的走子字段：

```json
{"messageType":"move","fromX":"b","fromY":3,"toX":"b","toY":4,"isFlip":false}
```

本组客户端发送走子时会同时带上 `source/destination` 和 `fromX/fromY/toX/toY`，本组服务器接收时也同时兼容这两种格式。

客户端发送原地翻子：

```json
{"messageType":"move","source":"b3","destination":"b3","isFlip":true,"turnStartTime":1712345678901}
```

服务器返回走子结果：

```json
{"messageType":"moveResult","success":true,"valid":true,"move":{"source":"b3","destination":"b4","isFlip":false},"flipResult":"Cannon","captured":"","board":[...]}
```

服务器返回 `moveResult` 时也会同时包含平铺字段，便于别组客户端解析：

```json
{
  "messageType": "moveResult",
  "success": true,
  "valid": true,
  "source": "b3",
  "destination": "b4",
  "fromX": "b",
  "fromY": 3,
  "toX": "b",
  "toY": 4,
  "isFlip": false,
  "flipResult": "Cannon"
}
```

非法走子时：

```json
{"messageType":"moveResult","success":false,"valid":false,"message":"illegal Rook move"}
```

## 已实现规则

- 棋盘坐标：横向 `a-i`，纵向 `0-9`。
- 红方在下，黑方在上，红方先走。
- 初始只有将/帅明子，其余棋子暗置。
- 暗子移动时按当前位置原始棋子的规则移动。
- 暗子移动后由服务器揭示真实棋子类型。
- 支持原地翻子，作为一个回合。
- 服务器禁止错误走法。
- 服务器禁止未轮到的一方走棋。
- 服务器每步限制 65 秒，包含 60 秒思考时间和 5 秒网络延迟余量。
- 40 回合无吃子判和，对应双方 80 个半回合。
- 吃掉明将/帅判胜。
- 士、象揭开后可离开九宫/过河。
- 保留蹩马腿、塞象眼、炮隔山、车直线无阻挡等规则。

## 简化说明

本程序重点展示课程要求中的面向对象设计、Socket 通信、JSON 交互、规则校验和服务器仲裁。以下内容做了简化：

- 客户端为命令行界面，没有美化 GUI。
- 暂未实现将军、将死、困毙、长将/长捉的完整自动判定。
- 当前服务器一次支持一盘对局，可扩展为多房间。

## 验收展示建议

1. 运行 `GameServer`。
2. 运行两个 `ConsoleClient`。
3. 红方输入：

```text
move a6 a5
```

4. 黑方输入：

```text
move a3 a4
```

5. 尝试非法走法，例如：

```text
move a5 c5
```

服务器会返回 `valid:false`。

6. 演示原地翻子：

```text
flip b7
```

7. 演示心跳：

```text
ping
```

## 跨组互操作说明

本项目已经针对公共接口做了兼容：

- 可接收 `source/destination` 格式。
- 可接收公共协议 `fromX/fromY/toX/toY` 格式。
- 对 `fromY/toY` 兼容两种常见坐标解释：课程坐标标号和数组行号。
- 可响应 `Login`、`register`、`startMatch`、`Ready`、`requestFirstHand`、`ping`、`Resign` 等消息。
- `gameStart` 同时发送 `initialBoard` 和 `board` 字段。
- `moveResult` 同时发送嵌套 `move`、平铺坐标字段、`board`、`boardArray`。

可运行下面的测试验证本组服务器能接收只包含公共字段的客户端走子：

```powershell
javac -encoding UTF-8 -d bin src\*.java
java -cp bin InteropProtocolTest
```

## AI 辅助说明

本项目按大作业要求使用 AI 辅助完成分析、设计、编码和测试。采用的过程：

1. 从题面和公共接口中提取需求。
2. 设计领域类：棋盘、棋子、走子、坐标、对局、规则校验。
3. 决定使用纯 Java TCP Socket 和 JSON 行协议，避免第三方依赖影响课堂验收。
4. 分模块实现：先领域模型，再规则校验，再服务器/客户端，再文档和测试。
5. 使用 `javac --release 8` 验证 Java 8 兼容性。

## 贡献者

- 角色A（仓库所有者/评审者）：朱鼎
- 角色B（贡献者）：欧阳晨

## 更新日志

- 2026-09-25：初始版本提交，包含完整的揭棋对弈程序实现
