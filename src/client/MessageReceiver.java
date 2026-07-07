package client;

import common.Message;

import javax.swing.SwingUtilities;

/**
 * 客户端后台接收线程。
 *
 * 持续从 {@link ClientConnection} 读取服务端转发的消息，并通过
 * {@link SwingUtilities#invokeLater} 切回事件分发线程（EDT）通知监听者，
 * 以保证 Swing 界面更新的线程安全。
 *
 * 监听者应在其 {@link Listener#onMessage} 中处理 {@code PRIVATE_TEXT}、
 * {@code SERVER_NOTICE} 等类型，刷新对应好友的 {@link client.ui.ChatPanel}
 * 与 {@link client.ui.FriendListPanel}。
 */
public class MessageReceiver implements Runnable {

    /** 接收线程的回调接口，所有回调均在 EDT 上执行。 */
    public interface Listener {
        /**
         * 收到一条服务端消息。
         * 对 {@link common.MessageType#PRIVATE_TEXT} 应更新对应会话历史并刷新聊天区；
         * 对 {@link common.MessageType#SERVER_NOTICE} 应在当前聊天区显示系统提示。
         */
        void onMessage(Message message);

        /** 与服务端的连接已断开。 */
        void onDisconnected();
    }

    private final ClientConnection connection;
    private final Listener listener;
    private volatile boolean running = true;

    public MessageReceiver(ClientConnection connection, Listener listener) {
        this.connection = connection;
        this.listener = listener;
    }

    @Override
    public void run() {
        try {
            Message message;
            while (running && (message = connection.receive()) != null) {
                final Message delivered = message;
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        listener.onMessage(delivered);
                    }
                });
            }
        } catch (Exception e) {
            // 连接断开或反序列化失败时退出循环
        } finally {
            if (running) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        listener.onDisconnected();
                    }
                });
            }
        }
    }

    /** 请求停止接收（例如用户主动退出时）。 */
    public void stop() {
        running = false;
    }
}
