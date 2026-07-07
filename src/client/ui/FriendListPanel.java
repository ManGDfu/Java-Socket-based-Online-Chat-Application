package client.ui;

import common.User;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

/**
 * 好友列表面板（左侧「好友」标签页内容）。
 *
 * 展示服务端返回的好友信息（昵称 + ID），支持添加好友与选中切换私聊对象。
 */
public class FriendListPanel extends JPanel {

    /** 选中好友时的回调。 */
    public interface SelectionListener {
        void onFriendSelected(String friendUserId);
    }

    /** 点击「添加」时向服务端发起添加好友请求。 */
    public interface AddFriendListener {
        void onAddFriend(String friendUserId);
    }

    private static final Color BG = new Color(0xF2F2F2);

    private final DefaultListModel<User> displayModel = new DefaultListModel<User>();
    private final List<String> friendIds = new ArrayList<String>();
    private final List<User> friendUsers = new ArrayList<User>();
    private final JList<User> friendList = new JList<User>(displayModel);
    private final JTextField addField = new JTextField(8);
    private final JButton addButton = new JButton("添加");

    private SelectionListener selectionListener;
    private AddFriendListener addFriendListener;

    public FriendListPanel() {
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout(0, 6));
        setBackground(BG);
        setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        friendList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        friendList.setCellRenderer(new UserAvatarListCellRenderer());
        friendList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting()) {
                    return;
                }
                notifySelection();
            }
        });
        add(new JScrollPane(friendList), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(4, 0));
        bottom.setBackground(BG);
        bottom.add(new JLabel("ID："), BorderLayout.WEST);
        bottom.add(addField, BorderLayout.CENTER);
        bottom.add(addButton, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);

        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addFriendFromInput();
            }
        });
        addField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addFriendFromInput();
            }
        });
    }

    public void setSelectionListener(SelectionListener listener) {
        this.selectionListener = listener;
    }

    public void setAddFriendListener(AddFriendListener listener) {
        this.addFriendListener = listener;
    }

    /** 用服务端返回的好友 User 列表替换当前列表。 */
    public void setFriendUsers(List<User> friends) {
        displayModel.clear();
        friendIds.clear();
        friendUsers.clear();
        if (friends != null) {
            for (User user : friends) {
                if (user == null || user.getUserId() == null || user.getUserId().trim().isEmpty()) {
                    continue;
                }
                String id = user.getUserId().trim();
                friendIds.add(id);
                friendUsers.add(user);
                displayModel.addElement(user);
            }
        }
    }

    /** 返回当前好友 User 列表副本，供邀请入群等场景使用。 */
    public List<User> getFriendUsers() {
        return new ArrayList<User>(friendUsers);
    }

    /** 若不存在则加入列表（收到陌生人私聊时，以 ID 作为显示）。 */
    public void ensureFriend(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return;
        }
        String id = userId.trim();
        if (!containsFriend(id)) {
            User user = new User(id, id);
            user.setOnline(false);
            friendIds.add(id);
            friendUsers.add(user);
            displayModel.addElement(user);
        }
    }

    /** 更新指定好友的昵称与头像（资料变更通知时调用）。 */
    public void updateFriendProfile(String friendUserId, String nickname, String avatarPath) {
        if (friendUserId == null) {
            return;
        }
        String id = friendUserId.trim();
        for (int i = 0; i < friendUsers.size(); i++) {
            User user = friendUsers.get(i);
            if (user != null && id.equals(user.getUserId())) {
                if (nickname != null && !nickname.trim().isEmpty()) {
                    user.setNickname(nickname.trim());
                }
                if (avatarPath != null && !avatarPath.trim().isEmpty()) {
                    user.setAvatarPath(avatarPath.trim());
                }
                displayModel.set(i, user);
                return;
            }
        }
    }

    public boolean containsFriend(String userId) {
        if (userId == null) {
            return false;
        }
        return friendIds.contains(userId.trim());
    }

    public String getSelectedFriendId() {
        int index = friendList.getSelectedIndex();
        if (index < 0 || index >= friendIds.size()) {
            return null;
        }
        return friendIds.get(index);
    }

    private void addFriendFromInput() {
        String id = addField.getText().trim();
        if (id.isEmpty()) {
            return;
        }
        if (addFriendListener != null) {
            addFriendListener.onAddFriend(id);
        }
        addField.setText("");
    }

    private void notifySelection() {
        if (selectionListener == null) {
            return;
        }
        String selected = getSelectedFriendId();
        if (selected != null) {
            selectionListener.onFriendSelected(selected);
        }
    }

}
