package common;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * 通信协议相关的常量与工具方法。
 *
 * 本项目使用对象流传输 {@link Message} 对象，这里集中放置默认端口、主机
 * 等配置，以及基于对象流的收发封装，方便服务端与客户端复用。
 */
public final class Protocol {

    /** 默认服务端监听端口。 */
    public static final int DEFAULT_PORT = 8888;

    /** 默认服务端主机地址。 */
    public static final String DEFAULT_HOST = "127.0.0.1";

    private Protocol() {
    }

    /**
     * 向对象输出流发送一条消息，并立即刷新缓冲。
     */
    public static void send(ObjectOutputStream out, Message message) throws IOException {
        synchronized (out) {
            out.writeObject(message);
            out.flush();
            // reset 可避免对象流缓存同一引用导致后续修改不被发送
            out.reset();
        }
    }

    /**
     * 从对象输入流读取一条消息。
     */
    public static Message receive(ObjectInputStream in) throws IOException, ClassNotFoundException {
        Object obj = in.readObject();
        return (Message) obj;
    }
}
