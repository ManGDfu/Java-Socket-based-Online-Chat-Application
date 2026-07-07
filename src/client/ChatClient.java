package client;

import client.ui.LoginFrame;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Swing 版聊天客户端程序入口。
 *
 * 启动后在事件分发线程（EDT）上打开 {@link LoginFrame} 登录窗口，
 * 登录成功后由登录窗负责打开主窗口 {@link client.ui.MainFrame}。
 *
 * Socket 连接与收发逻辑已抽取到 {@link ClientConnection} 与
 * {@link MessageReceiver}，界面层只通过这两个类与服务端交互。
 */
public class ChatClient {

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // 使用系统外观失败时退回到默认外观即可
        }

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new LoginFrame().setVisible(true);
            }
        });
    }
}
