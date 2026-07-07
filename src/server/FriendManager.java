package server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 好友关系管理器。
 *
 * 内存中维护好友关系，结构对应 README「好友数据」字段：
 * userId、friendUserId、createdAt。
 */
public class FriendManager {

    /** 好友关系记录，key 为 "userId|friendUserId"（按字典序规范化）。 */
    private static final class Friendship {
        private final String userId;
        private final String friendUserId;
        private final long createdAt;

        Friendship(String userId, String friendUserId, long createdAt) {
            this.userId = userId;
            this.friendUserId = friendUserId;
            this.createdAt = createdAt;
        }

        String getUserId() {
            return userId;
        }

        String getFriendUserId() {
            return friendUserId;
        }

        long getCreatedAt() {
            return createdAt;
        }
    }

    /**
     * 【持久化改造】可序列化的好友关系记录，供 JsonPersistenceManager 读写 friend_relation.json。
     */
    static final class FriendshipRecord {
        private final String userId;
        private final String friendUserId;
        private final long createdAt;

        FriendshipRecord(String userId, String friendUserId, long createdAt) {
            this.userId = userId;
            this.friendUserId = friendUserId;
            this.createdAt = createdAt;
        }

        String getUserId() {
            return userId;
        }

        String getFriendUserId() {
            return friendUserId;
        }

        long getCreatedAt() {
            return createdAt;
        }
    }

    /** userId -> 好友 ID 集合。 */
    private final ConcurrentHashMap<String, Set<String>> friendsMap = new ConcurrentHashMap<String, Set<String>>();

    /** 双向关系记录，便于查询 createdAt。 */
    private final ConcurrentHashMap<String, Friendship> friendships = new ConcurrentHashMap<String, Friendship>();

    /** JSON 持久化协调器；由 {@link ChatServer} 注入。 */
    private final JsonPersistenceManager persistence;

    /** 默认构造：不启用 JSON 持久化。 */
    public FriendManager() {
        this(null);
    }

    /** 【持久化改造】注入 JsonPersistenceManager。 */
    public FriendManager(JsonPersistenceManager persistence) {
        this.persistence = persistence;
    }

    /**
     * 添加好友（双向）。
     *
     * @return 成功返回 true；已是好友或自己加自己时返回 false。
     */
    public boolean addFriend(String userId, String friendUserId) {
        if (userId == null || friendUserId == null) {
            return false;
        }
        userId = userId.trim();
        friendUserId = friendUserId.trim();
        if (userId.isEmpty() || friendUserId.isEmpty() || userId.equals(friendUserId)) {
            return false;
        }
        if (isFriend(userId, friendUserId)) {
            return false;
        }

        long now = System.currentTimeMillis();
        addOneWay(userId, friendUserId, now);
        addOneWay(friendUserId, userId, now);
        // 【持久化改造】双向好友建立成功后，立即同步写入 friend_relation.json
        persistFriends();
        return true;
    }

    /**
     * 【持久化改造】删除好友（双向）。
     * 当前客户端尚未实现 DELETE_FRIEND 消息，此方法供后续扩展或管理端调用；
     * 删除成功后同样会同步写入 friend_relation.json。
     *
     * @return 成功返回 true；非好友或参数无效时返回 false。
     */
    public boolean removeFriend(String userId, String friendUserId) {
        if (userId == null || friendUserId == null) {
            return false;
        }
        userId = userId.trim();
        friendUserId = friendUserId.trim();
        if (userId.isEmpty() || friendUserId.isEmpty() || userId.equals(friendUserId)) {
            return false;
        }
        if (!isFriend(userId, friendUserId)) {
            return false;
        }
        removeOneWay(userId, friendUserId);
        removeOneWay(friendUserId, userId);
        // 【持久化改造】删除好友后立即落盘
        persistFriends();
        return true;
    }

