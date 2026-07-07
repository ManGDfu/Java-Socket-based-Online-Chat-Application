package common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 群聊模型。
 *
 * 当前阶段尚未实现群聊功能，这里先定义出完整字段，
 * 供后续“创建群聊、群成员管理、群消息转发”等功能使用。
 */
public class Group implements Serializable {

    private static final long serialVersionUID = 1L;

    private String groupId;                  // 群聊 ID
    private String groupName;                // 群名称
    private String ownerUserId;              // 群主 ID
    private List<String> memberUserIds;      // 群成员 ID 列表
    private long createdAt;                   // 创建时间

    public Group() {
        this.memberUserIds = new ArrayList<String>();
        this.createdAt = System.currentTimeMillis();
    }

    public Group(String groupId, String groupName, String ownerUserId) {
        this();
        this.groupId = groupId;
        this.groupName = groupName;
        this.ownerUserId = ownerUserId;
        if (ownerUserId != null) {
            this.memberUserIds.add(ownerUserId);
        }
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(String ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    public List<String> getMemberUserIds() {
        return memberUserIds;
    }

    public void setMemberUserIds(List<String> memberUserIds) {
        this.memberUserIds = memberUserIds;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Group{" +
                "groupId='" + groupId + '\'' +
                ", groupName='" + groupName + '\'' +
                ", owner='" + ownerUserId + '\'' +
                ", members=" + memberUserIds +
                '}';
    }
}
