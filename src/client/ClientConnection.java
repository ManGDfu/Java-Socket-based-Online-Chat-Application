package client;

import common.Message;
import common.Protocol;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * 客户端与服务端之间的 Socket 连接封装。
 *
 * 职责：
 * - 建立到服务端的连接（创建对象输入/输出流）。
 * - 发送 {@link Message}。
 * - 接收 {@link Message}。
 * - 关闭连接。
 *
 * 该类不涉及任何界面逻辑，便于在命令行与 Swing 客户端之间复用。
 */
public class ClientConnection {

    private final String host;
    private final int port;

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    public ClientConnection(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /**
     * 建立连接。注意必须先创建并 flush 输出流，再创建输入流，
     * 否则会与服务端的对象流互相阻塞。
     */
    public void connect() throws IOException {
        socket = new Socket(host, port);
        out = new ObjectOutputStream(socket.getOutputStream());
        out.flush();
        in = new ObjectInputStream(socket.getInputStream());
    }

    /** 发送一条消息。 */
    public void send(Message message) throws IOException {
        Protocol.send(out, message);
    }

    /** 阻塞读取一条消息。 */
    public Message receive() throws IOException, ClassNotFoundException {
        return Protocol.receive(in);
    }

    /** 关闭连接，释放所有流与 Socket。 */
    public void close() {
        try {
            if (out != null) {
                out.close();
            }
        } catch (IOException ignored) {
        }
        try {
            if (in != null) {
                in.close();
            }
        } catch (IOException ignored) {
        }
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException ignored) {
        }
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }
}
