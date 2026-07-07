package client.ui;

import client.ClientConnection;
import client.MessageReceiver;
import common.FilePacket;
import server.FileTransferManager;
import common.Group;
import common.Message;
import common.MessageType;
import common.User;
import util.FileUtil;
import util.ImageUtil;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.WindowConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileNameExtensionFilter;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 主聊天窗口：左侧好友/群聊列表，右侧聊天区与群成员列表。
 */
public class MainFrame extends JFrame implements MessageReceiver.Listener {

    private enum ChatMode {
        FRIEND,
        GROUP
    }

    private static final Color BG = new Color(0xF2F2F2);
    private static final long MAX_FILE_BYTES = 10L * 1024L * 1024L;
    private static final long MAX_VOICE_BYTES = FileTransferManager.MAX_VOICE_BYTES;

    private final ClientConnection connection;
    private final String userId;
    private String nickname;
    private String avatarPath;

    private final JLabel headerAvatarLabel = new JLabel();
    private final JLabel headerTitleLabel = new JLabel();
    private final JButton profileButton = new JButton("个人资料");

    private MessageReceiver receiver;
    private Thread receiverThread;

    private final FriendListPanel friendListPanel = new FriendListPanel();
    private final GroupListPanel groupListPanel = new GroupListPanel();
    private final ChatPanel chatPanel = new ChatPanel();
    private final DefaultListModel<User> memberModel = new DefaultListModel<User>();
    private final JList<User> memberList = new JList<User>(memberModel);
    private final List<String> memberIds = new ArrayList<String>();
    private final List<Boolean> memberIsFriend = new ArrayList<Boolean>();
    private final JLabel chatTitleLabel = new JLabel("请选择好友或群聊");
    private final JButton inviteButton = new JButton("邀请好友");
    private final JButton renameGroupButton = new JButton("修改群名");
    private final JButton addMemberFriendButton = new JButton("加好友");
    private final JPanel memberPanel = new JPanel(new BorderLayout());

    private ChatMode chatMode = ChatMode.FRIEND;
    private String currentGroupId;
    private String currentGroupName;

    private final Map<String, List<ChatItem>> chatHistories = new HashMap<String, List<ChatItem>>();
    private final Map<String, List<ChatItem>> groupHistories = new HashMap<String, List<ChatItem>>();

    public MainFrame(ClientConnection connection, String userId, String nickname) {
        this(connection, userId, nickname, null, null);
    }

    public MainFrame(ClientConnection connection, String userId, String nickname,
                     String initialAvatarPath, byte[] initialAvatarBytes) {
        super("聊天客户端 - " + nickname);
        this.connection = connection;
        this.userId = userId;
        this.nickname = nickname;
        this.avatarPath = initialAvatarPath;
        if (initialAvatarBytes != null && initialAvatarBytes.length > 0) {
            try {
                this.avatarPath = ImageUtil.saveClientAvatar(userId, initialAvatarBytes,
                        initialAvatarPath != null ? new File(initialAvatarPath).getName() : "avatar.png");
            } catch (IOException ignored) {
            }
        }
        initUI();
        updateHeaderDisplay();
        startReceiver();
        requestFriendList();
        requestGroupList();
    }

    private void initUI() {
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setSize(820, 520);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG);
        root.add(buildHeader(), BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildLeftPane(), buildRightPane());
        split.setDividerLocation(200);
        split.setBorder(null);
        root.add(split, BorderLayout.CENTER);

        setContentPane(root);

        friendListPanel.setSelectionListener(new FriendListPanel.SelectionListener() {
            @Override
            public void onFriendSelected(String friendUserId) {
                switchToFriend(friendUserId);
            }
        });

        friendListPanel.setAddFriendListener(new FriendListPanel.AddFriendListener() {
            @Override
            public void onAddFriend(String friendUserId) {
                sendAddFriend(friendUserId);
            }
        });

        groupListPanel.setSelectionListener(new GroupListPanel.SelectionListener() {
            @Override
            public void onGroupSelected(String groupId, String groupName) {
                switchToGroup(groupId, groupName);
            }
        });

        groupListPanel.setCreateGroupListener(new GroupListPanel.CreateGroupListener() {
            @Override
            public void onCreateGroup(String groupName) {
                sendCreateGroup(groupName);
            }
        });

        chatPanel.setSendListener(new ChatPanel.SendListener() {
            @Override
            public void onSend(String text) {
                if (chatMode == ChatMode.GROUP) {
                    sendGroupText(text);
                } else {
                    sendPrivateText(text);
                }
            }
        });

        chatPanel.setImageListener(new ChatPanel.ImageListener() {
            @Override
            public void onPickImage() {
                MainFrame.this.onPickImage();
            }
        });

        chatPanel.setFileListener(new ChatPanel.FileListener() {
            @Override
            public void onPickFile() {
                MainFrame.this.onPickFile();
            }
        });

        chatPanel.setVoiceListener(new ChatPanel.VoiceListener() {
            @Override
            public void onPickVoice() {
                MainFrame.this.onPickVoice();
            }
        });

        chatPanel.setRecordVoiceListener(new ChatPanel.RecordVoiceListener() {
            @Override
            public void onRecordVoice() {
                MainFrame.this.onRecordVoice();
            }
        });

        inviteButton.addActionListener(e -> showInviteFriendsDialog());
        renameGroupButton.addActionListener(e -> showRenameGroupDialog());