    private void removeOneWay(String userId, String friendUserId) {
        Set<String> friends = friendsMap.get(userId);
        if (friends != null) {
            synchronized (friends) {
                friends.remove(friendUserId);
            }
            if (friends.isEmpty()) {
                friendsMap.remove(userId, friends);
            }
        }
        friendships.remove(friendshipKey(userId, friendUserId));
    }

    private void addOneWay(String userId, String friendUserId, long createdAt) {
        Set<String> friends = friendsMap.get(userId);
        if (friends == null) {
            Set<String> newSet = Collections.synchronizedSet(new HashSet<String>());
            friends = friendsMap.putIfAbsent(userId, newSet);
            if (friends == null) {
                friends = newSet;
            }
        }
        friends.add(friendUserId);
        friendships.put(friendshipKey(userId, friendUserId), new Friendship(userId, friendUserId, createdAt));
    }

    /** 查询某用户的所有好友 ID。 */
    public Set<String> getFriends(String userId) {
        if (userId == null) {
            return Collections.emptySet();
        }
        Set<String> friends = friendsMap.get(userId.trim());
        if (friends == null || friends.isEmpty()) {
            return Collections.emptySet();
        }
        synchronized (friends) {
            return new HashSet<String>(friends);
        }
    }

    /** 判断两个用户是否为好友。 */
    public boolean isFriend(String userId, String friendUserId) {
        if (userId == null || friendUserId == null) {
            return false;
        }
        Set<String> friends = friendsMap.get(userId.trim());
        return friends != null && friends.contains(friendUserId.trim());
    }

    /** 获取好友关系建立时间，不存在时返回 -1。 */
    public long getCreatedAt(String userId, String friendUserId) {
        Friendship f = friendships.get(friendshipKey(userId, friendUserId));
        return f != null ? f.getCreatedAt() : -1L;
    }

    private static String friendshipKey(String userId, String friendUserId) {
        return userId.trim() + "|" + friendUserId.trim();
    }

    /** 【持久化改造】启动时由 JsonPersistenceManager 调用，用磁盘数据替换内存 Map。 */
    void replaceAllFriends(Map<String, Set<String>> loadedFriendsMap,
                           List<FriendshipRecord> loadedRecords) {
        friendsMap.clear();
        friendships.clear();
        if (loadedFriendsMap != null) {
            for (Map.Entry<String, Set<String>> entry : loadedFriendsMap.entrySet()) {
                Set<String> copy = Collections.synchronizedSet(new HashSet<String>(entry.getValue()));
                friendsMap.put(entry.getKey(), copy);
            }
        }
        if (loadedRecords != null) {
            for (FriendshipRecord record : loadedRecords) {
                friendships.put(
                        friendshipKey(record.getUserId(), record.getFriendUserId()),
                        new Friendship(record.getUserId(), record.getFriendUserId(), record.getCreatedAt())
                );
            }
        }
    }

    /** 【持久化改造】供 JsonPersistenceManager 序列化 friendsMap 快照。 */
    Map<String, Set<String>> getFriendsMapSnapshot() {
        Map<String, Set<String>> snapshot = new HashMap<String, Set<String>>();
        for (Map.Entry<String, Set<String>> entry : friendsMap.entrySet()) {
            Set<String> friends = entry.getValue();
            synchronized (friends) {
                snapshot.put(entry.getKey(), new HashSet<String>(friends));
            }
        }
        return snapshot;
    }

    /** 【持久化改造】供 JsonPersistenceManager 序列化 friendships 快照。 */
    List<FriendshipRecord> getFriendshipRecordsSnapshot() {
        List<FriendshipRecord> list = new ArrayList<FriendshipRecord>();
        for (Friendship friendship : friendships.values()) {
            list.add(new FriendshipRecord(
                    friendship.getUserId(),
                    friendship.getFriendUserId(),
                    friendship.getCreatedAt()
            ));
        }
        return list;
    }

    /** 【持久化改造】好友关系变更后触发 JSON 落盘。 */
    private void persistFriends() {
        if (persistence != null) {
            persistence.saveFriends(this);
        }
    }
}
