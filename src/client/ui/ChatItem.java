package client.ui;

import javax.swing.ImageIcon;

/**
 * 聊天会话中的单条历史记录，支持文字、图片、文件与系统消息。
 */
public class ChatItem {

    public enum Kind {
        TEXT,
        IMAGE,
        FILE,
        VOICE,
        SYSTEM
    }

    private final Kind kind;
    private final String senderLabel;
    private final long timestamp;
    private final String text;
    private final ImageIcon thumbnail;
    private final byte[] fullImage;
    private final String fileName;
    private final long fileSize;
    private final byte[] fileBytes;

    private ChatItem(Kind kind, String senderLabel, long timestamp, String text,
                     ImageIcon thumbnail, byte[] fullImage, String fileName,
                     long fileSize, byte[] fileBytes) {
        this.kind = kind;
        this.senderLabel = senderLabel;
        this.timestamp = timestamp;
        this.text = text;
        this.thumbnail = thumbnail;
        this.fullImage = fullImage;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.fileBytes = fileBytes;
    }

    public static ChatItem text(String senderLabel, long timestamp, String text) {
        return new ChatItem(Kind.TEXT, senderLabel, timestamp, text, null, null, null, 0L, null);
    }

    public static ChatItem image(String senderLabel, long timestamp, ImageIcon thumbnail,
                                 byte[] fullImage, String fileName) {
        return new ChatItem(Kind.IMAGE, senderLabel, timestamp, null, thumbnail, fullImage, fileName, 0L, null);
    }

    public static ChatItem file(String senderLabel, long timestamp, String fileName,
                                long fileSize, byte[] fileBytes) {
        return new ChatItem(Kind.FILE, senderLabel, timestamp, null, null, null, fileName, fileSize, fileBytes);
    }

    public static ChatItem voice(String senderLabel, long timestamp, String fileName,
                                 long fileSize, byte[] fileBytes) {
        return new ChatItem(Kind.VOICE, senderLabel, timestamp, null, null, null, fileName, fileSize, fileBytes);
    }

    public static ChatItem system(String text) {
        return new ChatItem(Kind.SYSTEM, null, 0L, text, null, null, null, 0L, null);
    }

    public Kind getKind() {
        return kind;
    }

    public String getSenderLabel() {
        return senderLabel;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getText() {
        return text;
    }

    public ImageIcon getThumbnail() {
        return thumbnail;
    }

    public byte[] getFullImage() {
        return fullImage;
    }

    public String getFileName() {
        return fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public byte[] getFileBytes() {
        return fileBytes;
    }
}
