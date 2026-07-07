# Java Socket 网上聊天程序

本项目是一个基于 Java 原生 Socket 与 Swing 的网络聊天程序，面向计算机网络课程实验场景设计。系统采用客户端/服务器架构，支持双人好友聊天、群聊对话、文字消息、语音消息、文件传输、图片发送、好友管理、群成员管理、头像与用户 ID 设置等功能。

界面使用 Java 原生 Swing 实现，整体风格简洁清晰，重点展示 Socket 网络通信、多线程并发处理、消息协议设计与客户端图形界面交互。

## 功能目标

### 用户基础功能

- 用户可以设置唯一 ID、昵称和头像。
- 客户端启动后连接服务器，完成用户登录或注册。
- 用户信息包括：用户 ID、昵称、头像路径或头像数据、在线状态。
- 支持在线用户状态展示，例如在线、离线。

### 好友聊天

- 好友聊天与群聊对话分开展示。
- 支持查看好友列表。
- 支持添加好友。
- 支持与好友进行一对一聊天。
- 支持在好友聊天中发送文字、图片、语音和文件。
- 支持显示聊天记录中的发送方、发送时间和消息类型。

### 群聊功能

- 支持创建群聊。
- 支持查看自己加入的群聊列表。
- 支持进入群聊并发送群消息。
- 支持在群聊中发送文字、图片、语音和文件。
- 支持查看群成员列表。
- 支持将自己的好友拉入群聊。
- 支持将群聊中的非好友成员添加为好友。
- 支持显示群消息发送者、发送时间和消息内容。

### 消息内容类型

- 文字消息：直接发送 UTF-8 文本内容。
- 图片消息：支持选择本地图片并发送，接收端可在聊天窗口预览。
- 文件消息：支持选择本地文件并发送，接收端可保存到本地。
- 语音消息：支持录音文件发送，接收端可下载或播放。

### 界面功能

- 使用 Java Swing 编写客户端界面。
- 主界面分为好友聊天区和群聊聊天区。
- 左侧展示好友列表与群聊列表。
- 右侧展示当前聊天窗口。
- 底部提供消息输入框、发送按钮、图片按钮、文件按钮、语音按钮。
- 支持用户头像展示。
- 支持用户 ID、昵称展示。
- 整体界面简洁，避免复杂视觉元素，突出实验功能。

## 功能实现清单

以下对照「功能目标」与「异常处理要求」，标注当前版本实现状态（`[x]` 已实现，`[ ]` 未实现）：

### 用户基础功能

- [x] 用户可以设置唯一 ID、昵称和头像
- [x] 客户端启动后连接服务器，完成用户登录或注册
- [x] 用户信息包括：用户 ID、昵称、头像路径或头像数据、在线状态
- [x] 支持在线用户状态展示（在线、离线）

### 好友聊天

- [x] 好友聊天与群聊对话分开展示（Tab 切换）
- [x] 支持查看好友列表
- [x] 支持添加好友（含重复添加校验与提示）
- [x] 支持与好友进行一对一聊天
- [x] 支持在好友聊天中发送文字、图片、语音和文件
- [x] 支持显示聊天记录中的发送方、发送时间和消息类型

### 群聊功能

- [x] 支持创建群聊
- [x] 支持查看自己加入的群聊列表
- [x] 支持进入群聊并发送群消息
- [x] 支持在群聊中发送文字、图片、语音和文件
- [x] 支持查看群成员列表
- [x] 支持将自己的好友拉入群聊（含重复入群校验与提示）
- [x] 支持将群聊中的非好友成员添加为好友
- [x] 支持显示群消息发送者、发送时间和消息内容

### 消息内容类型

- [x] 文字消息
- [x] 图片消息（选择本地图片、接收端预览）
- [x] 文件消息（选择本地文件、接收端保存）
- [x] 语音消息（本地 wav/mp3、录音、播放）

### 界面功能

- [x] 使用 Java Swing 编写客户端界面
- [x] 主界面分为好友聊天区和群聊聊天区
- [x] 左侧展示好友列表与群聊列表
- [x] 右侧展示当前聊天窗口
- [x] 底部提供消息输入框、发送按钮、图片/文件/语音/录音按钮
- [x] 支持用户头像展示
- [x] 支持用户 ID、昵称展示
- [x] 空列表、未选中聊天对象时显示友好提示

