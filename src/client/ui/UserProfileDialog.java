package client.ui;

import util.ImageUtil;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileNameExtensionFilter;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

/**
 * 用户资料对话框：展示用户 ID（只读）、昵称与头像，支持修改后保存。
 */
public class UserProfileDialog extends JDialog {

    public interface SaveListener {
        /**
         * @param nickname      新昵称
         * @param avatarBytes   新头像字节，未更换头像时为 null
         * @param avatarFileName 头像文件名，未更换时为 null
         */
        void onSave(String nickname, byte[] avatarBytes, String avatarFileName);
    }

    private static final Color BG = new Color(0xF2F2F2);

    private final JLabel avatarPreview = new JLabel();
    private final JTextField nicknameField = new JTextField(16);
    private final JLabel userIdLabel;

    private byte[] pendingAvatarBytes;
    private String pendingAvatarFileName;
    private String currentAvatarPath;

    private SaveListener saveListener;

    public UserProfileDialog(Frame owner, String userId, String nickname, String avatarPath) {
        super(owner, "个人资料", true);
        this.currentAvatarPath = avatarPath;
        this.userIdLabel = new JLabel(userId != null ? userId : "");
        initUI(nickname);
        refreshAvatarPreview();
    }

    private void initUI(String nickname) {
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setResizable(false);

        JPanel root = new JPanel(new BorderLayout(0, 10));
        root.setBackground(BG);
        root.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(BG);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 4, 6, 4);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.gridy = 0;
        form.add(new JLabel("头像："), gbc);

        JPanel avatarPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        avatarPanel.setBackground(BG);
        avatarPreview.setBorder(BorderFactory.createLineBorder(new Color(0xCCCCCC)));
        avatarPreview.setPreferredSize(new java.awt.Dimension(
                ImageUtil.PROFILE_AVATAR_SIZE, ImageUtil.PROFILE_AVATAR_SIZE));
        JButton chooseButton = new JButton("选择图片");
        chooseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                chooseAvatar();
            }
        });
        avatarPanel.add(avatarPreview);
        avatarPanel.add(chooseButton);
        gbc.gridx = 1;
        form.add(avatarPanel, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        form.add(new JLabel("用户 ID："), gbc);
        gbc.gridx = 1;
        userIdLabel.setForeground(new Color(0x666666));
        form.add(userIdLabel, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        form.add(new JLabel("昵称："), gbc);
        gbc.gridx = 1;
        nicknameField.setText(nickname != null ? nickname : "");
        form.add(nicknameField, gbc);

        JLabel idHint = new JLabel("（用户 ID 注册后不可修改）");
        idHint.setForeground(new Color(0x888888));
        idHint.setFont(idHint.getFont().deriveFont(11f));
        gbc.gridx = 1;
        gbc.gridy = 3;
        form.add(idHint, gbc);

        root.add(form, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setBackground(BG);
        JButton cancelButton = new JButton("取消");
        JButton saveButton = new JButton("保存");
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doSave();
            }
        });
        buttons.add(cancelButton);
        buttons.add(saveButton);
        root.add(buttons, BorderLayout.SOUTH);

        setContentPane(root);
        pack();
        setLocationRelativeTo(getOwner());
    }

    public void setSaveListener(SaveListener listener) {
        this.saveListener = listener;
    }

    private void chooseAvatar() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("选择头像图片");
        chooser.setFileFilter(new FileNameExtensionFilter("图片文件 (jpg, png, gif)", "jpg", "jpeg", "png", "gif"));
        int result = chooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = chooser.getSelectedFile();
        if (file == null || !ImageUtil.isImageFile(file)) {
            JOptionPane.showMessageDialog(this, "请选择 jpg/png/gif 格式的图片。", "提示",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (file.length() > ImageUtil.MAX_IMAGE_BYTES) {
            JOptionPane.showMessageDialog(this,
                    "图片过大，最大允许 " + util.FileUtil.humanSize(ImageUtil.MAX_IMAGE_BYTES) + "。",
                    "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            pendingAvatarBytes = util.FileUtil.readFileBytes(file);
            pendingAvatarFileName = file.getName();
            refreshAvatarPreview();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "读取图片失败：" + ex.getMessage(), "错误",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void refreshAvatarPreview() {
        if (pendingAvatarBytes != null) {
            avatarPreview.setIcon(ImageUtil.scaleIcon(ImageUtil.toIcon(pendingAvatarBytes),
                    ImageUtil.PROFILE_AVATAR_SIZE));
        } else {
            avatarPreview.setIcon(ImageUtil.avatarForUser(userIdLabel.getText(), currentAvatarPath,
                    ImageUtil.PROFILE_AVATAR_SIZE));
        }
    }

    private void doSave() {
        String nickname = nicknameField.getText().trim();
        if (nickname.isEmpty()) {
            JOptionPane.showMessageDialog(this, "昵称不能为空。", "提示",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (saveListener != null) {
            saveListener.onSave(nickname, pendingAvatarBytes, pendingAvatarFileName);
        }
        dispose();
    }
}
