package server;

import common.Group;
import common.User;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * JSON 本地文件持久化管理器。
 *
 * <p>在服务端工作目录（通常为 ChatServer.exe 所在目录，即 {@code user.dir}）下
 * 维护三个 JSON 文件，与内存中的 {@link UserManager}、{@link FriendManager}、
 * {@link GroupManager} 双向同步。</p>
 *
 * <ul>
 *   <li>{@code user.json} — 注册用户 ID、昵称、密码、头像路径等个人资料</li>
 *   <li>{@code friend_relation.json} — 双向好友关系</li>
 *   <li>{@code group_data.json} — 群 ID、群名称、成员 ID 列表等</li>
 * </ul>
 *
 * <p>不依赖 MySQL/SQLite，也不引入第三方 jar；序列化/反序列化使用 JDK 自带的
 * 轻量 JSON 读写实现，保证 jpackage 打包后可直接运行。</p>
 */
public class JsonPersistenceManager {

    /** 用户数据文件名（服务端根目录）。 */
    public static final String USER_FILE = "user.json";

    /** 好友关系文件名（服务端根目录）。 */
    public static final String FRIEND_FILE = "friend_relation.json";

    /** 群聊数据文件名（服务端根目录）。 */
    public static final String GROUP_FILE = "group_data.json";

    /** JSON 文件存放目录，默认为 JVM 启动时的 user.dir。 */
    private final Path baseDir;

    /**
     * 使用当前工作目录作为 JSON 文件根目录。
     * jpackage 打包后，工作目录一般为 ChatServer.exe 所在文件夹。
     */
    public JsonPersistenceManager() {
        this(Paths.get(System.getProperty("user.dir")));
    }

    /**
     * 指定 JSON 文件根目录（便于单元测试或自定义部署路径）。
     */
    public JsonPersistenceManager(Path baseDir) {
        this.baseDir = baseDir;
    }

    public Path getBaseDir() {
        return baseDir;
    }

    // ======================== 启动加载 ========================

    /**
     * 服务端启动时调用：检查三个 JSON 文件是否存在，不存在则创建空文件；
     * 存在则读取并反序列化，填充到各 Manager 的内存 Map 中。
     *
     * <p>注意：从磁盘加载的用户一律标记为离线（online=false），
     * 在线状态仅由本次运行的登录/登出维护，不写入 JSON。</p>
     */
    public void loadAll(UserManager userManager, FriendManager friendManager, GroupManager groupManager) {
        try {
            ensureFileExists(USER_FILE, "{\"users\":[]}");
            ensureFileExists(FRIEND_FILE, "{\"friendsMap\":{},\"friendships\":[]}");
            ensureFileExists(GROUP_FILE, "{\"groups\":[]}");

            loadUsers(userManager, readText(USER_FILE));
            loadFriends(friendManager, readText(FRIEND_FILE));
            loadGroups(groupManager, readText(GROUP_FILE));

            System.out.println("[持久化] 已从 JSON 加载数据，目录：" + baseDir.toAbsolutePath());
            System.out.println("[持久化] 用户 " + userManager.getUserCount()
                    + " 个，群聊 " + groupManager.getGroupCount() + " 个");
        } catch (IOException e) {
            System.err.println("[持久化] 加载 JSON 失败：" + e.getMessage());
            e.printStackTrace();
        }
    }

    /** 若文件不存在则写入默认空 JSON 结构。 */
    private void ensureFileExists(String fileName, String emptyContent) throws IOException {
        Path path = baseDir.resolve(fileName);
        if (!Files.exists(path)) {
            Files.write(path, emptyContent.getBytes(StandardCharsets.UTF_8));
            System.out.println("[持久化] 已创建空文件：" + path.toAbsolutePath());
        }
    }