### 异常处理

- [x] 服务端处理客户端异常断开，更新在线状态并通知相关好友（`SERVER_NOTICE`）
- [x] 客户端处理连接失败、连接中断（弹窗提示，支持重新登录）
- [x] 客户端发送失败时 `JOptionPane` 提示
- [x] 文件过大时给出提示
- [x] 添加好友时避免重复添加
- [x] 邀请入群时避免重复加入
- [x] 群聊发送前校验用户是否仍在群中
- [x] Socket 与文件流使用后正确关闭（try-with-resources / finally）

## 技术选型

- 开发语言：Java
- 图形界面：Java Swing
- 网络通信：Java Socket
- 并发处理：Java Thread / ThreadPoolExecutor
- 数据传输：自定义消息协议
- 文件处理：Java IO / NIO
- 图片显示：ImageIcon
- 音频处理：Java Sound API 或发送录音文件

## 系统架构

系统采用 C/S 架构：

- 服务端负责维护用户连接、消息转发、好友关系、群聊关系和文件中转。
- 客户端负责用户界面展示、消息输入、文件选择、消息发送和消息接收。

```text
+------------------+          Socket          +------------------+
|   Client A       | <-----------------------> |                  |
| Swing UI         |                           |                  |
| Message Sender   |                           |                  |
| Message Receiver |                           |                  |
+------------------+                           |                  |
                                                |   Chat Server    |
+------------------+          Socket          |                  |
|   Client B       | <-----------------------> |                  |
| Swing UI         |                           |                  |
| Message Sender   |                           |                  |
| Message Receiver |                           |                  |
+------------------+                           +------------------+
```

## 推荐项目结构

```text
chat-system/
├── README.md
├── src/
│   ├── server/
│   │   ├── ChatServer.java
│   │   ├── ClientHandler.java
│   │   ├── UserManager.java
│   │   ├── FriendManager.java
│   │   ├── GroupManager.java
│   │   └── FileTransferManager.java
│   ├── client/
│   │   ├── ChatClient.java
│   │   ├── ClientConnection.java
│   │   ├── MessageReceiver.java
│   │   └── ui/
│   │       ├── LoginFrame.java
│   │       ├── MainFrame.java
│   │       ├── ChatPanel.java
│   │       ├── FriendListPanel.java
│   │       ├── GroupListPanel.java
│   │       └── UserProfileDialog.java
│   ├── common/
│   │   ├── Message.java
│   │   ├── MessageType.java
│   │   ├── User.java
│   │   ├── Group.java
│   │   ├── Protocol.java
│   │   └── FilePacket.java
│   └── util/
│       ├── JsonUtil.java
│       ├── FileUtil.java
│       ├── ImageUtil.java
│       └── TimeUtil.java
├── data/
│   ├── users.dat
│   ├── friends.dat
│   └── groups.dat
├── uploads/
│   ├── images/
│   ├── files/
│   └── voices/
└── downloads/
```

## 核心模块说明

### 服务端模块

#### ChatServer

负责启动服务器 Socket，监听指定端口，接收客户端连接，并为每个客户端分配独立处理线程。

主要职责：

- 监听端口。
- 接收客户端连接。
- 管理线程池。
- 保存在线用户连接。

#### ClientHandler

负责处理单个客户端连接，持续读取客户端发送的消息，并根据消息类型调用不同处理逻辑。

主要职责：

- 接收客户端消息。
- 解析消息类型。
- 转发私聊消息。
- 转发群聊消息。
- 处理好友请求。
- 处理拉人入群请求。
- 处理文件、图片、语音转发。

#### UserManager

负责用户信息管理。

主要职责：

- 用户注册。
- 用户登录。
- 用户 ID 唯一性校验。
- 用户头像和昵称更新。
- 在线状态维护。

#### FriendManager

负责好友关系管理。

主要职责：

- 添加好友。
- 查询好友列表。
- 判断两个用户是否为好友。
- 在群聊中添加非好友成员为好友。

#### GroupManager

