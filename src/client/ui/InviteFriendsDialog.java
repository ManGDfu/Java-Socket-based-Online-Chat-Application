package client.ui;

import common.User;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.WindowConstants;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

/**
 * 邀请好友入群对话框：展示当前用户好友列表，支持多选后确认邀请。
 */
public class InviteFriendsDialog extends JDialog {

    public interface InviteListener {
        void onInvite(List<String> friendUserIds);
    }

    private static final Color BG = new Color(0xF2F2F2);

    private final DefaultListModel<String> displayModel = new DefaultListModel<String>();
    private final List<String> friendIds = new ArrayList<String>();
    private final JList<String> friendList = new JList<String>(displayModel);

    private InviteListener inviteListener;

    public InviteFriendsDialog(Frame owner, List<User> friends) {
        super(owner, "邀请好友入群", true);
        initUI(friends);
    }

    private void initUI(List<User> friends) {
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setSize(360, 320);
        setLocationRelativeTo(getOwner());

        JPanel root = new JPanel(new BorderLayout(0, 8));
        root.setBackground(BG);
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel hint = new JLabel("请选择要邀请的好友（可多选）：");
        root.add(hint, BorderLayout.NORTH);

        friendList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        populateFriends(friends);
        root.add(new JScrollPane(friendList), BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.setBackground(BG);
        JButton cancelButton = new JButton("取消");
        JButton inviteButton = new JButton("邀请");
        buttons.add(cancelButton);
        buttons.add(inviteButton);
        root.add(buttons, BorderLayout.SOUTH);

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
        inviteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doInvite();
            }
        });

        setContentPane(root);
    }

    public void setInviteListener(InviteListener listener) {
        this.inviteListener = listener;
    }

    private void populateFriends(List<User> friends) {
        displayModel.clear();
        friendIds.clear();
        if (friends == null) {
            return;
        }
        for (User user : friends) {
            if (user == null || user.getUserId() == null || user.getUserId().trim().isEmpty()) {
                continue;
            }
            String id = user.getUserId().trim();
            String nickname = user.getNickname();
            if (nickname == null || nickname.trim().isEmpty()) {
                nickname = id;
            }
            friendIds.add(id);
            String status = user.isOnline() ? "在线" : "离线";
            displayModel.addElement(nickname + "（ID：" + id + "）[" + status + "]");
        }
    }

    private void doInvite() {
        int[] indices = friendList.getSelectedIndices();
        if (indices == null || indices.length == 0) {
            return;
        }
        List<String> selected = new ArrayList<String>();
        for (int index : indices) {
            if (index >= 0 && index < friendIds.size()) {
                selected.add(friendIds.get(index));
            }
        }
        if (selected.isEmpty() || inviteListener == null) {
            return;
        }
        inviteListener.onInvite(selected);
        dispose();
    }
}