    private String readText(String fileName) throws IOException {
        Path path = baseDir.resolve(fileName);
        byte[] bytes = Files.readAllBytes(path);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    // ======================== 即时保存（数据变更后调用） ========================

    /**
     * 将 {@link UserManager} 内存中的用户 Map 序列化并覆盖写入 user.json。
     * 在注册、资料更新等操作成功后由 UserManager 调用。
     */
    public void saveUsers(UserManager userManager) {
        try {
            String json = serializeUsers(userManager);
            writeText(USER_FILE, json);
            System.out.println("[持久化] 已保存 user.json（" + userManager.getUserCount() + " 个用户）");
        } catch (IOException e) {
            System.err.println("[持久化] 保存 user.json 失败：" + e.getMessage());
        }
    }

    /**
     * 将 {@link FriendManager} 内存中的好友 Map 序列化并覆盖写入 friend_relation.json。
     * 在添加/删除好友成功后由 FriendManager 调用。
     */
    public void saveFriends(FriendManager friendManager) {
        try {
            String json = serializeFriends(friendManager);
            writeText(FRIEND_FILE, json);
            System.out.println("[持久化] 已保存 friend_relation.json");
        } catch (IOException e) {
            System.err.println("[持久化] 保存 friend_relation.json 失败：" + e.getMessage());
        }
    }

    /**
     * 将 {@link GroupManager} 内存中的群 Map 序列化并覆盖写入 group_data.json。
     * 在创建群、邀请入群、退群、改群名等成功后由 GroupManager 调用。
     */
    public void saveGroups(GroupManager groupManager) {
        try {
            String json = serializeGroups(groupManager);
            writeText(GROUP_FILE, json);
            System.out.println("[持久化] 已保存 group_data.json（" + groupManager.getGroupCount() + " 个群）");
        } catch (IOException e) {
            System.err.println("[持久化] 保存 group_data.json 失败：" + e.getMessage());
        }
    }

    private void writeText(String fileName, String content) throws IOException {
        Path path = baseDir.resolve(fileName);
        Files.write(path, content.getBytes(StandardCharsets.UTF_8));
    }

    // ======================== 序列化 ========================

    private String serializeUsers(UserManager userManager) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"users\":[");
        boolean first = true;
        for (User user : userManager.getAllUsersSnapshot()) {
            if (!first) {
                sb.append(',');
            }
            first = false;
            sb.append('{');
            sb.append("\"userId\":").append(JsonCodec.quote(user.getUserId()));
            sb.append(",\"nickname\":").append(JsonCodec.quote(user.getNickname()));
            sb.append(",\"password\":").append(JsonCodec.quote(user.getPassword()));
            sb.append(",\"avatarPath\":").append(JsonCodec.quote(
                    user.getAvatarPath() != null ? user.getAvatarPath() : ""));
            sb.append('}');
        }
        sb.append("]}");
        return sb.toString();
    }

    private String serializeFriends(FriendManager friendManager) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"friendsMap\":{");
        Map<String, Set<String>> friendsMap = friendManager.getFriendsMapSnapshot();
        boolean firstUser = true;
        for (Map.Entry<String, Set<String>> entry : friendsMap.entrySet()) {
            if (!firstUser) {
                sb.append(',');
            }
            firstUser = false;
            sb.append(JsonCodec.quote(entry.getKey())).append(":[");
            boolean firstFriend = true;
            for (String friendId : entry.getValue()) {
                if (!firstFriend) {
                    sb.append(',');
                }
                firstFriend = false;
                sb.append(JsonCodec.quote(friendId));
            }
            sb.append(']');
        }
        sb.append("},\"friendships\":[");
        List<FriendManager.FriendshipRecord> records = friendManager.getFriendshipRecordsSnapshot();
        boolean firstRecord = true;
        for (FriendManager.FriendshipRecord record : records) {
            if (!firstRecord) {
                sb.append(',');
            }
            firstRecord = false;
            sb.append('{');
            sb.append("\"userId\":").append(JsonCodec.quote(record.getUserId()));
            sb.append(",\"friendUserId\":").append(JsonCodec.quote(record.getFriendUserId()));
            sb.append(",\"createdAt\":").append(record.getCreatedAt());
            sb.append('}');
        }
        sb.append("]}");
        return sb.toString();
    }

    private String serializeGroups(GroupManager groupManager) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"groups\":[");
        boolean first = true;
        for (Group group : groupManager.getAllGroupsSnapshot()) {
            if (!first) {
                sb.append(',');
            }
            first = false;
            sb.append('{');
            sb.append("\"groupId\":").append(JsonCodec.quote(group.getGroupId()));
            sb.append(",\"groupName\":").append(JsonCodec.quote(group.getGroupName()));
            sb.append(",\"ownerUserId\":").append(JsonCodec.quote(group.getOwnerUserId()));
            sb.append(",\"createdAt\":").append(group.getCreatedAt());
            sb.append(",\"memberUserIds\":[");
            List<String> members = group.getMemberUserIds();
            synchronized (members) {
                for (int i = 0; i < members.size(); i++) {
                    if (i > 0) {
                        sb.append(',');
                    }
                    sb.append(JsonCodec.quote(members.get(i)));
                }
            }
            sb.append("]}");
        }
        sb.append("]}");
        return sb.toString();
    }

    // ======================== 反序列化并灌入内存 Map ========================

    private void loadUsers(UserManager userManager, String json) {
        Map<String, Object> root = JsonCodec.parseObject(json);
        Object usersNode = root.get("users");
        if (!(usersNode instanceof List)) {
            return;
        }
        @SuppressWarnings("unchecked")
        List<Object> usersList = (List<Object>) usersNode;
        Map<String, User> loaded = new LinkedHashMap<String, User>();
        for (Object item : usersList) {
            if (!(item instanceof Map)) {
                continue;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) item;
            String userId = JsonCodec.asString(map.get("userId"));
            if (userId == null || userId.trim().isEmpty()) {
                continue;
            }
            User user = new User(
                    userId.trim(),
                    JsonCodec.asString(map.get("nickname")),
                    JsonCodec.asString(map.get("password"))
            );
            String avatarPath = JsonCodec.asString(map.get("avatarPath"));
            if (avatarPath != null && !avatarPath.isEmpty()) {
                user.setAvatarPath(avatarPath);
            }
            // 重启后所有用户默认离线，在线状态不入库
            user.setOnline(false);
            loaded.put(user.getUserId(), user);
        }
        userManager.replaceAllUsers(loaded);
    }

    private void loadFriends(FriendManager friendManager, String json) {
        Map<String, Object> root = JsonCodec.parseObject(json);
        Map<String, Set<String>> friendsMap = new HashMap<String, Set<String>>();
        Object friendsNode = root.get("friendsMap");
        if (friendsNode instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> rawMap = (Map<String, Object>) friendsNode;
            for (Map.Entry<String, Object> entry : rawMap.entrySet()) {
                Set<String> friendIds = new HashSet<String>();
                if (entry.getValue() instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Object> list = (List<Object>) entry.getValue();
                    for (Object id : list) {
                        String friendId = JsonCodec.asString(id);
                        if (friendId != null && !friendId.isEmpty()) {
                            friendIds.add(friendId);
                        }
                    }
                }
                if (!friendIds.isEmpty()) {
                    friendsMap.put(entry.getKey(), friendIds);
                }
            }
        }

        List<FriendManager.FriendshipRecord> records = new ArrayList<FriendManager.FriendshipRecord>();
        Object friendshipsNode = root.get("friendships");
        if (friendshipsNode instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>) friendshipsNode;
            for (Object item : list) {
                if (!(item instanceof Map)) {
                    continue;
                }
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) item;
                String userId = JsonCodec.asString(map.get("userId"));
                String friendUserId = JsonCodec.asString(map.get("friendUserId"));
                long createdAt = JsonCodec.asLong(map.get("createdAt"), System.currentTimeMillis());
                if (userId != null && friendUserId != null
                        && !userId.isEmpty() && !friendUserId.isEmpty()) {
                    records.add(new FriendManager.FriendshipRecord(userId, friendUserId, createdAt));
                }
            }
        }
        friendManager.replaceAllFriends(friendsMap, records);
    }

    private void loadGroups(GroupManager groupManager, String json) {
        Map<String, Object> root = JsonCodec.parseObject(json);
        Object groupsNode = root.get("groups");
        if (!(groupsNode instanceof List)) {
            return;
        }
        @SuppressWarnings("unchecked")
        List<Object> groupsList = (List<Object>) groupsNode;
        Map<String, Group> loaded = new LinkedHashMap<String, Group>();
        for (Object item : groupsList) {
            if (!(item instanceof Map)) {
                continue;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) item;
            String groupId = JsonCodec.asString(map.get("groupId"));
            if (groupId == null || groupId.trim().isEmpty()) {
                continue;
            }
            Group group = new Group();
            group.setGroupId(groupId.trim());
            group.setGroupName(JsonCodec.asString(map.get("groupName")));
            group.setOwnerUserId(JsonCodec.asString(map.get("ownerUserId")));
            group.setCreatedAt(JsonCodec.asLong(map.get("createdAt"), System.currentTimeMillis()));

            List<String> memberIds = new ArrayList<String>();
            Object membersNode = map.get("memberUserIds");
            if (membersNode instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> list = (List<Object>) membersNode;
                for (Object id : list) {
                    String memberId = JsonCodec.asString(id);
                    if (memberId != null && !memberId.isEmpty()) {
                        memberIds.add(memberId);
                    }
                }
            }
            group.setMemberUserIds(memberIds);
            loaded.put(group.getGroupId(), group);
        }
        groupManager.replaceAllGroups(loaded);
    }

    /**
     * 轻量 JSON 编解码器（仅支持本项目用到的 object / array / string / number / boolean / null）。
     * 使用 JDK 标准库实现，无需额外依赖。
     */
    static final class JsonCodec {

        private JsonCodec() {
        }

        static String quote(String value) {
            if (value == null) {
                return "null";
            }
            StringBuilder sb = new StringBuilder();
            sb.append('"');
            for (int i = 0; i < value.length(); i++) {
                char c = value.charAt(i);
                switch (c) {
                    case '"':
                        sb.append("\\\"");
                        break;
                    case '\\':
                        sb.append("\\\\");
                        break;
                    case '\b':
                        sb.append("\\b");
                        break;
                    case '\f':
                        sb.append("\\f");
                        break;
                    case '\n':
                        sb.append("\\n");
                        break;
                    case '\r':
                        sb.append("\\r");
                        break;
                    case '\t':
                        sb.append("\\t");
                        break;
                    default:
                        if (c < 0x20) {
                            sb.append(String.format("\\u%04x", (int) c));
                        } else {
                            sb.append(c);
                        }
                        break;
                }
            }
            sb.append('"');
            return sb.toString();
        }

        static Map<String, Object> parseObject(String json) {
            if (json == null) {
                return Collections.emptyMap();
            }
            String trimmed = json.trim();
            if (trimmed.isEmpty()) {
                return Collections.emptyMap();
            }
            Parser parser = new Parser(trimmed);
            Object value = parser.parseValue();
            return value instanceof Map ? (Map<String, Object>) value : Collections.emptyMap();
        }

        static String asString(Object value) {
            return value == null ? null : String.valueOf(value);
        }

        static long asLong(Object value, long defaultValue) {
            if (value instanceof Number) {
                return ((Number) value).longValue();
            }
            if (value instanceof String) {
                try {
                    return Long.parseLong((String) value);
                } catch (NumberFormatException ignored) {
                    return defaultValue;
                }
            }
            return defaultValue;
        }

        /** 递归下降 JSON 解析器。 */
        private static final class Parser {
            private final String text;
            private int index;

            Parser(String text) {
                this.text = text;
            }

            Object parseValue() {
                skipWhitespace();
                if (index >= text.length()) {
                    return null;
                }
                char c = text.charAt(index);
                if (c == '{') {
                    return parseObject();
                }
                if (c == '[') {
                    return parseArray();
                }
                if (c == '"') {
                    return parseString();
                }
                if (c == 't' && text.startsWith("true", index)) {
                    index += 4;
                    return Boolean.TRUE;
                }
                if (c == 'f' && text.startsWith("false", index)) {
                    index += 5;
                    return Boolean.FALSE;
                }
                if (c == 'n' && text.startsWith("null", index)) {
                    index += 4;
                    return null;
                }
                return parseNumber();
            }

            Map<String, Object> parseObject() {
                Map<String, Object> map = new LinkedHashMap<String, Object>();
                index++; // skip {
                skipWhitespace();
                if (peek('}')) {
                    index++;
                    return map;
                }
                while (index < text.length()) {
                    skipWhitespace();
                    String key = parseString();
                    skipWhitespace();
                    expect(':');
                    Object value = parseValue();
                    map.put(key, value);
                    skipWhitespace();
                    if (peek('}')) {
                        index++;
                        break;
                    }
                    expect(',');
                }
                return map;
            }

            List<Object> parseArray() {
                List<Object> list = new ArrayList<Object>();
                index++; // skip [
                skipWhitespace();
                if (peek(']')) {
                    index++;
                    return list;
                }
                while (index < text.length()) {
                    list.add(parseValue());
                    skipWhitespace();
                    if (peek(']')) {
                        index++;
                        break;
                    }
                    expect(',');
                }
                return list;
            }

            String parseString() {
                index++; // skip opening "
                StringBuilder sb = new StringBuilder();
                while (index < text.length()) {
                    char c = text.charAt(index++);
                    if (c == '"') {
                        return sb.toString();
                    }
                    if (c == '\\') {
                        if (index >= text.length()) {
                            break;
                        }
                        char esc = text.charAt(index++);
                        switch (esc) {
                            case '"':
                                sb.append('"');
                                break;
                            case '\\':
                                sb.append('\\');
                                break;
                            case '/':
                                sb.append('/');
                                break;
                            case 'b':
                                sb.append('\b');
                                break;
                            case 'f':
                                sb.append('\f');
                                break;
                            case 'n':
                                sb.append('\n');
                                break;
                            case 'r':
                                sb.append('\r');
                                break;
                            case 't':
                                sb.append('\t');
                                break;
                            case 'u':
                                if (index + 4 <= text.length()) {
                                    String hex = text.substring(index, index + 4);
                                    sb.append((char) Integer.parseInt(hex, 16));
                                    index += 4;
                                }
                                break;
                            default:
                                sb.append(esc);
                                break;
                        }
                    } else {
                        sb.append(c);
                    }
                }
                return sb.toString();
            }

            Number parseNumber() {
                int start = index;
                if (peek('-')) {
                    index++;
                }
                while (index < text.length() && Character.isDigit(text.charAt(index))) {
                    index++;
                }
                if (index < text.length() && text.charAt(index) == '.') {
                    index++;
                    while (index < text.length() && Character.isDigit(text.charAt(index))) {
                        index++;
                    }
                }
                if (index < text.length() && (text.charAt(index) == 'e' || text.charAt(index) == 'E')) {
                    index++;
                    if (index < text.length() && (text.charAt(index) == '+' || text.charAt(index) == '-')) {
                        index++;
                    }
                    while (index < text.length() && Character.isDigit(text.charAt(index))) {
                        index++;
                    }
                }
                String num = text.substring(start, index);
                if (num.contains(".") || num.contains("e") || num.contains("E")) {
                    return Double.valueOf(num);
                }
                try {
                    return Long.valueOf(num);
                } catch (NumberFormatException e) {
                    return 0L;
                }
            }

            void skipWhitespace() {
                while (index < text.length() && Character.isWhitespace(text.charAt(index))) {
                    index++;
                }
            }

            boolean peek(char expected) {
                return index < text.length() && text.charAt(index) == expected;
            }

            void expect(char expected) {
                skipWhitespace();
                if (index >= text.length() || text.charAt(index) != expected) {
                    throw new IllegalArgumentException("JSON 解析错误：期望 '" + expected + "'，位置 " + index);
                }
                index++;
            }
        }
    }
}
