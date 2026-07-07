package client.ui;



import client.ClientConnection;

import common.Message;

import common.MessageType;

import common.Protocol;



import javax.swing.BorderFactory;

import javax.swing.JButton;

import javax.swing.JFrame;

import javax.swing.JLabel;

import javax.swing.JOptionPane;

import javax.swing.JPanel;

import javax.swing.JPasswordField;

import javax.swing.JTextField;

import javax.swing.SwingUtilities;

import javax.swing.WindowConstants;



import java.awt.Color;

import java.awt.Component;

import java.awt.Dimension;

import java.awt.GridBagConstraints;

import java.awt.GridBagLayout;

import java.awt.Insets;

import java.awt.event.ActionEvent;

import java.awt.event.ActionListener;



/**

 * 登录 / 注册窗口。

 *

 * 用户填写服务器地址、端口、用户名、昵称与密码：

 * <ul>

 *   <li>「登录」：发送 {@link MessageType#LOGIN}，密码放在 {@code extra} 字段。</li>

 *   <li>「注册」：发送 {@link MessageType#REGISTER}，昵称放在 {@code content}，密码放在 {@code extra}。</li>

 * </ul>

 */

public class LoginFrame extends JFrame {



    private final JTextField hostField = new JTextField(Protocol.DEFAULT_HOST, 14);

    private final JTextField portField = new JTextField(String.valueOf(Protocol.DEFAULT_PORT), 14);

    private final JTextField userIdField = new JTextField(14);

    private final JTextField nicknameField = new JTextField(14);

    private final JPasswordField passwordField = new JPasswordField(14);



    private final JButton loginButton = new JButton("登录");

    private final JButton registerButton = new JButton("注册");



    public LoginFrame() {

        super("聊天客户端 - 登录");

        initUI();

    }



