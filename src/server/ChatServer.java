package server;

import common.Message;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 聊天服务端。
 *
 * 职责：
 * - 监听端口（默认 8888）。
 * - 接收客户端连接，为每个连接分配一个 {@link ClientHandler} 线程。
 * - 通过线程池管理并发连接。
 * - 维护在线用户连接表，供消息转发使用。
 *
 * 当前阶段仅支持登录与私聊文字转发，群聊、文件等暂未实现。
 */
public class ChatServer {

    /** 默认监听端口。 */
    public static final int PORT = common.Protocol.DEFAULT_PORT;

    /** 在线用户：userId -> 处理该用户连接的 ClientHandler。 */
    private final ConcurrentHashMap<String, ClientHandler> onlineHandlers = new ConcurrentHashMap<String, ClientHandler>();

    /** 【持久化改造】JSON 文件持久化管理器，在 exe 工作目录读写三个 json 文件。 */
    private final JsonPersistenceManager persistence = new JsonPersistenceManager();

    /** 【持久化改造】注入 persistence，数据变更后自动写 user.json。 */
    private final UserManager userManager = new UserManager(persistence);

    /** 【持久化改造】注入 persistence，好友变更后自动写 friend_relation.json。 */
    private final FriendManager friendManager = new FriendManager(persistence);

    /** 【持久化改造】注入 persistence，群聊变更后自动写 group_data.json。 */
    private final GroupManager groupManager = new GroupManager(persistence);

    private final ExecutorService threadPool = Executors.newCachedThreadPool();

    private volatile boolean running = false;

    public void start(int port) {
        running = true;

        // 【持久化改造】启动监听前，先从 JSON 文件加载历史用户/好友/群聊到内存 Map
        persistence.loadAll(userManager, friendManager, groupManager);

        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("[服务器] 已启动，监听端口 " + port + " ...");
            while (running) {
                Socket socket = serverSocket.accept();
                System.out.println("[服务器] 新连接：" + socket.getRemoteSocketAddress());
                ClientHandler handler = new ClientHandler(socket, this);
                threadPool.execute(handler);
            }
        } catch (IOException e) {
            if (running) {
                System.err.println("[服务器] 运行异常：" + e.getMessage());
            }
        } finally {
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (IOException ignored) {
                }
            }
            threadPool.shutdownNow();
        }
    }

    public UserManager getUserManager() {
        return userManager;
    }

    public FriendManager getFriendManager() {
        return friendManager;
    }

    public GroupManager getGroupManager() {
        return groupManager;
    }

    /** 注册一个在线连接（登录成功后调用）。 */
    public void register(String userId, ClientHandler handler) {
        onlineHandlers.put(userId, handler);
    }

    /** 移除一个在线连接（断开或退出时调用）。 */
    public void unregister(String userId) {
        if (userId != null) {
            onlineHandlers.remove(userId);
            userManager.logout(userId);
        }
    }

    public boolean isOnline(String userId) {
        return userId != null && onlineHandlers.containsKey(userId);
    }

    /**
     * 将消息发送给指定在线用户。
     *
     * @return 目标用户在线并发送成功返回 true，否则 false。
     */
    public boolean sendToUser(String toUserId, Message message) {
        ClientHandler handler = onlineHandlers.get(toUserId);
        if (handler == null) {
            return false;
        }
        handler.sendMessage(message);
        return true;
    }

    /** 向群内所有在线成员发送消息（不含发送者自身时可在外层控制）。 */
    public void sendToGroupMembers(String groupId, Message message, String excludeUserId) {
        for (String memberId : groupManager.getMemberIds(groupId)) {
            if (excludeUserId != null && excludeUserId.equals(memberId)) {
                continue;
            }
            sendToUser(memberId, message);
        }
    }

    public static void main(String[] args) {
        int port = PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("[服务器] 端口参数无效，使用默认端口 " + PORT);
            }
        }
        new ChatServer().start(port);
    }
}