负责群聊管理。

主要职责：

- 创建群聊。
- 查询用户所在群聊。
- 查询群成员。
- 邀请好友入群。
- 转发群消息。

#### FileTransferManager

负责图片、文件、语音等二进制内容的保存与转发。

主要职责：

- 接收上传文件。
- 保存到服务器目录。
- 生成文件访问标识。
- 通知接收方下载或接收文件。

### 客户端模块

#### ChatClient

客户端程序入口，负责启动登录界面并建立与服务器的连接。

#### ClientConnection

负责客户端与服务端之间的 Socket 连接。

主要职责：

- 建立连接。
- 发送消息。
- 发送文件。
- 关闭连接。

#### MessageReceiver

客户端后台接收线程，负责持续读取服务端消息，并更新 Swing 界面。

主要职责：

- 接收服务端转发的消息。
- 根据消息类型更新聊天窗口。
- 刷新好友列表、群聊列表和在线状态。

#### Swing UI

客户端界面使用 Swing 实现。

主要窗口：

- LoginFrame：登录与注册窗口。
- MainFrame：主聊天窗口。
- ChatPanel：聊天内容区与输入区。
- FriendListPanel：好友列表。
- GroupListPanel：群聊列表。
- UserProfileDialog：用户资料、头像、ID 设置窗口。

## 消息协议设计

客户端与服务端之间统一使用 `Message` 对象传输。实际实现时可使用对象流 `ObjectInputStream/ObjectOutputStream`，也可以使用 JSON 字符串协议。

推荐消息字段：

```text
messageId      消息唯一编号
type           消息类型
fromUserId     发送者 ID
toUserId       接收者 ID，私聊时使用
groupId        群聊 ID，群聊时使用
content        文本内容或文件描述
fileName       文件名
fileSize       文件大小
fileType       文件类型
fileBytes      文件二进制内容
timestamp      发送时间
extra          扩展字段
```

### 消息类型枚举

```text
LOGIN                 用户登录
REGISTER              用户注册
LOGOUT                用户退出
UPDATE_PROFILE        修改用户资料
PRIVATE_TEXT          私聊文字
PRIVATE_IMAGE         私聊图片
PRIVATE_FILE          私聊文件
PRIVATE_VOICE         私聊语音
GROUP_TEXT            群聊文字
GROUP_IMAGE           群聊图片
GROUP_FILE            群聊文件
GROUP_VOICE           群聊语音
ADD_FRIEND            添加好友
FRIEND_LIST           获取好友列表
CREATE_GROUP          创建群聊
INVITE_TO_GROUP       邀请好友入群
GROUP_LIST            获取群聊列表
GROUP_MEMBER_LIST     获取群成员列表
SERVER_NOTICE         服务端通知
ERROR                 错误消息
```

## 主要业务流程

### 登录流程

1. 用户输入 ID、昵称或密码信息。
2. 客户端发送登录消息到服务端。
3. 服务端校验用户信息。
4. 登录成功后，服务端记录用户在线状态。
5. 客户端进入主聊天界面。
6. 客户端拉取好友列表和群聊列表。

### 私聊流程

1. 用户在好友列表中选择好友。
2. 客户端切换到好友聊天窗口。
3. 用户输入文字或选择图片、文件、语音。
4. 客户端封装私聊消息并发送到服务端。
5. 服务端根据接收者 ID 查找在线连接。
6. 若接收者在线，则立即转发。
7. 若接收者离线，可保存离线消息，待其上线后推送。

### 群聊流程

1. 用户在群聊列表中选择群聊。
2. 客户端切换到群聊窗口。
3. 用户发送文字、图片、文件或语音。
4. 服务端查询该群所有成员。
5. 服务端将消息转发给群内所有在线成员。
6. 客户端在群聊窗口显示发送者昵称和消息内容。

### 邀请好友入群流程

1. 用户打开群成员管理窗口。
2. 用户从自己的好友列表中选择好友。
3. 客户端发送邀请入群请求。
4. 服务端校验邀请者是否在群内。
5. 服务端将好友加入群成员列表。
6. 服务端通知群内成员刷新成员列表。

### 添加群内非好友流程