        memberList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting()) {
                    return;
                }
                updateAddMemberFriendButton();
            }
        });
        addMemberFriendButton.addActionListener(e -> addFriendFromMemberList());
        addMemberFriendButton.setVisible(false);

        profileButton.addActionListener(e -> showUserProfileDialog());

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                shutdown();
            }
        });
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(0xE6E6E6));
        header.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        JPanel userInfo = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        userInfo.setOpaque(false);
        headerAvatarLabel.setPreferredSize(new Dimension(36, 36));
        userInfo.add(headerAvatarLabel);
        userInfo.add(headerTitleLabel);
        header.add(userInfo, BorderLayout.WEST);

        JPanel headerActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        headerActions.setOpaque(false);
        headerActions.add(profileButton);
        header.add(headerActions, BorderLayout.EAST);
        return header;
    }

    private void updateHeaderDisplay() {
        headerAvatarLabel.setIcon(ImageUtil.avatarForUser(userId, avatarPath, 36));
        headerTitleLabel.setText("当前用户：" + nickname + "（ID：" + userId + "）");
        setTitle("聊天客户端 - " + nickname);
    }

    private void showUserProfileDialog() {
        UserProfileDialog dialog = new UserProfileDialog(this, userId, nickname, avatarPath);
        dialog.setSaveListener(new UserProfileDialog.SaveListener() {
            @Override
            public void onSave(String newNickname, byte[] avatarBytes, String avatarFileName) {
                sendUpdateProfile(newNickname, avatarBytes, avatarFileName);
            }
        });
        dialog.setVisible(true);
    }

    private void sendUpdateProfile(String newNickname, byte[] avatarBytes, String avatarFileName) {
        try {
            Message msg = new Message();
            msg.setType(MessageType.UPDATE_PROFILE);
            msg.setFromUserId(userId);
            msg.setContent(newNickname);
            if (avatarBytes != null && avatarBytes.length > 0) {
                msg.setFileBytes(avatarBytes);
                msg.setFileName(avatarFileName);
            }
            connection.send(msg);
        } catch (IOException ex) {
            appendSystemNotice("[错误] 资料更新失败：" + ex.getMessage());
        }
    }

    private JPanel buildLeftPane() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("好友", friendListPanel);
        tabs.addTab("群聊", groupListPanel);
        tabs.addChangeListener(e -> {
            if (tabs.getSelectedIndex() == 0) {
                String sel = friendListPanel.getSelectedFriendId();
                if (sel != null) {
                    switchToFriend(sel);
                } else {
                    chatMode = ChatMode.FRIEND;
                    currentGroupId = null;
                    currentGroupName = null;
                    inviteButton.setVisible(false);
                    renameGroupButton.setVisible(false);
                    memberPanel.setVisible(false);
                    chatTitleLabel.setText("请选择好友");
                    chatPanel.showConversation(null, Collections.<ChatItem>emptyList());
                }
            } else {
                String sel = groupListPanel.getSelectedGroupId();
                if (sel != null) {
                    switchToGroup(sel, groupListPanel.getSelectedGroupName());
                } else {
                    chatMode = ChatMode.GROUP;
                    currentGroupId = null;
                    currentGroupName = null;
                    inviteButton.setVisible(false);
                    renameGroupButton.setVisible(false);
                    memberPanel.setVisible(false);
                    chatTitleLabel.setText("请选择群聊");
                    chatPanel.showConversation(null, Collections.<ChatItem>emptyList());
                }
            }
        });

        JPanel left = new JPanel(new BorderLayout());
        left.setBackground(BG);
        left.add(tabs, BorderLayout.CENTER);
        left.setPreferredSize(new Dimension(200, 0));
        return left;
    }

    private JPanel buildRightPane() {
        JPanel right = new JPanel(new BorderLayout());
        right.setBackground(BG);

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(new Color(0xE6E6E6));
        topBar.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        topBar.add(chatTitleLabel, BorderLayout.WEST);

        JPanel topActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        topActions.setOpaque(false);
        inviteButton.setVisible(false);
        renameGroupButton.setVisible(false);
        topActions.add(renameGroupButton);
        topActions.add(inviteButton);
        topBar.add(topActions, BorderLayout.EAST);
        right.add(topBar, BorderLayout.NORTH);

        memberPanel.setBackground(BG);
        memberPanel.setBorder(BorderFactory.createTitledBorder("群成员"));
        memberList.setCellRenderer(new UserAvatarListCellRenderer());
        memberPanel.add(new JScrollPane(memberList), BorderLayout.CENTER);

        JPanel memberActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 4));
        memberActions.setOpaque(false);
        memberActions.add(addMemberFriendButton);
        memberPanel.add(memberActions, BorderLayout.SOUTH);

        memberPanel.setPreferredSize(new Dimension(0, 130));
        memberPanel.setVisible(false);

        JSplitPane chatSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, memberPanel, chatPanel);
        chatSplit.setDividerLocation(130);
        chatSplit.setResizeWeight(0.0);
        chatSplit.setBorder(null);
        right.add(chatSplit, BorderLayout.CENTER);

        return right;
    }

    private void startReceiver() {
        receiver = new MessageReceiver(connection, this);
        receiverThread = new Thread(receiver, "message-receiver");
        receiverThread.setDaemon(true);
        receiverThread.start();
    }

    private void requestFriendList() {
        try {
            Message msg = new Message();
            msg.setType(MessageType.FRIEND_LIST);
            msg.setFromUserId(userId);
            connection.send(msg);
        } catch (IOException ex) {
            appendSystemNotice("[错误] 获取好友列表失败：" + ex.getMessage());
        }
    }

    private void requestGroupList() {
        try {
            Message msg = new Message();
            msg.setType(MessageType.GROUP_LIST);
            msg.setFromUserId(userId);
            connection.send(msg);
        } catch (IOException ex) {
            appendSystemNotice("[错误] 获取群聊列表失败：" + ex.getMessage());
        }
    }

    private void requestGroupMemberList(String groupId) {
        try {
            Message msg = new Message();
            msg.setType(MessageType.GROUP_MEMBER_LIST);
            msg.setFromUserId(userId);
            msg.setGroupId(groupId);
            connection.send(msg);
        } catch (IOException ex) {
            appendSystemNotice("[错误] 获取群成员列表失败：" + ex.getMessage());
        }
    }

    private void sendAddFriend(String friendUserId) {
        if (friendUserId == null || friendUserId.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "请填写要添加的好友 ID。", "提示",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        friendUserId = friendUserId.trim();
        if (friendUserId.equals(userId)) {
            JOptionPane.showMessageDialog(this, "不能添加自己为好友。", "提示",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (friendListPanel.containsFriend(friendUserId)) {
            JOptionPane.showMessageDialog(this, "用户 [" + friendUserId + "] 已是你的好友。", "提示",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        try {
            Message msg = new Message();
            msg.setType(MessageType.ADD_FRIEND);
            msg.setFromUserId(userId);
            msg.setToUserId(friendUserId);
            connection.send(msg);
        } catch (IOException ex) {
            appendSystemNotice("[错误] 添加好友失败：" + ex.getMessage());
        }
    }

    private void sendCreateGroup(String groupName) {
        try {
            Message msg = new Message();
            msg.setType(MessageType.CREATE_GROUP);
            msg.setFromUserId(userId);
            msg.setContent(groupName);
            connection.send(msg);
        } catch (IOException ex) {
            appendSystemNotice("[错误] 创建群聊失败：" + ex.getMessage());
        }
    }

    private void showRenameGroupDialog() {
        if (chatMode != ChatMode.GROUP || currentGroupId == null) {
            JOptionPane.showMessageDialog(this, "请先选择一个群聊。", "提示",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String newName = JOptionPane.showInputDialog(this,
                "请输入新的群名称：", "修改群名称",
                JOptionPane.PLAIN_MESSAGE);
        if (newName == null) {
            return;
        }
        newName = newName.trim();
        if (newName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "群名称不能为空。", "提示",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (newName.equals(currentGroupName)) {
            return;
        }
        sendRenameGroup(newName);
    }

    private void sendRenameGroup(String newGroupName) {
        if (currentGroupId == null) {
            return;
        }
        try {
            Message msg = new Message();
            msg.setType(MessageType.RENAME_GROUP);
            msg.setFromUserId(userId);
            msg.setGroupId(currentGroupId);
            msg.setContent(newGroupName);
            connection.send(msg);
        } catch (IOException ex) {
            appendSystemNotice("[错误] 修改群名称失败：" + ex.getMessage());
        }
    }

    private void showInviteFriendsDialog() {
        if (chatMode != ChatMode.GROUP || currentGroupId == null) {
            JOptionPane.showMessageDialog(this, "请先选择一个群聊。", "提示",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        List<User> friends = friendListPanel.getFriendUsers();
        if (friends.isEmpty()) {
            JOptionPane.showMessageDialog(this, "你还没有好友可邀请。", "提示",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        InviteFriendsDialog dialog = new InviteFriendsDialog(this, friends);
        dialog.setInviteListener(new InviteFriendsDialog.InviteListener() {
            @Override
            public void onInvite(List<String> friendUserIds) {
                sendInviteToGroup(friendUserIds);
            }
        });
        dialog.setVisible(true);
    }

    private void sendInviteToGroup(List<String> friendUserIds) {
        if (currentGroupId == null || friendUserIds == null || friendUserIds.isEmpty()) {
            return;
        }
        try {
            Message msg = new Message();
            msg.setType(MessageType.INVITE_TO_GROUP);
            msg.setFromUserId(userId);
            msg.setGroupId(currentGroupId);
            if (friendUserIds.size() == 1) {
                msg.setToUserId(friendUserIds.get(0));
            } else {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < friendUserIds.size(); i++) {
                    if (i > 0) {
                        sb.append(',');
                    }
                    sb.append(friendUserIds.get(i));
                }
                msg.setExtra(sb.toString());
            }
            connection.send(msg);
        } catch (IOException ex) {
            appendSystemNotice("[错误] 邀请好友失败：" + ex.getMessage());
        }
    }

    private void switchToFriend(String friendUserId) {
        chatMode = ChatMode.FRIEND;
        currentGroupId = null;
        currentGroupName = null;
        inviteButton.setVisible(false);
        renameGroupButton.setVisible(false);
        memberPanel.setVisible(false);
        chatTitleLabel.setText("私聊：" + friendUserId);
        chatPanel.showConversation(friendUserId, getFriendHistory(friendUserId));
    }

    private void switchToGroup(String groupId, String groupName) {
        chatMode = ChatMode.GROUP;
        currentGroupId = groupId;
        currentGroupName = groupName != null ? groupName : groupId;
        inviteButton.setVisible(true);
        renameGroupButton.setVisible(true);
        memberPanel.setVisible(true);
        chatTitleLabel.setText("群聊：" + currentGroupName + "（ID：" + groupId + "）");
        chatPanel.showConversation(groupId, getGroupHistory(groupId));
        requestGroupMemberList(groupId);
    }

    private void sendPrivateText(String text) {
        String peerId = friendListPanel.getSelectedFriendId();
        if (peerId == null || peerId.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请先选择或添加一位好友。", "提示",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (peerId.equals(userId)) {
            JOptionPane.showMessageDialog(this, "不能给自己发送私聊消息。", "提示",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            Message msg = Message.text(userId, peerId, text);
            connection.send(msg);
            long now = System.currentTimeMillis();
            appendFriendChatMessage(peerId, "我", now, text);
            chatPanel.clearInput();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "发送失败：" + ex.getMessage(), "发送错误",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void sendGroupText(String text) {
        if (currentGroupId == null) {
            JOptionPane.showMessageDialog(this, "请先选择一个群聊。", "提示",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        try {
            Message msg = new Message();
            msg.setType(MessageType.GROUP_TEXT);
            msg.setFromUserId(userId);
            msg.setGroupId(currentGroupId);
            msg.setContent(text);
            connection.send(msg);
            long now = System.currentTimeMillis();
            appendGroupChatMessage(currentGroupId, "我", now, text);
            chatPanel.clearInput();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "发送失败：" + ex.getMessage(), "发送错误",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    public void appendSystemMessage(String content) {
        appendSystemNotice(content);
    }

    private void appendSystemNotice(String content) {
        ChatItem item = ChatItem.system(content);
        if (chatMode == ChatMode.GROUP && currentGroupId != null) {
            appendToGroupHistory(currentGroupId, item);
        } else {
            String peerId = chatPanel.getPeerUserId();
            if (peerId != null) {
                appendToFriendHistory(peerId, item);
            }
        }
        chatPanel.appendSystemMessage(content);
    }

    private void appendFriendChatMessage(String peerId, String senderLabel, long timestamp, String content) {
        ChatItem item = ChatItem.text(senderLabel, timestamp, content);
        appendToFriendHistory(peerId, item);
        if (chatMode == ChatMode.FRIEND && peerId.equals(chatPanel.getPeerUserId())) {
            chatPanel.appendChatLine(senderLabel, timestamp, content);
        }
    }

    private void appendGroupChatMessage(String groupId, String senderLabel, long timestamp, String content) {
        ChatItem item = ChatItem.text(senderLabel, timestamp, content);
        appendToGroupHistory(groupId, item);
        if (chatMode == ChatMode.GROUP && groupId.equals(currentGroupId)) {
            chatPanel.appendChatLine(senderLabel, timestamp, content);
        }
    }

    private void appendFriendImageMessage(String peerId, String senderLabel, long timestamp,
                                          javax.swing.ImageIcon thumbnail, byte[] fullImage, String fileName) {
        ChatItem item = ChatItem.image(senderLabel, timestamp, thumbnail, fullImage, fileName);
        appendToFriendHistory(peerId, item);
        if (chatMode == ChatMode.FRIEND && peerId.equals(chatPanel.getPeerUserId())) {
            chatPanel.appendImageLine(senderLabel, timestamp, thumbnail, fullImage, fileName);
        }
    }

    private void appendGroupImageMessage(String groupId, String senderLabel, long timestamp,
                                         javax.swing.ImageIcon thumbnail, byte[] fullImage, String fileName) {
        ChatItem item = ChatItem.image(senderLabel, timestamp, thumbnail, fullImage, fileName);
        appendToGroupHistory(groupId, item);
        if (chatMode == ChatMode.GROUP && groupId.equals(currentGroupId)) {
            chatPanel.appendImageLine(senderLabel, timestamp, thumbnail, fullImage, fileName);
        }
    }

    private void appendFriendFileMessage(String peerId, String senderLabel, long timestamp,
                                         String fileName, long fileSize, byte[] fileBytes) {
        ChatItem item = ChatItem.file(senderLabel, timestamp, fileName, fileSize, fileBytes);
        appendToFriendHistory(peerId, item);
        if (chatMode == ChatMode.FRIEND && peerId.equals(chatPanel.getPeerUserId())) {
            chatPanel.appendFileLine(senderLabel, timestamp, fileName, fileSize, fileBytes);
        }
    }

    private void appendGroupFileMessage(String groupId, String senderLabel, long timestamp,
                                        String fileName, long fileSize, byte[] fileBytes) {
        ChatItem item = ChatItem.file(senderLabel, timestamp, fileName, fileSize, fileBytes);
        appendToGroupHistory(groupId, item);
        if (chatMode == ChatMode.GROUP && groupId.equals(currentGroupId)) {
            chatPanel.appendFileLine(senderLabel, timestamp, fileName, fileSize, fileBytes);
        }
    }

    private void appendFriendVoiceMessage(String peerId, String senderLabel, long timestamp,
                                          String fileName, long fileSize, byte[] voiceBytes) {
        ChatItem item = ChatItem.voice(senderLabel, timestamp, fileName, fileSize, voiceBytes);
        appendToFriendHistory(peerId, item);
        if (chatMode == ChatMode.FRIEND && peerId.equals(chatPanel.getPeerUserId())) {
            chatPanel.appendVoiceLine(senderLabel, timestamp, fileName, fileSize, voiceBytes);
        }
    }

    private void appendGroupVoiceMessage(String groupId, String senderLabel, long timestamp,
                                         String fileName, long fileSize, byte[] voiceBytes) {
        ChatItem item = ChatItem.voice(senderLabel, timestamp, fileName, fileSize, voiceBytes);
        appendToGroupHistory(groupId, item);
        if (chatMode == ChatMode.GROUP && groupId.equals(currentGroupId)) {
            chatPanel.appendVoiceLine(senderLabel, timestamp, fileName, fileSize, voiceBytes);
        }
    }

    private void appendToFriendHistory(String peerId, ChatItem item) {
        List<ChatItem> history = chatHistories.get(peerId);
        if (history == null) {
            history = new ArrayList<ChatItem>();
            chatHistories.put(peerId, history);
        }
        history.add(item);
    }

    private void appendToGroupHistory(String groupId, ChatItem item) {
        List<ChatItem> history = groupHistories.get(groupId);
        if (history == null) {
            history = new ArrayList<ChatItem>();
            groupHistories.put(groupId, history);
        }
        history.add(item);
    }

    private List<ChatItem> getFriendHistory(String peerId) {
        List<ChatItem> history = chatHistories.get(peerId);
        return history != null ? new ArrayList<ChatItem>(history) : new ArrayList<ChatItem>();
    }

    private List<ChatItem> getGroupHistory(String groupId) {
        List<ChatItem> history = groupHistories.get(groupId);
        return history != null ? new ArrayList<ChatItem>(history) : new ArrayList<ChatItem>();
    }

    private String resolvePeerId(Message message) {
        if (message.getFromUserId() != null && message.getFromUserId().equals(userId)) {
            return message.getToUserId();
        }
        return message.getFromUserId();
    }

    private String resolveSenderLabel(Message message) {
        if (message.getFromUserId() != null && message.getFromUserId().equals(userId)) {
            return "我";
        }
        return message.getFromUserId();
    }

    @Override
    public void onMessage(Message message) {
        if (message == null || message.getType() == null) {
            return;
        }
        switch (message.getType()) {
            case PRIVATE_TEXT:
                handlePrivateText(message);
                break;
            case GROUP_TEXT:
                handleGroupText(message);
                break;
            case PRIVATE_IMAGE:
                handlePrivateImage(message);
                break;
            case GROUP_IMAGE:
                handleGroupImage(message);
                break;
            case PRIVATE_FILE:
                handlePrivateFile(message);
                break;
            case GROUP_FILE:
                handleGroupFile(message);
                break;
            case PRIVATE_VOICE:
                handlePrivateVoice(message);
                break;
            case GROUP_VOICE:
                handleGroupVoice(message);
                break;
            case SERVER_NOTICE:
                handleServerNotice(message);
                break;
            case FRIEND_LIST:
                handleFriendList(message);
                break;
            case GROUP_LIST:
                handleGroupList(message);
                break;
            case GROUP_MEMBER_LIST:
                handleGroupMemberList(message);
                break;
            case UPDATE_PROFILE:
                handleUpdateProfile(message);
                break;
            case ERROR:
                JOptionPane.showMessageDialog(this, message.getContent(), "操作失败",
                        JOptionPane.WARNING_MESSAGE);
                break;
            default:
                appendSystemNotice("收到消息：" + message);
                break;
        }
    }

    private void handlePrivateText(Message message) {
        String peerId = resolvePeerId(message);
        if (peerId == null || peerId.trim().isEmpty()) {
            return;
        }
        friendListPanel.ensureFriend(peerId);
        String sender = resolveSenderLabel(message);
        long ts = message.getTimestamp() > 0 ? message.getTimestamp() : System.currentTimeMillis();
        appendFriendChatMessage(peerId, sender, ts, message.getContent());
    }

    private void handleGroupText(Message message) {
        if (message.getFromUserId() != null && message.getFromUserId().equals(userId)) {
            return;
        }
        String groupId = message.getGroupId();
        if (groupId == null || groupId.trim().isEmpty()) {
            return;
        }
        String sender = resolveSenderLabel(message);
        long ts = message.getTimestamp() > 0 ? message.getTimestamp() : System.currentTimeMillis();
        appendGroupChatMessage(groupId, sender, ts, message.getContent());
    }

    private void handlePrivateImage(Message message) {
        String peerId = resolvePeerId(message);
        if (peerId == null || peerId.trim().isEmpty()) {
            return;
        }
        byte[] data = message.getFileBytes();
        if (data == null || data.length == 0) {
            appendSystemNotice("[错误] 收到的图片数据为空。");
            return;
        }
        friendListPanel.ensureFriend(peerId);
        String sender = resolveSenderLabel(message);
        long ts = message.getTimestamp() > 0 ? message.getTimestamp() : System.currentTimeMillis();
        javax.swing.ImageIcon thumbnail = ImageUtil.toThumbnail(data);
        appendFriendImageMessage(peerId, sender, ts, thumbnail, data, message.getFileName());
    }

    private void handleGroupImage(Message message) {
        String groupId = message.getGroupId();
        if (groupId == null || groupId.trim().isEmpty()) {
            return;
        }
        byte[] data = message.getFileBytes();
        if (data == null || data.length == 0) {
            appendSystemNotice("[错误] 收到的群图片数据为空。");
            return;
        }
        String sender = resolveSenderLabel(message);
        long ts = message.getTimestamp() > 0 ? message.getTimestamp() : System.currentTimeMillis();
        javax.swing.ImageIcon thumbnail = ImageUtil.toThumbnail(data);
        appendGroupImageMessage(groupId, sender, ts, thumbnail, data, message.getFileName());
    }

    private void handlePrivateFile(Message message) {
        String peerId = resolvePeerId(message);
        if (peerId == null || peerId.trim().isEmpty()) {
            return;
        }
        byte[] data = message.getFileBytes();
        if (data == null || data.length == 0) {
            appendSystemNotice("[错误] 收到的文件数据为空。");
            return;
        }
        friendListPanel.ensureFriend(peerId);
        String sender = resolveSenderLabel(message);
        long ts = message.getTimestamp() > 0 ? message.getTimestamp() : System.currentTimeMillis();
        long size = message.getFileSize() > 0 ? message.getFileSize() : data.length;
        appendFriendFileMessage(peerId, sender, ts, message.getFileName(), size, data);
    }

    private void handleGroupFile(Message message) {
        String groupId = message.getGroupId();
        if (groupId == null || groupId.trim().isEmpty()) {
            return;
        }
        byte[] data = message.getFileBytes();
        if (data == null || data.length == 0) {
            appendSystemNotice("[错误] 收到的群文件数据为空。");
            return;
        }
        String sender = resolveSenderLabel(message);
        long ts = message.getTimestamp() > 0 ? message.getTimestamp() : System.currentTimeMillis();
        long size = message.getFileSize() > 0 ? message.getFileSize() : data.length;
        appendGroupFileMessage(groupId, sender, ts, message.getFileName(), size, data);
    }

    private void handlePrivateVoice(Message message) {
        String peerId = resolvePeerId(message);
        if (peerId == null || peerId.trim().isEmpty()) {
            return;
        }
        byte[] data = message.getFileBytes();
        if (data == null || data.length == 0) {
            appendSystemNotice("[错误] 收到的语音数据为空。");
            return;
        }
        friendListPanel.ensureFriend(peerId);
        String sender = resolveSenderLabel(message);
        long ts = message.getTimestamp() > 0 ? message.getTimestamp() : System.currentTimeMillis();
        long size = message.getFileSize() > 0 ? message.getFileSize() : data.length;
        appendFriendVoiceMessage(peerId, sender, ts, message.getFileName(), size, data);
    }

    private void handleGroupVoice(Message message) {
        String groupId = message.getGroupId();
        if (groupId == null || groupId.trim().isEmpty()) {
            return;
        }
        byte[] data = message.getFileBytes();
        if (data == null || data.length == 0) {
            appendSystemNotice("[错误] 收到的群语音数据为空。");
            return;
        }
        String sender = resolveSenderLabel(message);
        long ts = message.getTimestamp() > 0 ? message.getTimestamp() : System.currentTimeMillis();
        long size = message.getFileSize() > 0 ? message.getFileSize() : data.length;
        appendGroupVoiceMessage(groupId, sender, ts, message.getFileName(), size, data);
    }

    private void onPickImage() {
        if (chatMode == ChatMode.GROUP) {
            sendGroupImage();
        } else {
            sendPrivateImage();
        }
    }

    private void onPickFile() {
        if (chatMode == ChatMode.GROUP) {
            sendGroupFile();
        } else {
            sendPrivateFile();
        }
    }

    private void onPickVoice() {
        if (chatMode == ChatMode.GROUP) {
            sendGroupVoice();
        } else {
            sendPrivateVoice();
        }
    }

    private void onRecordVoice() {
        if (chatMode == ChatMode.GROUP) {
            if (currentGroupId == null) {
                JOptionPane.showMessageDialog(this, "请先选择一个群聊。", "提示",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            showRecordDialog(true, null);
        } else {
            String peerId = friendListPanel.getSelectedFriendId();
            if (peerId == null || peerId.isEmpty()) {
                JOptionPane.showMessageDialog(this, "请先选择或添加一位好友。", "提示",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            if (peerId.equals(userId)) {
                JOptionPane.showMessageDialog(this, "不能给自己发送私聊语音。", "提示",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            showRecordDialog(false, peerId);
        }
    }

    private void showRecordDialog(final boolean isGroup, final String peerId) {
        VoiceRecordDialog dialog = new VoiceRecordDialog(this);
        dialog.setSendListener(new VoiceRecordDialog.SendListener() {
            @Override
            public void onSend(byte[] wavBytes, String fileName) {
                if (isGroup) {
                    sendGroupVoiceBytes(wavBytes, fileName);
                } else {
                    sendPrivateVoiceBytes(peerId, wavBytes, fileName);
                }
            }
        });
        dialog.setVisible(true);
    }

    private void sendPrivateImage() {
        String peerId = friendListPanel.getSelectedFriendId();
        if (peerId == null || peerId.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请先选择或添加一位好友。", "提示",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (peerId.equals(userId)) {
            JOptionPane.showMessageDialog(this, "不能给自己发送私聊图片。", "提示",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        File file = chooseImageFile();
        if (file == null) {
            return;
        }
        try {
            if (file.length() > ImageUtil.MAX_IMAGE_BYTES) {
                JOptionPane.showMessageDialog(this,
                        "图片过大，最大允许 " + FileUtil.humanSize(ImageUtil.MAX_IMAGE_BYTES) + "。",
                        "错误", JOptionPane.ERROR_MESSAGE);
                return;
            }
            FilePacket packet = ImageUtil.readImage(file);
            Message msg = new Message();
            msg.setType(MessageType.PRIVATE_IMAGE);
            msg.setFromUserId(userId);
            msg.setToUserId(peerId);
            msg.setFileName(packet.getFileName());
            msg.setFileSize(packet.getFileSize());
            msg.setFileType(packet.getFileType());
            msg.setFileBytes(packet.getData());
            connection.send(msg);
            long now = System.currentTimeMillis();
            javax.swing.ImageIcon thumbnail = ImageUtil.toThumbnail(packet.getData());
            appendFriendImageMessage(peerId, "我", now, thumbnail, packet.getData(), packet.getFileName());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "发送失败：" + ex.getMessage(), "发送错误",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void sendGroupImage() {
        if (currentGroupId == null) {
            JOptionPane.showMessageDialog(this, "请先选择一个群聊。", "提示",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        File file = chooseImageFile();
        if (file == null) {
            return;
        }
        try {
            if (file.length() > ImageUtil.MAX_IMAGE_BYTES) {
                JOptionPane.showMessageDialog(this,
                        "图片过大，最大允许 " + FileUtil.humanSize(ImageUtil.MAX_IMAGE_BYTES) + "。",
                        "错误", JOptionPane.ERROR_MESSAGE);
                return;
            }
            FilePacket packet = ImageUtil.readImage(file);
            Message msg = new Message();
            msg.setType(MessageType.GROUP_IMAGE);
            msg.setFromUserId(userId);
            msg.setGroupId(currentGroupId);
            msg.setFileName(packet.getFileName());
            msg.setFileSize(packet.getFileSize());
            msg.setFileType(packet.getFileType());
            msg.setFileBytes(packet.getData());
            connection.send(msg);
            long now = System.currentTimeMillis();
            javax.swing.ImageIcon thumbnail = ImageUtil.toThumbnail(packet.getData());
            appendGroupImageMessage(currentGroupId, "我", now, thumbnail, packet.getData(), packet.getFileName());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "发送失败：" + ex.getMessage(), "发送错误",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void sendPrivateFile() {
        String peerId = friendListPanel.getSelectedFriendId();
        if (peerId == null || peerId.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请先选择或添加一位好友。", "提示",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (peerId.equals(userId)) {
            JOptionPane.showMessageDialog(this, "不能给自己发送私聊文件。", "提示",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        File file = chooseFile();
        if (file == null) {
            return;
        }
        try {
            if (file.length() > MAX_FILE_BYTES) {
                JOptionPane.showMessageDialog(this,
                        "文件过大，最大允许 " + FileUtil.humanSize(MAX_FILE_BYTES) + "。",
                        "错误", JOptionPane.ERROR_MESSAGE);
                return;
            }
            byte[] data = FileUtil.readFileBytes(file);
            String ext = FileUtil.extensionOf(file.getName());
            Message msg = new Message();
            msg.setType(MessageType.PRIVATE_FILE);
            msg.setFromUserId(userId);
            msg.setToUserId(peerId);
            msg.setFileName(file.getName());
            msg.setFileSize(data.length);
            msg.setFileType(ext);
            msg.setFileBytes(data);
            connection.send(msg);
            long now = System.currentTimeMillis();
            appendFriendFileMessage(peerId, "我", now, file.getName(), data.length, data);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "发送失败：" + ex.getMessage(), "发送错误",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void sendGroupFile() {
        if (currentGroupId == null) {
            JOptionPane.showMessageDialog(this, "请先选择一个群聊。", "提示",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        File file = chooseFile();
        if (file == null) {
            return;
        }
        try {
            if (file.length() > MAX_FILE_BYTES) {
                JOptionPane.showMessageDialog(this,
                        "文件过大，最大允许 " + FileUtil.humanSize(MAX_FILE_BYTES) + "。",
                        "错误", JOptionPane.ERROR_MESSAGE);
                return;
            }
            byte[] data = FileUtil.readFileBytes(file);
            String ext = FileUtil.extensionOf(file.getName());
            Message msg = new Message();
            msg.setType(MessageType.GROUP_FILE);
            msg.setFromUserId(userId);
            msg.setGroupId(currentGroupId);
            msg.setFileName(file.getName());
            msg.setFileSize(data.length);
            msg.setFileType(ext);
            msg.setFileBytes(data);
            connection.send(msg);
            long now = System.currentTimeMillis();
            appendGroupFileMessage(currentGroupId, "我", now, file.getName(), data.length, data);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "发送失败：" + ex.getMessage(), "发送错误",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private File chooseImageFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("选择图片");
        chooser.setFileFilter(new FileNameExtensionFilter("图片文件 (jpg, png, gif)", "jpg", "jpeg", "png", "gif"));
        int result = chooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return null;
        }
        File file = chooser.getSelectedFile();
        if (file == null || !ImageUtil.isImageFile(file)) {
            JOptionPane.showMessageDialog(this, "请选择 jpg/png/gif 格式的图片。", "提示",
                    JOptionPane.WARNING_MESSAGE);
            return null;
        }
        return file;
    }

    private File chooseFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("选择文件");
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        int result = chooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return null;
        }
        File file = chooser.getSelectedFile();
        if (file == null || !file.isFile()) {
            JOptionPane.showMessageDialog(this, "请选择有效的文件。", "提示",
                    JOptionPane.WARNING_MESSAGE);
            return null;
        }
        return file;
    }

    private void sendPrivateVoice() {
        String peerId = friendListPanel.getSelectedFriendId();
        if (peerId == null || peerId.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请先选择或添加一位好友。", "提示",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (peerId.equals(userId)) {
            JOptionPane.showMessageDialog(this, "不能给自己发送私聊语音。", "提示",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        File file = chooseVoiceFile();
        if (file == null) {
            return;
        }
        try {
            if (file.length() > MAX_VOICE_BYTES) {
                JOptionPane.showMessageDialog(this,
                        "语音文件过大，最大允许 " + FileUtil.humanSize(MAX_VOICE_BYTES) + "。",
                        "错误", JOptionPane.ERROR_MESSAGE);
                return;
            }
            byte[] data = FileUtil.readFileBytes(file);
            sendPrivateVoiceBytes(peerId, data, file.getName());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "发送失败：" + ex.getMessage(), "发送错误",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void sendGroupVoice() {
        if (currentGroupId == null) {
            JOptionPane.showMessageDialog(this, "请先选择一个群聊。", "提示",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        File file = chooseVoiceFile();
        if (file == null) {
            return;
        }
        try {
            if (file.length() > MAX_VOICE_BYTES) {
                JOptionPane.showMessageDialog(this,
                        "语音文件过大，最大允许 " + FileUtil.humanSize(MAX_VOICE_BYTES) + "。",
                        "错误", JOptionPane.ERROR_MESSAGE);
                return;
            }
            byte[] data = FileUtil.readFileBytes(file);
            sendGroupVoiceBytes(data, file.getName());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "发送失败：" + ex.getMessage(), "发送错误",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void sendPrivateVoiceBytes(String peerId, byte[] data, String fileName) {
        try {
            String ext = FileUtil.extensionOf(fileName);
            Message msg = new Message();
            msg.setType(MessageType.PRIVATE_VOICE);
            msg.setFromUserId(userId);
            msg.setToUserId(peerId);
            msg.setFileName(fileName);
            msg.setFileSize(data.length);
            msg.setFileType(ext);
            msg.setFileBytes(data);
            connection.send(msg);
            long now = System.currentTimeMillis();
            appendFriendVoiceMessage(peerId, "我", now, fileName, data.length, data);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "发送失败：" + ex.getMessage(), "发送错误",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void sendGroupVoiceBytes(byte[] data, String fileName) {
        try {
            String ext = FileUtil.extensionOf(fileName);
            Message msg = new Message();
            msg.setType(MessageType.GROUP_VOICE);
            msg.setFromUserId(userId);
            msg.setGroupId(currentGroupId);
            msg.setFileName(fileName);
            msg.setFileSize(data.length);
            msg.setFileType(ext);
            msg.setFileBytes(data);
            connection.send(msg);
            long now = System.currentTimeMillis();
            appendGroupVoiceMessage(currentGroupId, "我", now, fileName, data.length, data);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "发送失败：" + ex.getMessage(), "发送错误",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private File chooseVoiceFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("选择语音文件");
        chooser.setFileFilter(new FileNameExtensionFilter("音频文件 (wav, mp3)", "wav", "mp3"));
        int result = chooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return null;
        }
        File file = chooser.getSelectedFile();
        if (file == null || !file.isFile()) {
            JOptionPane.showMessageDialog(this, "请选择有效的音频文件。", "提示",
                    JOptionPane.WARNING_MESSAGE);
            return null;
        }
        String ext = FileUtil.extensionOf(file.getName());
        if (!"wav".equals(ext) && !"mp3".equals(ext)) {
            JOptionPane.showMessageDialog(this, "请选择 wav 或 mp3 格式的音频文件。", "提示",
                    JOptionPane.WARNING_MESSAGE);
            return null;
        }
        return file;
    }

    private void handleServerNotice(Message message) {
        appendSystemNotice(message.getContent());
    }

    private void handleUpdateProfile(Message message) {
        String targetId = message.getFromUserId();
        if (targetId == null) {
            return;
        }
        String newNickname = message.getContent();
        String newAvatarPath = message.getExtra();
        byte[] avatarBytes = message.getFileBytes();

        if (targetId.equals(userId)) {
            if (newNickname != null && !newNickname.trim().isEmpty()) {
                nickname = newNickname.trim();
            }
            if (avatarBytes != null && avatarBytes.length > 0) {
                try {
                    avatarPath = ImageUtil.saveClientAvatar(userId, avatarBytes, message.getFileName());
                } catch (IOException ex) {
                    appendSystemNotice("[错误] 头像保存失败：" + ex.getMessage());
                }
            } else if (newAvatarPath != null && !newAvatarPath.trim().isEmpty()) {
                avatarPath = newAvatarPath.trim();
            }
            updateHeaderDisplay();
            appendSystemNotice("个人资料已更新。");
        } else {
            if (avatarBytes != null && avatarBytes.length > 0) {
                try {
                    newAvatarPath = ImageUtil.saveClientAvatar(targetId, avatarBytes, message.getFileName());
                } catch (IOException ignored) {
                }
            }
            friendListPanel.updateFriendProfile(targetId, newNickname, newAvatarPath);
            refreshMemberAvatars(targetId, newNickname, newAvatarPath);
        }
    }

    private void refreshMemberAvatars(String memberId, String nickname, String avatarPath) {
        for (int i = 0; i < memberModel.size(); i++) {
            User user = memberModel.get(i);
            if (user != null && memberId.equals(user.getUserId())) {
                if (nickname != null && !nickname.trim().isEmpty()) {
                    user.setNickname(nickname.trim());
                }
                if (avatarPath != null && !avatarPath.trim().isEmpty()) {
                    user.setAvatarPath(avatarPath.trim());
                }
                memberModel.set(i, user);
            }
        }
    }

    private void handleFriendList(Message message) {
        List<User> friends = parseUserList(message.getContent());
        friendListPanel.setFriendUsers(friends);
        updateAddMemberFriendButton();
    }

    private void handleGroupList(Message message) {
        List<Group> groups = parseGroupList(message.getContent());
        groupListPanel.setGroups(groups);
        if (currentGroupId != null) {
            for (Group group : groups) {
                if (group != null && currentGroupId.equals(group.getGroupId())) {
                    currentGroupName = group.getGroupName();
                    if (chatMode == ChatMode.GROUP) {
                        chatTitleLabel.setText("群聊：" + currentGroupName + "（ID：" + currentGroupId + "）");
                    }
                    break;
                }
            }
        }
    }

    private void handleGroupMemberList(Message message) {
        String groupId = message.getGroupId();
        if (groupId == null) {
            return;
        }
        if (chatMode == ChatMode.GROUP && groupId.equals(currentGroupId)) {
            updateMemberList(parseGroupMemberList(message.getContent()));
        }
    }

    private void updateMemberList(List<GroupMemberEntry> members) {
        memberModel.clear();
        memberIds.clear();
        memberIsFriend.clear();
        if (members != null) {
            for (GroupMemberEntry entry : members) {
                if (entry == null || entry.userId == null) {
                    continue;
                }
                String id = entry.userId;
                User user = new User(id, entry.nickname);
                user.setOnline(entry.online);
                user.setAvatarPath(entry.avatarPath);
                memberIds.add(id);
                memberIsFriend.add(entry.isFriend);
                memberModel.addElement(user);
            }
        }
        updateAddMemberFriendButton();
    }

    private void addFriendFromMemberList() {
        int index = memberList.getSelectedIndex();
        if (index < 0 || index >= memberIds.size()) {
            return;
        }
        String targetId = memberIds.get(index);
        if (!canAddMemberAsFriend(index)) {
            return;
        }
        sendAddFriend(targetId);
    }

    private void updateAddMemberFriendButton() {
        if (chatMode != ChatMode.GROUP || !memberPanel.isVisible()) {
            addMemberFriendButton.setVisible(false);
            return;
        }
        int index = memberList.getSelectedIndex();
        boolean show = canAddMemberAsFriend(index);
        addMemberFriendButton.setVisible(show);
        addMemberFriendButton.setEnabled(show);
    }

    /** 选中成员可添加为好友：非自己、非已是好友（服务端标记与本地好友列表双重校验）。 */
    private boolean canAddMemberAsFriend(int index) {
        if (index < 0 || index >= memberIds.size()) {
            return false;
        }
        String memberId = memberIds.get(index);
        if (memberId == null || memberId.equals(userId)) {
            return false;
        }
        if (index < memberIsFriend.size() && memberIsFriend.get(index)) {
            return false;
        }
        return !friendListPanel.containsFriend(memberId);
    }

    private static final class GroupMemberEntry {
        private final String userId;
        private final String nickname;
        private final boolean online;
        private final boolean isFriend;
        private final String avatarPath;

        private GroupMemberEntry(String userId, String nickname, boolean online, boolean isFriend,
                                 String avatarPath) {
            this.userId = userId;
            this.nickname = nickname;
            this.online = online;
            this.isFriend = isFriend;
            this.avatarPath = avatarPath;
        }
    }

    private List<GroupMemberEntry> parseGroupMemberList(String content) {
        List<GroupMemberEntry> result = new ArrayList<GroupMemberEntry>();
        if (content == null || content.trim().isEmpty()) {
            return result;
        }
        String[] lines = content.split("\n");
        for (String line : lines) {
            if (line == null || line.trim().isEmpty()) {
                continue;
            }
            String[] parts = line.split("\u0001", -1);
            if (parts.length < 2) {
                continue;
            }
            String id = parts[0].trim();
            String nickname = parts[1];
            boolean online = parts.length >= 3 && "1".equals(parts[2]);
            boolean isFriend = parts.length >= 4 && "1".equals(parts[3]);
            String avatarPath = parts.length >= 5 ? parts[4] : "";
            result.add(new GroupMemberEntry(id, nickname, online, isFriend, avatarPath));
        }
        return result;
    }

    private List<User> parseUserList(String content) {
        List<User> result = new ArrayList<User>();
        if (content == null || content.trim().isEmpty()) {
            return result;
        }
        String[] lines = content.split("\n");
        for (String line : lines) {
            if (line == null || line.trim().isEmpty()) {
                continue;
            }
            String[] parts = line.split("\u0001", -1);
            if (parts.length < 2) {
                continue;
            }
            String id = parts[0].trim();
            String nickname = parts[1];
            boolean online = parts.length >= 3 && "1".equals(parts[2]);
            String avatarPath = parts.length >= 4 ? parts[3] : "";
            User user = new User(id, nickname);
            user.setOnline(online);
            user.setAvatarPath(avatarPath);
            result.add(user);
        }
        return result;
    }

    private List<Group> parseGroupList(String content) {
        List<Group> result = new ArrayList<Group>();
        if (content == null || content.trim().isEmpty()) {
            return result;
        }
        String[] lines = content.split("\n");
        for (String line : lines) {
            if (line == null || line.trim().isEmpty()) {
                continue;
            }
            String[] parts = line.split("\u0001", -1);
            if (parts.length < 2) {
                continue;
            }
            Group group = new Group();
            group.setGroupId(parts[0].trim());
            group.setGroupName(parts[1]);
            result.add(group);
        }
        return result;
    }

    @Override
    public void onDisconnected() {
        appendSystemNotice("与服务器的连接已断开。");
        chatPanel.setInputEnabled(false);
        inviteButton.setEnabled(false);
        renameGroupButton.setEnabled(false);
        addMemberFriendButton.setEnabled(false);
        int choice = JOptionPane.showConfirmDialog(this,
                "与服务器的连接已断开。\n是否重新登录？",
                "连接断开", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice == JOptionPane.YES_OPTION) {
            if (receiver != null) {
                receiver.stop();
            }
            connection.close();
            new LoginFrame().setVisible(true);
            dispose();
        }
    }

    private void shutdown() {
        if (receiver != null) {
            receiver.stop();
        }
        try {
            Message logout = new Message();
            logout.setType(MessageType.LOGOUT);
            logout.setFromUserId(userId);
            connection.send(logout);
        } catch (IOException ignored) {
        }
        connection.close();
        dispose();
        System.exit(0);
    }
}
