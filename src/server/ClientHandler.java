package server;

import common.Message;
import common.MessageType;
import common.Protocol;
import common.User;
import common.Group;

import util.FileUtil;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 单个客户端连接的处理线程。
 *
 * 职责：
 * - 持续读取客户端发来的 {@link Message}。
 * - 处理登录（LOGIN）与注册（REGISTER）请求。
 * - 转发私聊文字（PRIVATE_TEXT）。
 *
 * 群聊、好友、文件等消息类型当前未实现，会返回错误提示。
 */
public class ClientHandler implements Runnable {

    private final Socket socket;
    private final ChatServer server;
    private final FileTransferManager fileManager = new FileTransferManager();

    private ObjectOutputStream out;
    private ObjectInputStream in;

    private String userId; // 登录成功后赋值

    public ClientHandler(Socket socket, ChatServer server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        try {
            // 注意：ObjectOutputStream 需先创建并 flush，避免与对端的输入流互相阻塞
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());

            Message message;
            while ((message = Protocol.receive(in)) != null) {
                handleMessage(message);
            }
        } catch (IOException e) {
            // 客户端断开属正常情况
        } catch (ClassNotFoundException e) {
            System.err.println("[服务器] 反序列化失败：" + e.getMessage());
        } finally {
            close();
        }
    }

    private void handleMessage(Message message) {
        if (message == null || message.getType() == null) {
            return;
        }
        switch (message.getType()) {
            case LOGIN:
                handleLogin(message);
                break;
            case REGISTER:
                handleRegister(message);
                break;
            case PRIVATE_TEXT:
                handlePrivateText(message);
                break;
            case ADD_FRIEND:
                handleAddFriend(message);
                break;
            case FRIEND_LIST:
                handleFriendList(message);
                break;
            case CREATE_GROUP:
                handleCreateGroup(message);
                break;
            case RENAME_GROUP:
                handleRenameGroup(message);
                break;
            case GROUP_LIST:
                handleGroupList(message);
                break;
            case GROUP_MEMBER_LIST:
                handleGroupMemberList(message);
                break;
            case INVITE_TO_GROUP:
                handleInviteToGroup(message);
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
            case UPDATE_PROFILE:
                handleUpdateProfile(message);
                break;
            case LOGOUT:
                close();
                break;
            default:
                sendMessage(Message.error("当前版本暂不支持的消息类型：" + message.getType()));
                break;
        }
    }

    private void handleLogin(Message message) {
        String reqId = message.getFromUserId();
        String password = message.getExtra();

        User user = server.getUserManager().login(reqId, password);
        if (user == null) {
            String reason = server.getUserManager().getLastLoginError();
            sendMessage(Message.error(reason != null ? reason : "登录失败，请稍后重试。"));
            return;
        }

        this.userId = user.getUserId();
        server.register(this.userId, this);

        Message ok = Message.notice("登录成功，欢迎你，" + user.getNickname() + "（ID：" + user.getUserId() + "）");
        ok.setToUserId(this.userId);
        if (user.getAvatarPath() != null && !user.getAvatarPath().trim().isEmpty()) {
            ok.setExtra(user.getAvatarPath());
            try {
                byte[] avatarBytes = FileUtil.readFileBytes(new File(user.getAvatarPath()));
                ok.setFileBytes(avatarBytes);
                ok.setFileName(new File(user.getAvatarPath()).getName());
            } catch (IOException ignored) {
            }
        }
        sendMessage(ok);

        System.out.println("[服务器] 用户登录：" + user.getUserId() + "（" + user.getNickname() + "）");
    }

    private void handleRegister(Message message) {
        String reqId = message.getFromUserId();
        String nickname = message.getContent();
        String password = message.getExtra();

        String error = server.getUserManager().register(reqId, nickname, password);
        if (error != null) {
            sendMessage(Message.error(error));
            return;
        }

        User user = server.getUserManager().getUser(reqId.trim());
        Message ok = Message.notice("注册成功，用户名：" + user.getUserId()
                + "，昵称：" + user.getNickname() + "。请返回登录。");
        sendMessage(ok);
        System.out.println("[服务器] 用户注册：" + user.getUserId() + "（" + user.getNickname() + "）");
    }

    private void handleAddFriend(Message message) {
        if (userId == null) {
            sendMessage(Message.error("请先登录后再添加好友。"));
            return;
        }
        String targetId = message.getToUserId();
        if (targetId == null || targetId.trim().isEmpty()) {
            sendMessage(Message.error("添加失败：未指定好友 ID。"));
            return;
        }
        targetId = targetId.trim();
        if (targetId.equals(userId)) {
            sendMessage(Message.error("添加失败：不能添加自己为好友。"));
            return;
        }
        User targetUser = server.getUserManager().getUser(targetId);
        if (targetUser == null) {
            sendMessage(Message.error("添加失败：用户 [" + targetId + "] 不存在。"));
            return;
        }
        if (server.getFriendManager().isFriend(userId, targetId)) {
            sendMessage(Message.error("添加失败：用户 [" + targetId + "] 已是你的好友。"));
            return;
        }

        boolean added = server.getFriendManager().addFriend(userId, targetId);
        if (!added) {
            sendMessage(Message.error("添加失败：无法建立好友关系。"));
            return;
        }

        User me = server.getUserManager().getUser(userId);
        String myNickname = me != null ? me.getNickname() : userId;
        String targetNickname = targetUser.getNickname();

        Message okForMe = Message.notice("已添加好友 " + targetNickname + "（ID：" + targetId + "）");
        okForMe.setToUserId(userId);
        sendMessage(okForMe);

        Message okForTarget = Message.notice("用户 " + myNickname + "（ID：" + userId + "）已把你添加为好友");
        okForTarget.setToUserId(targetId);
        server.sendToUser(targetId, okForTarget);

        pushFriendList(userId);
        pushFriendList(targetId);

        System.out.println("[服务器] 好友关系建立：" + userId + " <-> " + targetId);
    }

    private void handleUpdateProfile(Message message) {
        if (userId == null) {
            sendMessage(Message.error("请先登录后再修改资料。"));
            return;
        }
        String nickname = message.getContent();
        String newAvatarPath = null;
        byte[] avatarBytes = message.getFileBytes();
        if (avatarBytes != null && avatarBytes.length > 0) {
            try {
                newAvatarPath = fileManager.saveAvatar(userId, message);
            } catch (IOException e) {
                sendMessage(Message.error("头像保存失败：" + e.getMessage()));
                return;
            }
        }

        String error = server.getUserManager().updateProfile(userId, nickname, newAvatarPath);
        if (error != null) {
            sendMessage(Message.error(error));
            return;
        }

        User user = server.getUserManager().getUser(userId);
        Message response = buildProfileUpdateMessage(user, avatarBytes, message.getFileName());
        response.setToUserId(userId);
        sendMessage(response);

        Set<String> friends = server.getFriendManager().getFriends(userId);
        for (String friendId : friends) {
            Message notify = buildProfileUpdateMessage(user, avatarBytes, message.getFileName());
            notify.setFromUserId(userId);
            notify.setToUserId(friendId);
            server.sendToUser(friendId, notify);
            pushFriendList(friendId);
        }
        pushFriendList(userId);

        System.out.println("[服务器] 用户资料更新：" + userId + "，昵称=" + user.getNickname());
    }

    private Message buildProfileUpdateMessage(User user, byte[] avatarBytes, String fileName) {
        Message msg = new Message();
        msg.setType(MessageType.UPDATE_PROFILE);
        msg.setFromUserId(user != null ? user.getUserId() : "server");
        msg.setContent(user != null ? user.getNickname() : "");
        msg.setExtra(user != null ? user.getAvatarPath() : null);
        if (avatarBytes != null && avatarBytes.length > 0) {
            msg.setFileBytes(avatarBytes);
            msg.setFileName(fileName);
        }
        return msg;
    }

    private void handleFriendList(Message message) {
        if (userId == null) {
            sendMessage(Message.error("请先登录后再获取好友列表。"));
            return;
        }
        pushFriendList(userId);
    }

    /** 向指定在线用户推送最新好友列表。 */
    private void pushFriendList(String ownerId) {
        Message listMsg = buildFriendListMessage(ownerId);
        if (ownerId.equals(userId)) {
            sendMessage(listMsg);
        } else {
            server.sendToUser(ownerId, listMsg);
        }
    }

    /**
     * 构建 FRIEND_LIST 响应消息。
     * content 编码：每行 friendUserId\u0001nickname\u0001online(0/1)\u0001avatarPath，多行以 \n 连接。
     */
    private Message buildFriendListMessage(String ownerId) {
        Set<String> friendIds = server.getFriendManager().getFriends(ownerId);
        StringBuilder sb = new StringBuilder();
        for (String friendId : friendIds) {
            User friend = server.getUserManager().getUser(friendId);
            String nickname = friend != null ? friend.getNickname() : friendId;
            String avatarPath = friend != null && friend.getAvatarPath() != null ? friend.getAvatarPath() : "";
            boolean online = server.isOnline(friendId);
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append(friendId).append('\u0001').append(nickname).append('\u0001')
                    .append(online ? '1' : '0').append('\u0001').append(avatarPath);
        }
        Message msg = new Message();
        msg.setType(MessageType.FRIEND_LIST);
        msg.setFromUserId("server");
        msg.setToUserId(ownerId);
        msg.setContent(sb.toString());
        return msg;
    }

    private void handleCreateGroup(Message message) {
        if (userId == null) {
            sendMessage(Message.error("请先登录后再创建群聊。"));
            return;
        }
        String groupName = message.getContent();
        Group group = server.getGroupManager().createGroup(groupName, userId);
        if (group == null) {
            sendMessage(Message.error("创建群聊失败。"));
            return;
        }

        Message ok = Message.notice("群聊「" + group.getGroupName() + "」创建成功（ID：" + group.getGroupId() + "）");
        ok.setToUserId(userId);
        sendMessage(ok);

        pushGroupList(userId);
        pushGroupMemberList(userId, group.getGroupId());

        System.out.println("[服务器] 群聊创建：" + group.getGroupId() + "（" + group.getGroupName() + "），群主：" + userId);
    }

    private void handleRenameGroup(Message message) {
        if (userId == null) {
            sendMessage(Message.error("请先登录后再修改群名称。"));
            return;
        }
        String groupId = message.getGroupId();
        if (groupId == null || groupId.trim().isEmpty()) {
            sendMessage(Message.error("修改失败：未指定群 ID。"));
            return;
        }
        groupId = groupId.trim();

        GroupManager.RenameResult result = server.getGroupManager().renameGroup(
                groupId, userId, message.getContent());
        switch (result) {
            case SUCCESS:
                Group group = server.getGroupManager().getGroup(groupId);
                User operator = server.getUserManager().getUser(userId);
                String operatorNickname = operator != null ? operator.getNickname() : userId;

                Message ok = Message.notice("群名称已修改为「" + group.getGroupName() + "」");
                ok.setToUserId(userId);
                sendMessage(ok);

                for (String memberId : server.getGroupManager().getMemberIds(groupId)) {
                    pushGroupList(memberId);
                    if (!memberId.equals(userId)) {
                        Message notice = Message.notice(
                                operatorNickname + "（ID：" + userId + "）将群聊名称修改为「"
                                        + group.getGroupName() + "」");
                        notice.setToUserId(memberId);
                        server.sendToUser(memberId, notice);
                    }
                }
                System.out.println("[服务器] 群名称修改：" + groupId + " -> "
                        + group.getGroupName() + "，操作者：" + userId);
                break;
            case GROUP_NOT_FOUND:
                sendMessage(Message.error("修改失败：群聊不存在。"));
                break;
            case NOT_MEMBER:
                sendMessage(Message.error("修改失败：你不是该群成员。"));
                break;
            default:
                break;
        }
    }

    private void handleGroupList(Message message) {
        if (userId == null) {
            sendMessage(Message.error("请先登录后再获取群聊列表。"));
            return;
        }
        pushGroupList(userId);
    }

    private void pushGroupList(String ownerId) {
        Message listMsg = buildGroupListMessage(ownerId);
        if (ownerId.equals(userId)) {
            sendMessage(listMsg);
        } else {
            server.sendToUser(ownerId, listMsg);
        }
    }

    /**
     * 构建 GROUP_LIST 响应。
     * content 编码：每行 groupId\u0001groupName，多行以 \n 连接。
     */
    private Message buildGroupListMessage(String ownerId) {
        List<Group> groups = server.getGroupManager().getUserGroups(ownerId);
        StringBuilder sb = new StringBuilder();
        for (Group group : groups) {
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append(group.getGroupId()).append('\u0001').append(group.getGroupName());
        }
        Message msg = new Message();
        msg.setType(MessageType.GROUP_LIST);
        msg.setFromUserId("server");
        msg.setToUserId(ownerId);
        msg.setContent(sb.toString());
        return msg;
    }

    private void handleGroupMemberList(Message message) {
        if (userId == null) {
            sendMessage(Message.error("请先登录后再获取群成员列表。"));
            return;
        }
        String groupId = message.getGroupId();
        if (groupId == null || groupId.trim().isEmpty()) {
            sendMessage(Message.error("获取失败：未指定群 ID。"));
            return;
        }
        groupId = groupId.trim();
        if (!server.getGroupManager().isMember(groupId, userId)) {
            sendMessage(Message.error("获取失败：你不是该群成员。"));
            return;
        }
        pushGroupMemberList(userId, groupId);
    }

    private void pushGroupMemberList(String ownerId, String groupId) {
        Message listMsg = buildGroupMemberListMessage(ownerId, groupId);
        if (ownerId.equals(userId)) {
            sendMessage(listMsg);
        } else {
            server.sendToUser(ownerId, listMsg);
        }
    }

    /** 向群内所有在线成员推送最新成员列表。 */
    private void pushGroupMemberListToAllMembers(String groupId) {
        for (String memberId : server.getGroupManager().getMemberIds(groupId)) {
            pushGroupMemberList(memberId, groupId);
        }
    }

    /**
     * 构建 GROUP_MEMBER_LIST 响应。
     * content 编码：每行 memberUserId\u0001nickname\u0001online(0/1)\u0001isFriend(0/1)\u0001avatarPath，多行以 \n 连接。
     * isFriend 表示该成员是否已是 ownerId 的好友（自己恒为 1）。
     */
    private Message buildGroupMemberListMessage(String ownerId, String groupId) {
        List<String> memberIds = server.getGroupManager().getMemberIds(groupId);
        StringBuilder sb = new StringBuilder();
        for (String memberId : memberIds) {
            User member = server.getUserManager().getUser(memberId);
            String nickname = member != null ? member.getNickname() : memberId;
            String avatarPath = member != null && member.getAvatarPath() != null ? member.getAvatarPath() : "";
            boolean online = server.isOnline(memberId);
            boolean isFriend = memberId.equals(ownerId)
                    || server.getFriendManager().isFriend(ownerId, memberId);
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append(memberId).append('\u0001').append(nickname).append('\u0001')
                    .append(online ? '1' : '0').append('\u0001')
                    .append(isFriend ? '1' : '0').append('\u0001').append(avatarPath);
        }
        Message msg = new Message();
        msg.setType(MessageType.GROUP_MEMBER_LIST);
        msg.setFromUserId("server");
        msg.setToUserId(ownerId);
        msg.setGroupId(groupId);
        msg.setContent(sb.toString());
        return msg;
    }

    private void handleInviteToGroup(Message message) {
        if (userId == null) {
            sendMessage(Message.error("请先登录后再邀请好友入群。"));
            return;
        }
        String groupId = message.getGroupId();
        if (groupId == null || groupId.trim().isEmpty()) {
            sendMessage(Message.error("邀请失败：未指定群 ID。"));
            return;
        }
        groupId = groupId.trim();

        List<String> inviteeIds = parseInviteeIds(message);
        if (inviteeIds.isEmpty()) {
            sendMessage(Message.error("邀请失败：未指定被邀请用户。"));
            return;
        }

        Group group = server.getGroupManager().getGroup(groupId);
        if (group == null) {
            sendMessage(Message.error("邀请失败：群聊不存在。"));
            return;
        }

        User inviter = server.getUserManager().getUser(userId);
        String inviterNickname = inviter != null ? inviter.getNickname() : userId;

        int successCount = 0;
        StringBuilder errors = new StringBuilder();

        for (String inviteeId : inviteeIds) {
            GroupManager.InviteResult result = server.getGroupManager().inviteFriend(
                    groupId, userId, inviteeId,
                    server.getFriendManager(), server.getUserManager());

            switch (result) {
                case SUCCESS:
                    successCount++;
                    User invitee = server.getUserManager().getUser(inviteeId);
                    String inviteeNickname = invitee != null ? invitee.getNickname() : inviteeId;

                    Message noticeForInvitee = Message.notice(
                            inviterNickname + "（ID：" + userId + "）邀请你加入群聊「"
                                    + group.getGroupName() + "」（ID：" + groupId + "）");
                    noticeForInvitee.setToUserId(inviteeId);
                    server.sendToUser(inviteeId, noticeForInvitee);

                    pushGroupList(inviteeId);

                    System.out.println("[服务器] 邀请入群：" + userId + " -> " + inviteeId + "，群：" + groupId);
                    break;
                case INVITER_NOT_MEMBER:
                    sendMessage(Message.error("邀请失败：你不是该群成员。"));
                    return;
                case NOT_FRIEND:
                    appendInviteError(errors, inviteeId, "不是你的好友");
                    break;
                case ALREADY_MEMBER:
                    appendInviteError(errors, inviteeId, "已在群内");
                    break;
                case TARGET_NOT_FOUND:
                    appendInviteError(errors, inviteeId, "用户不存在");
                    break;
                case GROUP_NOT_FOUND:
                    sendMessage(Message.error("邀请失败：群聊不存在。"));
                    return;
                default:
                    break;
            }
        }

        if (successCount > 0) {
            pushGroupMemberListToAllMembers(groupId);
            Message ok = Message.notice("已成功邀请 " + successCount + " 位好友加入群聊「" + group.getGroupName() + "」");
            ok.setToUserId(userId);
            sendMessage(ok);
        }
        if (errors.length() > 0) {
            sendMessage(Message.error(errors.toString()));
        } else if (successCount == 0) {
            sendMessage(Message.error("邀请失败：没有好友被成功邀请。"));
        }
    }

    private static void appendInviteError(StringBuilder errors, String inviteeId, String reason) {
        if (errors.length() > 0) {
            errors.append('；');
        }
        errors.append('[').append(inviteeId).append("] ").append(reason);
    }

    /** 解析被邀请用户 ID：优先 toUserId，extra 可含逗号分隔的多个 ID。 */
    private List<String> parseInviteeIds(Message message) {
        List<String> ids = new ArrayList<String>();
        if (message.getToUserId() != null && !message.getToUserId().trim().isEmpty()) {
            ids.add(message.getToUserId().trim());
        }
        String extra = message.getExtra();
        if (extra != null && !extra.trim().isEmpty()) {
            String[] parts = extra.split(",");
            for (String part : parts) {
                if (part != null && !part.trim().isEmpty()) {
                    String id = part.trim();
                    if (!ids.contains(id)) {
                        ids.add(id);
                    }
                }
            }
        }
        return ids;
    }

    private void handleGroupText(Message message) {
        if (userId == null) {
            sendMessage(Message.error("请先登录后再发送群消息。"));
            return;
        }
        String groupId = message.getGroupId();
        if (groupId == null || groupId.trim().isEmpty()) {
            sendMessage(Message.error("发送失败：未指定群 ID。"));
            return;
        }
        groupId = groupId.trim();
        if (!server.getGroupManager().isMember(groupId, userId)) {
            sendMessage(Message.error("发送失败：你不是该群成员。"));
            return;
        }
        message.setFromUserId(userId);
        server.sendToGroupMembers(groupId, message, null);
    }

    private void handlePrivateText(Message message) {
        if (userId == null) {
            sendMessage(Message.error("请先登录后再发送消息。"));
            return;
        }
        String toUserId = message.getToUserId();
        if (toUserId == null || toUserId.trim().isEmpty()) {
            sendMessage(Message.error("发送失败：未指定接收者。"));
            return;
        }
        // 确保发送者 ID 与登录身份一致
        message.setFromUserId(userId);

        boolean delivered = server.sendToUser(toUserId, message);
        if (!delivered) {
            sendMessage(Message.error("发送失败：用户 [" + toUserId + "] 不在线或不存在。"));
        }
    }

    private void handlePrivateImage(Message message) {
        if (userId == null) {
            sendMessage(Message.error("请先登录后再发送图片。"));
            return;
        }
        String toUserId = message.getToUserId();
        if (toUserId == null || toUserId.trim().isEmpty()) {
            sendMessage(Message.error("发送失败：未指定接收者。"));
            return;
        }
        if (message.getFileBytes() == null || message.getFileBytes().length == 0) {
            sendMessage(Message.error("发送失败：图片数据为空。"));
            return;
        }
        if (message.getFileSize() > FileTransferManager.MAX_IMAGE_BYTES
                || message.getFileBytes().length > FileTransferManager.MAX_IMAGE_BYTES) {
            sendMessage(Message.error("发送失败：图片过大，最大允许 5MB。"));
            return;
        }
        try {
            String savedPath = fileManager.saveImage(message);
            message.setContent(savedPath);
        } catch (IOException e) {
            sendMessage(Message.error("发送失败：图片保存异常，" + e.getMessage()));
            return;
        }
        message.setFromUserId(userId);
        boolean delivered = server.sendToUser(toUserId.trim(), message);
        if (!delivered) {
            sendMessage(Message.error("发送失败：用户 [" + toUserId + "] 不在线或不存在。"));
        }
    }

    private void handleGroupImage(Message message) {
        if (userId == null) {
            sendMessage(Message.error("请先登录后再发送群图片。"));
            return;
        }
        String groupId = message.getGroupId();
        if (groupId == null || groupId.trim().isEmpty()) {
            sendMessage(Message.error("发送失败：未指定群 ID。"));
            return;
        }
        groupId = groupId.trim();
        if (!server.getGroupManager().isMember(groupId, userId)) {
            sendMessage(Message.error("发送失败：你不是该群成员。"));
            return;
        }
        if (message.getFileBytes() == null || message.getFileBytes().length == 0) {
            sendMessage(Message.error("发送失败：图片数据为空。"));
            return;
        }
        if (message.getFileSize() > FileTransferManager.MAX_IMAGE_BYTES
                || message.getFileBytes().length > FileTransferManager.MAX_IMAGE_BYTES) {
            sendMessage(Message.error("发送失败：图片过大，最大允许 5MB。"));
            return;
        }
        try {
            String savedPath = fileManager.saveImage(message);
            message.setContent(savedPath);
        } catch (IOException e) {
            sendMessage(Message.error("发送失败：图片保存异常，" + e.getMessage()));
            return;
        }
        message.setFromUserId(userId);
        server.sendToGroupMembers(groupId, message, userId);
    }

    private void handlePrivateFile(Message message) {
        if (userId == null) {
            sendMessage(Message.error("请先登录后再发送文件。"));
            return;
        }
        String toUserId = message.getToUserId();
        if (toUserId == null || toUserId.trim().isEmpty()) {
            sendMessage(Message.error("发送失败：未指定接收者。"));
            return;
        }
        if (message.getFileBytes() == null || message.getFileBytes().length == 0) {
            sendMessage(Message.error("发送失败：文件数据为空。"));
            return;
        }
        if (message.getFileSize() > FileTransferManager.MAX_FILE_BYTES
                || message.getFileBytes().length > FileTransferManager.MAX_FILE_BYTES) {
            sendMessage(Message.error("发送失败：文件过大，最大允许 "
                    + util.FileUtil.humanSize(FileTransferManager.MAX_FILE_BYTES) + "。"));
            return;
        }
        try {
            String savedPath = fileManager.saveFile(message);
            message.setContent(savedPath);
        } catch (IOException e) {
            sendMessage(Message.error("发送失败：文件保存异常，" + e.getMessage()));
            return;
        }
        message.setFromUserId(userId);
        boolean delivered = server.sendToUser(toUserId.trim(), message);
        if (!delivered) {
            sendMessage(Message.error("发送失败：用户 [" + toUserId + "] 不在线或不存在。"));
        }
    }

    private void handleGroupFile(Message message) {
        if (userId == null) {
            sendMessage(Message.error("请先登录后再发送群文件。"));
            return;
        }
        String groupId = message.getGroupId();
        if (groupId == null || groupId.trim().isEmpty()) {
            sendMessage(Message.error("发送失败：未指定群 ID。"));
            return;
        }
        groupId = groupId.trim();
        if (!server.getGroupManager().isMember(groupId, userId)) {
            sendMessage(Message.error("发送失败：你不是该群成员。"));
            return;
        }
        if (message.getFileBytes() == null || message.getFileBytes().length == 0) {
            sendMessage(Message.error("发送失败：文件数据为空。"));
            return;
        }
        if (message.getFileSize() > FileTransferManager.MAX_FILE_BYTES
                || message.getFileBytes().length > FileTransferManager.MAX_FILE_BYTES) {
            sendMessage(Message.error("发送失败：文件过大，最大允许 "
                    + util.FileUtil.humanSize(FileTransferManager.MAX_FILE_BYTES) + "。"));
            return;
        }
        try {
            String savedPath = fileManager.saveFile(message);
            message.setContent(savedPath);
        } catch (IOException e) {
            sendMessage(Message.error("发送失败：文件保存异常，" + e.getMessage()));
            return;
        }
        message.setFromUserId(userId);
        server.sendToGroupMembers(groupId, message, userId);
    }

    private void handlePrivateVoice(Message message) {
        if (userId == null) {
            sendMessage(Message.error("请先登录后再发送语音。"));
            return;
        }
        String toUserId = message.getToUserId();
        if (toUserId == null || toUserId.trim().isEmpty()) {
            sendMessage(Message.error("发送失败：未指定接收者。"));
            return;
        }
        if (message.getFileBytes() == null || message.getFileBytes().length == 0) {
            sendMessage(Message.error("发送失败：语音数据为空。"));
            return;
        }
        if (message.getFileSize() > FileTransferManager.MAX_VOICE_BYTES
                || message.getFileBytes().length > FileTransferManager.MAX_VOICE_BYTES) {
            sendMessage(Message.error("发送失败：语音文件过大，最大允许 "
                    + util.FileUtil.humanSize(FileTransferManager.MAX_VOICE_BYTES) + "。"));
            return;
        }
        try {
            String savedPath = fileManager.saveVoice(message);
            message.setContent(savedPath);
        } catch (IOException e) {
            sendMessage(Message.error("发送失败：语音保存异常，" + e.getMessage()));
            return;
        }
        message.setFromUserId(userId);
        boolean delivered = server.sendToUser(toUserId.trim(), message);
        if (!delivered) {
            sendMessage(Message.error("发送失败：用户 [" + toUserId + "] 不在线或不存在。"));
        }
    }

    private void handleGroupVoice(Message message) {
        if (userId == null) {
            sendMessage(Message.error("请先登录后再发送群语音。"));
            return;
        }
        String groupId = message.getGroupId();
        if (groupId == null || groupId.trim().isEmpty()) {
            sendMessage(Message.error("发送失败：未指定群 ID。"));
            return;
        }
        groupId = groupId.trim();
        if (!server.getGroupManager().isMember(groupId, userId)) {
            sendMessage(Message.error("发送失败：你不是该群成员。"));
            return;
        }
        if (message.getFileBytes() == null || message.getFileBytes().length == 0) {
            sendMessage(Message.error("发送失败：语音数据为空。"));
            return;
        }
        if (message.getFileSize() > FileTransferManager.MAX_VOICE_BYTES
                || message.getFileBytes().length > FileTransferManager.MAX_VOICE_BYTES) {
            sendMessage(Message.error("发送失败：语音文件过大，最大允许 "
                    + util.FileUtil.humanSize(FileTransferManager.MAX_VOICE_BYTES) + "。"));
            return;
        }
        try {
            String savedPath = fileManager.saveVoice(message);
            message.setContent(savedPath);
        } catch (IOException e) {
            sendMessage(Message.error("发送失败：语音保存异常，" + e.getMessage()));
            return;
        }
        message.setFromUserId(userId);
        server.sendToGroupMembers(groupId, message, userId);
    }

    /** 线程安全地向该客户端发送消息。 */
    public void sendMessage(Message message) {
        if (out == null) {
            return;
        }
        try {
            Protocol.send(out, message);
        } catch (IOException e) {
            System.err.println("[服务器] 向 " + userId + " 发送消息失败：" + e.getMessage());
            close();
        }
    }

    private void close() {
        if (userId != null) {
            String closingUserId = userId;
            server.unregister(closingUserId);
            System.out.println("[服务器] 用户下线：" + closingUserId);

            Set<String> friends = server.getFriendManager().getFriends(closingUserId);
            User user = server.getUserManager().getUser(closingUserId);
            String displayName = user != null ? user.getNickname() : closingUserId;
            for (String friendId : friends) {
                Message listMsg = buildFriendListMessage(friendId);
                server.sendToUser(friendId, listMsg);
                Message notice = Message.notice(displayName + "（" + closingUserId + "）已离线");
                server.sendToUser(friendId, notice);
            }

            userId = null;
        }
        try {
            if (in != null) {
                in.close();
            }
        } catch (IOException ignored) {
        }
        try {
            if (out != null) {
                out.close();
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
}
