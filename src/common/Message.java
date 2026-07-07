package common;

import java.io.Serializable;
import java.util.UUID;

/**
 * 客户端与服务端之间统一传输的消息对象。
 *
 * 采用 {@link java.io.ObjectOutputStream}/{@link java.io.ObjectInputStream}
 * 进行序列化传输，因此必须实现 {@link Serializable}。
 *
 * 字段中与文件、群聊相关的部分在当前阶段暂不使用，先预留以便后续扩展。
 */
public class Message implements Serializable {

    private static final long serialVersionUID = 1L;

    private String messageId;     // 消息唯一编号
    private MessageType type;     // 消息类型
    private String fromUserId;    // 发送者 ID
    private String toUserId;      // 接收者 ID（私聊时使用）
    private String groupId;       // 群聊 ID（群聊时使用）
    private String content;       // 文本内容或文件描述
    private String fileName;      // 文件名（暂未使用）
    private long fileSize;        // 文件大小（暂未使用）
    private String fileType;      // 文件类型（暂未使用）
    private byte[] fileBytes;     // 文件二进制内容（暂未使用）
    private long timestamp;       // 发送时间
    private String extra;         // 扩展字段

    public Message() {
        this.messageId = UUID.randomUUID().toString();
        this.timestamp = System.currentTimeMillis();
    }

    public Message(MessageType type, String fromUserId, String toUserId, String content) {
        this();
        this.type = type;
        this.fromUserId = fromUserId;
        this.toUserId = toUserId;
        this.content = content;
    }

    /** 快速构造一个文本消息。 */
    public static Message text(String fromUserId, String toUserId, String content) {
        return new Message(MessageType.PRIVATE_TEXT, fromUserId, toUserId, content);
    }

    /** 快速构造一个服务端通知消息。 */
    public static Message notice(String content) {
        Message m = new Message();
        m.setType(MessageType.SERVER_NOTICE);
        m.setFromUserId("server");
        m.setContent(content);
        return m;
    }

    /** 快速构造一个错误消息。 */
    public static Message error(String content) {
        Message m = new Message();
        m.setType(MessageType.ERROR);
        m.setFromUserId("server");
        m.setContent(content);
        return m;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public String getFromUserId() {
        return fromUserId;
    }

    public void setFromUserId(String fromUserId) {
        this.fromUserId = fromUserId;
    }

    public String getToUserId() {
        return toUserId;
    }

    public void setToUserId(String toUserId) {
        this.toUserId = toUserId;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public byte[] getFileBytes() {
        return fileBytes;
    }

    public void setFileBytes(byte[] fileBytes) {
        this.fileBytes = fileBytes;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getExtra() {
        return extra;
    }

    public void setExtra(String extra) {
        this.extra = extra;
    }

    @Override
    public String toString() {
        return "Message{" +
                "type=" + type +
                ", from=" + fromUserId +
                ", to=" + toUserId +
                ", group=" + groupId +
                ", content='" + content + '\'' +
                '}';
    }
}
