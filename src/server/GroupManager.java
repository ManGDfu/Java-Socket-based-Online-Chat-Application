package server;

import common.Group;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 群聊管理器。
 *
 * 内存中维护群聊数据，结构对应 README「群聊数据」字段：
 * groupId、groupName、ownerUserId、memberUserIds、createdAt。
 */
public class GroupManager {

    /** 邀请入群结果。 */
    public enum InviteResult {
        SUCCESS,
        GROUP_NOT_FOUND,
        INVITER_NOT_MEMBER,
        NOT_FRIEND,
        ALREADY_MEMBER,
        TARGET_NOT_FOUND
    }

    /** 修改群名称结果。 */
    public enum RenameResult {
        SUCCESS,
        GROUP_NOT_FOUND,
        NOT_MEMBER
    }

    /** groupId -> Group。 */
    private final ConcurrentHashMap<String, Group> groups = new ConcurrentHashMap<String, Group>();

    /** JSON 持久化协调器；由 {@link ChatServer} 注入。 */
    private final JsonPersistenceManager persistence;

    /** 默认构造：不启用 JSON 持久化。 */
    public GroupManager() {
        this(null);
    }

    /** 【持久化改造】注入 JsonPersistenceManager。 */
    public GroupManager(JsonPersistenceManager persistence) {
        this.persistence = persistence;
    }

    /**
     * 创建群聊，创建者自动成为群主并加入成员列表。
     */
    public Group createGroup(String groupName, String ownerUserId) {
        if (ownerUserId == null || ownerUserId.trim().isEmpty()) {
            return null;
        }
        if (groupName == null || groupName.trim().isEmpty()) {
            groupName = "未命名群聊";
        }
        String groupId = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        Group group = new Group(groupId, groupName.trim(), ownerUserId.trim());
        groups.put(groupId, group);
        // 【持久化改造】创建群聊成功后，立即同步写入 group_data.json
        persistGroups();
        return group;
    }

    public Group getGroup(String groupId) {
        return groupId == null ? null : groups.get(groupId.trim());
    }

    /** 查询用户加入的所有群聊。 */
    public List<Group> getUserGroups(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return Collections.emptyList();
        }
        userId = userId.trim();
        List<Group> result = new ArrayList<Group>();
        for (Group group : groups.values()) {
            if (isMember(group.getGroupId(), userId)) {
                result.add(group);
            }
        }
        return result;
    }

    public boolean isMember(String groupId, String userId) {
        Group group = getGroup(groupId);
        if (group == null || userId == null) {
            return false;
        }
        List<String> members = group.getMemberUserIds();
        synchronized (members) {
            return members.contains(userId.trim());
        }
    }

    /** 返回群成员 ID 副本。 */
    public List<String> getMemberIds(String groupId) {
        Group group = getGroup(groupId);
        if (group == null) {
            return Collections.emptyList();
        }
        List<String> members = group.getMemberUserIds();
        synchronized (members) {
            return new ArrayList<String>(members);
        }
    }

    /**
     * 邀请好友入群。
     *
     * @param inviterUserId 邀请者（须为群成员）
     * @param inviteeUserId 被邀请者（须为邀请者好友且尚未在群内）
     */
    public InviteResult inviteFriend(String groupId, String inviterUserId, String inviteeUserId,
                                     FriendManager friendManager, UserManager userManager) {
        Group group = getGroup(groupId);
        if (group == null) {
            return InviteResult.GROUP_NOT_FOUND;
        }
        if (inviterUserId == null || inviteeUserId == null) {
            return InviteResult.TARGET_NOT_FOUND;
        }
        inviterUserId = inviterUserId.trim();
        inviteeUserId = inviteeUserId.trim();
        if (inviteeUserId.isEmpty()) {
            return InviteResult.TARGET_NOT_FOUND;
        }
        if (!isMember(groupId, inviterUserId)) {
            return InviteResult.INVITER_NOT_MEMBER;
        }
        if (userManager.getUser(inviteeUserId) == null) {
            return InviteResult.TARGET_NOT_FOUND;
        }
        if (!friendManager.isFriend(inviterUserId, inviteeUserId)) {
            return InviteResult.NOT_FRIEND;
        }
        if (isMember(groupId, inviteeUserId)) {
            return InviteResult.ALREADY_MEMBER;
        }

        List<String> members = group.getMemberUserIds();
        synchronized (members) {
            if (members.contains(inviteeUserId)) {
                return InviteResult.ALREADY_MEMBER;
            }
            members.add(inviteeUserId);
        }
        // 【持久化改造】邀请好友入群成功后，立即同步写入 group_data.json
        persistGroups();
        return InviteResult.SUCCESS;
    }

    /**
     * 【持久化改造】用户退出群聊。
     * 当前客户端尚未实现 LEAVE_GROUP 消息，此方法供后续扩展；
     * 退群成功后同步写入 group_data.json。若群已无成员则删除该群记录。
     *
     * @return 成功返回 true；非成员或群不存在时返回 false。
     */
    public boolean leaveGroup(String groupId, String userId) {
        Group group = getGroup(groupId);
        if (group == null || userId == null || userId.trim().isEmpty()) {
            return false;
        }
        userId = userId.trim();
        List<String> members = group.getMemberUserIds();
        boolean removed;
        synchronized (members) {
            removed = members.remove(userId);
        }
        if (!removed) {
            return false;
        }
        if (members.isEmpty()) {
            groups.remove(group.getGroupId());
        }
        // 【持久化改造】退群后立即落盘
        persistGroups();
        return true;
    }

    /**
     * 修改群名称（须为群成员）。
     *
     * @param newGroupName 新群名，空则设为「未命名群聊」
     */
    public RenameResult renameGroup(String groupId, String operatorUserId, String newGroupName) {
        Group group = getGroup(groupId);
        if (group == null) {
            return RenameResult.GROUP_NOT_FOUND;
        }
        if (operatorUserId == null || operatorUserId.trim().isEmpty()) {
            return RenameResult.NOT_MEMBER;
        }
        operatorUserId = operatorUserId.trim();
        if (!isMember(groupId, operatorUserId)) {
            return RenameResult.NOT_MEMBER;
        }
        if (newGroupName == null || newGroupName.trim().isEmpty()) {
            newGroupName = "未命名群聊";
        } else {
            newGroupName = newGroupName.trim();
        }
        group.setGroupName(newGroupName);
        // 【持久化改造】修改群名称成功后，立即同步写入 group_data.json
        persistGroups();
        return RenameResult.SUCCESS;
    }

    /** 【持久化改造】启动时由 JsonPersistenceManager 调用，用磁盘数据替换内存 Map。 */
    void replaceAllGroups(java.util.Map<String, Group> loaded) {
        groups.clear();
        if (loaded != null) {
            groups.putAll(loaded);
        }
    }

    /** 【持久化改造】供 JsonPersistenceManager 序列化群列表快照。 */
    java.util.List<Group> getAllGroupsSnapshot() {
        return new java.util.ArrayList<Group>(groups.values());
    }

    /** 【持久化改造】供启动日志统计群数量。 */
    int getGroupCount() {
        return groups.size();
    }

    /** 【持久化改造】群数据变更后触发 JSON 落盘。 */
    private void persistGroups() {
        if (persistence != null) {
            persistence.saveGroups(this);
        }
    }
}
