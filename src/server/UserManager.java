package server;

import common.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用户管理器。
 *
 * 内存中维护已注册用户；须先注册再登录，登录时校验密码。
 */
public class UserManager {

    /** userId -> User。 */
    private final ConcurrentHashMap<String, User> users = new ConcurrentHashMap<String, User>();

    /** JSON 持久化协调器；由 {@link ChatServer} 注入，为 null 时不写盘（兼容纯内存模式）。 */
    private final JsonPersistenceManager persistence;

    private String lastLoginError;

    /** 默认构造：不启用 JSON 持久化（测试或旧代码兼容）。 */
    public UserManager() {
        this(null);
    }

    /**
     * 【持久化改造】注入 JsonPersistenceManager，数据变更后自动写 user.json。
     */
    public UserManager(JsonPersistenceManager persistence) {
        this.persistence = persistence;
    }

    /**
     * 注册新用户。
     *
     * @return 成功返回 null；失败返回错误说明。
     */
    public String register(String userId, String nickname, String password) {
        if (userId == null || userId.trim().isEmpty()) {
            return "用户名不能为空。";
        }
        if (password == null || password.isEmpty()) {
            return "密码不能为空。";
        }
        userId = userId.trim();
        if (users.containsKey(userId)) {
            return "注册失败：用户名 [" + userId + "] 已存在。";
        }
        String shownName = (nickname == null || nickname.trim().isEmpty()) ? userId : nickname.trim();
        User user = new User(userId, shownName, password);
        users.put(userId, user);
        // 【持久化改造】新用户注册成功后，立即同步写入 user.json
        persistUsers();
        return null;
    }

    /**
     * 登录并校验密码。
     *
     * @return 登录成功返回用户；失败返回 null，可通过 {@link #getLastLoginError()} 获取原因。
     */
    public User login(String userId, String password) {
        lastLoginError = null;
        if (userId == null || userId.trim().isEmpty()) {
            lastLoginError = "用户名不能为空。";
            return null;
        }
        if (password == null || password.isEmpty()) {
            lastLoginError = "密码不能为空。";
            return null;
        }
        userId = userId.trim();

        User user = users.get(userId);
        if (user == null) {
            lastLoginError = "登录失败：用户不存在，请先注册。";
            return null;
        }
        if (user.isOnline()) {
            lastLoginError = "登录失败：用户 [" + userId + "] 已在线。";
            return null;
        }
        String stored = user.getPassword();
        if (stored == null || !stored.equals(password)) {
            lastLoginError = "登录失败：密码错误。";
            return null;
        }
        user.setOnline(true);
        return user;
    }

    public String getLastLoginError() {
        return lastLoginError;
    }

    /** 退出登录，标记为离线。 */
    public void logout(String userId) {
        if (userId == null) {
            return;
        }
        User user = users.get(userId);
        if (user != null) {
            user.setOnline(false);
        }
    }

    public User getUser(String userId) {
        return userId == null ? null : users.get(userId);
    }

    public boolean isOnline(String userId) {
        User user = getUser(userId);
        return user != null && user.isOnline();
    }

    /**
     * 更新用户昵称与头像路径（内存）。
     *
     * @param nickname   新昵称，为 null 或空白时保留原昵称
     * @param avatarPath 新头像路径，为 null 时保留原头像
     * @return 成功返回 null；失败返回错误说明
     */
    public String updateProfile(String userId, String nickname, String avatarPath) {
        if (userId == null || userId.trim().isEmpty()) {
            return "用户 ID 不能为空。";
        }
        userId = userId.trim();
        User user = users.get(userId);
        if (user == null) {
            return "用户不存在。";
        }
        if (nickname != null && !nickname.trim().isEmpty()) {
            user.setNickname(nickname.trim());
        }
        if (avatarPath != null && !avatarPath.trim().isEmpty()) {
            user.setAvatarPath(avatarPath.trim());
        }
        // 【持久化改造】昵称/头像变更后，立即同步写入 user.json
        persistUsers();
        return null;
    }

    /** 【持久化改造】启动时由 JsonPersistenceManager 调用，用磁盘数据替换内存 Map。 */
    void replaceAllUsers(Map<String, User> loaded) {
        users.clear();
        if (loaded != null) {
            users.putAll(loaded);
        }
    }

    /** 【持久化改造】供 JsonPersistenceManager 序列化时读取快照（副本，避免并发修改）。 */
    List<User> getAllUsersSnapshot() {
        return new ArrayList<User>(users.values());
    }

    /** 【持久化改造】供启动日志统计用户数量。 */
    int getUserCount() {
        return users.size();
    }

    /** 【持久化改造】注册/资料更新成功后触发 JSON 落盘。 */
    private void persistUsers() {
        if (persistence != null) {
            persistence.saveUsers(this);
        }
    }
}