1. 用户在群成员列表中选择某个非好友成员。
2. 点击添加好友。
3. 客户端发送添加好友请求。
4. 服务端建立好友关系。
5. 双方好友列表刷新。

### 文件、图片、语音发送流程

1. 用户在聊天窗口选择本地文件。
2. 客户端读取文件基本信息和二进制内容。
3. 客户端生成对应消息类型。
4. 服务端接收并保存文件。
5. 服务端将文件消息转发给目标用户或群成员。
6. 接收方客户端显示文件卡片、图片预览或语音消息。
7. 用户可选择保存文件或播放语音。

## 界面设计

主界面建议采用左右分栏布局：

```text
+-------------------------------------------------------------+
| 用户头像  昵称  ID                                           |
+----------------------+--------------------------------------+
| 好友列表 / 群聊列表   | 当前聊天对象名称                     |
|                      +--------------------------------------+
| 好友 A               | 聊天记录区                            |
| 好友 B               |                                      |
| 群聊 1               | 对方：你好                            |
| 群聊 2               | 我：你好                              |
|                      |                                      |
|                      +--------------------------------------+
|                      | 输入框                                |
|                      | [图片] [文件] [语音]          [发送]  |
+----------------------+--------------------------------------+
```

推荐 Swing 组件：

- JFrame：主窗口。
- JPanel：布局容器。
- JList：好友列表、群聊列表。
- JTextArea：聊天记录与消息输入。
- JButton：发送、图片、文件、语音、添加好友、邀请入群。
- JLabel：头像、昵称、ID、消息提示。
- JFileChooser：选择图片、文件和语音文件。
- JScrollPane：滚动聊天记录。
- JTabbedPane：切换好友聊天和群聊。

## 数据存储设计

实验版本可优先使用本地文件存储，便于实现和演示。

### 用户数据

```text
userId
nickname
avatarPath
password
onlineStatus
```

### 好友数据

```text
userId
friendUserId
createdAt
```

### 群聊数据

```text
groupId
groupName
ownerUserId
memberUserIds
createdAt
```

### 聊天记录

```text
messageId
type
fromUserId
toUserId
groupId
content
filePath
timestamp
```

## 运行方式

### 环境要求

- JDK 8 或以上版本
- Windows、macOS 或 Linux 均可运行
- 不依赖第三方界面库

### 编译

在项目根目录执行。首次编译需先创建 `out` 输出目录。

**Linux / macOS / Git Bash：**

```bash
mkdir -p out
javac -encoding UTF-8 -d out src/common/*.java src/util/*.java src/server/*.java src/client/*.java src/client/ui/*.java
```

**Windows PowerShell：**

```powershell
if (-not (Test-Path out)) { New-Item -ItemType Directory -Path out }
javac -encoding UTF-8 -d out src/common/*.java src/util/*.java src/server/*.java src/client/*.java src/client/ui/*.java
```

**Windows CMD（单行）：**

```cmd
if not exist out mkdir out && javac -encoding UTF-8 -d out src/common/*.java src/util/*.java src/server/*.java src/client/*.java src/client/ui/*.java
```

> 说明：`src/util/` 中的 `FileUtil`、`ImageUtil` 等工具类需与 `common`、`server`、`client` 一并编译。

### 启动服务端

```bash
java -cp out server.ChatServer
```

默认监听端口建议设置为：

```text
8888
```

### 启动客户端（Swing 界面）

客户端已升级为 Swing 图形界面。可以打开多个客户端模拟多用户聊天：

```bash
java -cp out client.ChatClient
```

启动后会弹出登录窗口，使用方式如下：

1. 在登录窗中填写：
   - **服务器地址**：默认 `127.0.0.1`。
   - **端口**：默认 `8888`。
   - **用户 ID**：必填，首次使用的 ID 会由服务端自动建号。
   - **昵称**：可留空，留空时以用户 ID 作为昵称。
2. 点击「登录」。登录成功后登录窗关闭并打开主窗口；连接失败或登录失败会弹出 `JOptionPane` 提示。
   - 也可点击「注册」完成账号注册后再登录。
