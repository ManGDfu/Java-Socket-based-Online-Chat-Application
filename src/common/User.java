package common;

import java.io.Serializable;

/**
 * 用户模型。
 *
 * 当前阶段主要使用 userId 与 nickname；password、avatarPath、onlineStatus
 * 先预留，供后续注册、资料设置、在线状态展示等功能使用。
 */
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    private String userId;        // 用户唯一 ID
    private String nickname;      // 昵称
    private String avatarPath;    // 头像路径或头像数据标识（暂未使用）
    private String password;      // 密码（暂未使用）
    private boolean online;       // 在线状态

    public User() {
    }

    public User(String userId, String nickname) {
        this.userId = userId;
        this.nickname = nickname;
    }

    public User(String userId, String nickname, String password) {
        this.userId = userId;
        this.nickname = nickname;
        this.password = password;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getAvatarPath() {
        return avatarPath;
    }

    public void setAvatarPath(String avatarPath) {
        this.avatarPath = avatarPath;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    @Override
    public String toString() {
        return "User{" +
                "userId='" + userId + '\'' +
                ", nickname='" + nickname + '\'' +
                ", online=" + online +
                '}';
    }
}
