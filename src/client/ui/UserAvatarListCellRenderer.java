package client.ui;

import common.User;
import util.ImageUtil;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;

/**
 * 带头像的用户列表单元格渲染器。
 */
public class UserAvatarListCellRenderer implements ListCellRenderer<User> {

    private final JLabel avatarLabel = new JLabel();
    private final JLabel textLabel = new JLabel();
    private final JPanel panel = new JPanel(new BorderLayout(6, 0));
    private final int avatarSize;

    public UserAvatarListCellRenderer(int avatarSize) {
        this.avatarSize = avatarSize;
        panel.setOpaque(true);
        avatarLabel.setHorizontalAlignment(SwingConstants.CENTER);
        avatarLabel.setPreferredSize(new Dimension(avatarSize + 4, avatarSize + 4));
        panel.add(avatarLabel, BorderLayout.WEST);
        panel.add(textLabel, BorderLayout.CENTER);
        panel.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
    }

    public UserAvatarListCellRenderer() {
        this(ImageUtil.LIST_AVATAR_SIZE);
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends User> list, User user, int index,
                                                  boolean isSelected, boolean cellHasFocus) {
        Color bg = isSelected ? list.getSelectionBackground() : list.getBackground();
        Color fg = isSelected ? list.getSelectionForeground() : list.getForeground();
        panel.setBackground(bg);
        textLabel.setForeground(fg);

        if (user == null) {
            avatarLabel.setIcon(ImageUtil.defaultAvatar(avatarSize));
            textLabel.setText("");
            return panel;
        }
        String id = user.getUserId() != null ? user.getUserId() : "";
        String nickname = user.getNickname();
        if (nickname == null || nickname.trim().isEmpty()) {
            nickname = id;
        }
        avatarLabel.setIcon(ImageUtil.avatarForUser(id, user.getAvatarPath(), avatarSize));
        String status = user.isOnline() ? "在线" : "离线";
        textLabel.setText(nickname + "（ID：" + id + "）[" + status + "]");
        return panel;
    }
}