3. 主窗口布局：
   - 顶部显示当前用户的昵称、ID 与头像。
   - 左侧为「好友 / 群聊」标签页，可切换列表并选择聊天对象。
   - 右侧为聊天记录区、群成员区（群聊时）、底部输入框与发送/图片/文件/语音/录音按钮。
   - 未选中好友或群聊时，聊天区显示「请选择好友」或「请选择群聊」提示。

> 说明：主窗口支持好友/群聊文字消息、图片发送预览、普通文件传输与语音消息（发送本地 wav/mp3、录音、播放）。连接中断时会弹窗提示，可选择重新登录。

启动参数不再需要在命令行指定服务器地址与端口，统一在登录窗口中填写。

## 打包成 exe 与他人共同使用

若希望把程序发给没有安装 JDK 的同学直接使用，可将服务端与客户端分别打包为 Windows 可执行文件（`.exe`）。推荐使用 JDK 自带的 **jpackage** 工具，它会自动捆绑运行时，对方电脑无需单独安装 Java。

### 打包环境要求

- **JDK 17 或以上**（需包含 `jpackage` 命令；在命令行执行 `jpackage --version` 可确认是否可用）
- Windows 10 / 11（生成 `.exe` 安装包或便携版应用）
- 可选：安装 [WiX Toolset 3.x](https://wixtoolset.org/) 或 [Inno Setup](https://jrsoftware.org/isinfo.php)，用于生成安装程序；不安装时也可使用 `app-image` 生成免安装目录

### 第一步：编译项目

在项目根目录执行（与上文「编译」步骤相同）：

```bash
mkdir out
javac -encoding UTF-8 -d out src/common/*.java src/util/*.java src/server/*.java src/client/*.java src/client/ui/*.java
```

### 第二步：生成可执行 JAR

在项目根目录创建 `dist` 目录，并将编译产物打成两个入口 JAR（服务端、客户端各一个）：

```bash
mkdir dist
jar cfe dist/chat-server.jar server.ChatServer -C out .
jar cfe dist/chat-client.jar client.ChatClient -C out .
```

说明：

- `chat-server.jar` 的主类为 `server.ChatServer`
- `chat-client.jar` 的主类为 `client.ChatClient`
- 两个 JAR 均包含 `common`、`server`、`client` 等全部已编译类，便于独立分发

打包前可用以下命令自测 JAR 是否正常：

```bash
java -jar dist/chat-server.jar
java -jar dist/chat-client.jar
```

### 第三步：使用 jpackage 生成 exe

在项目根目录执行（将 `C:\Program Files\Java\jdk-17` 替换为本机 JDK 17+ 的实际安装路径）：

**服务端（保留控制台窗口，便于查看日志）：**

```bash
jpackage --input dist ^
  --name ChatServer ^
  --main-jar chat-server.jar ^
  --main-class server.ChatServer ^
  --type app-image ^
  --dest release ^
  --java-options "-Dfile.encoding=UTF-8" ^
  --win-console
```

**客户端（图形界面，不弹出黑色控制台）：**

```bash
jpackage --input dist ^
  --name ChatClient ^
  --main-jar chat-client.jar ^
  --main-class client.ChatClient ^
  --type app-image ^
  --dest release ^
  --java-options "-Dfile.encoding=UTF-8"
```

完成后目录结构大致如下：

```text
release/
├── ChatServer/          # 服务端程序目录
│   └── ChatServer.exe
└── ChatClient/          # 客户端程序目录
    └── ChatClient.exe
```

将整个 `ChatServer` 或 `ChatClient` 文件夹压缩后发给他人即可；对方解压后双击对应 `.exe` 即可运行，**无需安装 JDK**。

若需要生成带安装向导的单一安装包，可将 `--type app-image` 改为 `--type exe`（可能需要 WiX 或 Inno Setup）。

### 与他人联机使用

1. **主机（运行服务端）**
   - 双击 `ChatServer.exe` 启动服务端，默认监听端口 `8888`。
   - 在命令行执行 `ipconfig`，记下本机局域网 IPv4 地址（例如 `192.168.1.100`）。
   - 在 Windows 防火墙中允许 `8888` 端口的入站连接，或临时关闭防火墙以便实验联机。

2. **其他参与者（运行客户端）**
   - 双击 `ChatClient.exe` 打开登录窗口。
   - **服务器地址**填写主机的局域网 IP（不要填 `127.0.0.1`，除非客户端与服务端在同一台电脑上）。
   - **端口**保持 `8888`，填写用户 ID 与昵称后点击「登录」。

3. **同一台电脑自测**
   - 先启动 `ChatServer.exe`，再启动多个 `ChatClient.exe`，服务器地址填 `127.0.0.1` 即可。

### 常见问题

| 现象 | 处理建议 |
|------|----------|
| 提示找不到 `jpackage` | 安装 JDK 17+，并确认 `JAVA_HOME` 指向该 JDK |
| 客户端无法连接服务器 | 检查服务端是否已启动、IP 与端口是否正确、防火墙是否放行 |
| 界面中文乱码 | 打包时已加入 `-Dfile.encoding=UTF-8`；源码编译需使用 `-encoding UTF-8` |
| 生成的 exe 体积较大 | 正常现象，因为内嵌了 Java 运行时（约数十 MB 至百余 MB） |

### 备选方案：Launch4j（仅熟悉 JAR 打包时）

若本机 JDK 版本较低、无法使用 `jpackage`，也可先用上文步骤生成 `chat-server.jar` / `chat-client.jar`，再借助 [Launch4j](http://launch4j.sourceforge.net/) 将 JAR 封装为 `.exe`。注意：Launch4j 默认不捆绑 JRE，对方电脑仍需安装 JRE 8+，或需自行配置内嵌运行时，分发便利性不如 `jpackage`。

## 实现顺序建议

1. 实现 `Message`、`MessageType`、`User`、`Group` 等公共模型。
2. 实现服务端 Socket 监听和客户端连接。
3. 实现客户端登录窗口和主窗口。
4. 实现文字私聊。
5. 实现好友列表和添加好友。
6. 实现群聊创建、群列表和群消息转发。
7. 实现邀请好友入群。
8. 实现群成员中添加非好友为好友。
9. 实现图片发送与预览。
10. 实现文件发送与保存。
11. 实现语音文件发送。
12. 实现头像和用户资料设置。
13. 完善异常处理、离线提示和界面细节。

## 异常处理要求

以上异常处理项均已实现，详见「功能实现清单 → 异常处理」。

## 已知限制

1. **数据无持久化**：用户、好友、群聊数据保存在服务端内存中，服务重启后丢失。
2. **断线重连**：连接中断后仅提示用户重新登录，不支持自动重连或会话恢复。
3. **离线消息**：目标用户不在线时私聊发送失败并提示，不保存离线消息。
4. **大文件传输**：图片/文件/语音完整嵌入 `Message` 对象流序列化，大文件时内存开销较大（图片上限 5MB，文件/语音上限 10MB）。
5. **登录昵称字段**：登录窗口的昵称输入仅影响本地显示，以服务端注册昵称为准。
6. **好友关系**：添加好友为单方直接建立双向关系，无好友申请确认流程。
7. **聊天记录**：仅保存在客户端内存，关闭客户端后丢失。

## 可扩展功能

- 离线消息保存与上线推送。
- 聊天记录本地持久化。
- 群主踢人、解散群聊。
- 好友申请确认机制。
- 消息已读状态。
- 文件分块传输与断点续传。
- 语音录制和即时播放。
- 局域网自动发现服务器。

## 实验重点

本项目重点体现以下计算机网络与 Java 编程知识：

- Socket 客户端与服务端通信。
- 多客户端并发连接处理。
- 自定义应用层消息协议。
- 文本与二进制数据传输。
- 私聊与群聊消息转发机制。
- Swing 图形界面事件处理。
- 文件 IO 与网络 IO 协同。

## 项目预期效果

完成后，用户可以启动一个服务端，并启动多个客户端模拟不同用户。不同用户之间可以添加好友并进行一对一聊天，也可以创建群聊并邀请好友入群。聊天过程中可以发送文字、图片、语音文件和普通文件。好友聊天与群聊聊天在界面上清晰分离，用户可以设置自己的头像、昵称和 ID，整体界面简洁直观，适合作为 Java Socket 网络编程课程实验项目。