    private void initUI() {

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        setResizable(false);



        JPanel content = new JPanel(new GridBagLayout());

        content.setBackground(new Color(0xF2F2F2));

        content.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));



        GridBagConstraints gbc = new GridBagConstraints();

        gbc.insets = new Insets(6, 6, 6, 6);

        gbc.anchor = GridBagConstraints.WEST;



        addRow(content, gbc, 0, "服务器地址：", hostField);

        addRow(content, gbc, 1, "端口：", portField);

        addRow(content, gbc, 2, "用户名：", userIdField);

        addRow(content, gbc, 3, "昵称：", nicknameField);

        addRow(content, gbc, 4, "密码：", passwordField);



        JPanel buttons = new JPanel();

        buttons.setBackground(new Color(0xF2F2F2));

        buttons.add(loginButton);

        buttons.add(registerButton);



        gbc.gridx = 0;

        gbc.gridy = 5;

        gbc.gridwidth = 2;

        gbc.anchor = GridBagConstraints.CENTER;

        content.add(buttons, gbc);



        setContentPane(content);



        loginButton.addActionListener(new ActionListener() {

            @Override

            public void actionPerformed(ActionEvent e) {

                doLogin();

            }

        });

        registerButton.addActionListener(new ActionListener() {

            @Override

            public void actionPerformed(ActionEvent e) {

                doRegister();

            }

        });



        getRootPane().setDefaultButton(loginButton);



        pack();

        setMinimumSize(new Dimension(340, getHeight()));

        setLocationRelativeTo(null);

    }



    private void addRow(JPanel panel, GridBagConstraints gbc, int row, String label, Component field) {

        gbc.gridx = 0;

        gbc.gridy = row;

        gbc.gridwidth = 1;

        gbc.anchor = GridBagConstraints.EAST;

        panel.add(new JLabel(label), gbc);



        gbc.gridx = 1;

        gbc.anchor = GridBagConstraints.WEST;

        panel.add(field, gbc);

    }



    private boolean validateCommonFields(String host, String portText, String userId, String password) {

        if (host.isEmpty()) {

            showError("请填写服务器地址。");

            return false;

        }

        try {

            Integer.parseInt(portText);

        } catch (NumberFormatException ex) {

            showError("端口必须是数字。");

            return false;

        }

        if (userId.isEmpty()) {

            showError("请填写用户名。");

            return false;

        }

        if (password.isEmpty()) {

            showError("请填写密码。");

            return false;

        }

        return true;

    }



    private void doLogin() {

        final String host = hostField.getText().trim();

        final String portText = portField.getText().trim();

        final String userId = userIdField.getText().trim();

        final String nickname = nicknameField.getText().trim();

        final String password = new String(passwordField.getPassword());



        if (!validateCommonFields(host, portText, userId, password)) {

            return;

        }

        final int port = Integer.parseInt(portText);



        setLoginInProgress(true);



        Thread worker = new Thread(new Runnable() {

            @Override

            public void run() {

                final ClientConnection connection = new ClientConnection(host, port);

                try {

                    connection.connect();



                    Message loginMsg = new Message();

                    loginMsg.setType(MessageType.LOGIN);

                    loginMsg.setFromUserId(userId);

                    loginMsg.setExtra(password);

                    connection.send(loginMsg);



                    final Message response = connection.receive();

                    SwingUtilities.invokeLater(new Runnable() {

                        @Override

                        public void run() {

                            handleLoginResponse(connection, userId, nickname, response);

                        }

                    });

                } catch (Exception ex) {

                    connection.close();

                    final String reason = ex.getMessage();

                    SwingUtilities.invokeLater(new Runnable() {

                        @Override

                        public void run() {

                            setInputEnabled(true);
                            showError("连接服务器失败：" + reason);

                        }

                    });

                }

            }

        });

        worker.setDaemon(true);

        worker.start();

    }



    private void doRegister() {

        final String host = hostField.getText().trim();

        final String portText = portField.getText().trim();

        final String userId = userIdField.getText().trim();

        final String nickname = nicknameField.getText().trim();

        final String password = new String(passwordField.getPassword());



        if (!validateCommonFields(host, portText, userId, password)) {

            return;

        }

        final int port = Integer.parseInt(portText);



        setRegisterInProgress(true);



        Thread worker = new Thread(new Runnable() {

            @Override

            public void run() {

                final ClientConnection connection = new ClientConnection(host, port);

                try {

                    connection.connect();



                    Message registerMsg = new Message();

                    registerMsg.setType(MessageType.REGISTER);

                    registerMsg.setFromUserId(userId);

                    registerMsg.setContent(nickname);

                    registerMsg.setExtra(password);

                    connection.send(registerMsg);



                    final Message response = connection.receive();

                    SwingUtilities.invokeLater(new Runnable() {

                        @Override

                        public void run() {

                            handleRegisterResponse(response);

                        }

                    });

                } catch (Exception ex) {

                    final String reason = ex.getMessage();

                    SwingUtilities.invokeLater(new Runnable() {

                        @Override

                        public void run() {

                            setInputEnabled(true);
                            showError("连接服务器失败：" + reason);

                        }

                    });

                } finally {

                    connection.close();

                }

            }

        });

        worker.setDaemon(true);

        worker.start();

    }



    private void handleLoginResponse(ClientConnection connection, String userId, String nickname, Message response) {

        if (response != null && response.getType() == MessageType.SERVER_NOTICE) {

            String shownName = nickname.isEmpty() ? userId : nickname;

            if (response.getContent() != null && response.getContent().contains("欢迎你")) {

                int start = response.getContent().indexOf("欢迎你，");

                if (start >= 0) {

                    int end = response.getContent().indexOf("（ID：");

                    if (end > start + 4) {

                        shownName = response.getContent().substring(start + 4, end);

                    }

                }

            }

            String avatarPath = response.getExtra();
            byte[] avatarBytes = response.getFileBytes();
            MainFrame mainFrame = new MainFrame(connection, userId, shownName, avatarPath, avatarBytes);

            mainFrame.appendSystemMessage(response.getContent());

            mainFrame.setVisible(true);

            dispose();

        } else {

            connection.close();

            setInputEnabled(true);
            String reason = (response != null && response.getContent() != null)

                    ? response.getContent()

                    : "登录失败，请稍后重试。";

            showError(reason);

        }

    }



    private void handleRegisterResponse(Message response) {

        setInputEnabled(true);
        if (response != null && response.getType() == MessageType.SERVER_NOTICE) {

            JOptionPane.showMessageDialog(this, response.getContent(), "注册成功",

                    JOptionPane.INFORMATION_MESSAGE);

        } else {

            String reason = (response != null && response.getContent() != null)

                    ? response.getContent()

                    : "注册失败，请稍后重试。";

            showError(reason);

        }

    }



    private void setLoginInProgress(boolean inProgress) {
        setInputEnabled(!inProgress);
        if (inProgress) {
            loginButton.setText("登录中…");
        }
    }

    private void setRegisterInProgress(boolean inProgress) {
        setInputEnabled(!inProgress);
        if (inProgress) {
            registerButton.setText("注册中…");
        }
    }

    private void setInputEnabled(boolean enabled) {
        hostField.setEnabled(enabled);
        portField.setEnabled(enabled);
        userIdField.setEnabled(enabled);
        nicknameField.setEnabled(enabled);
        passwordField.setEnabled(enabled);
        loginButton.setEnabled(enabled);
        registerButton.setEnabled(enabled);
        loginButton.setText("登录");
        registerButton.setText("注册");
    }



    private void showError(String message) {

        JOptionPane.showMessageDialog(this, message, "提示", JOptionPane.WARNING_MESSAGE);

    }

}


