package server;

import common.Message;
import util.FileUtil;
import util.ImageUtil;

import java.io.IOException;

/**
 * 负责图片、普通文件等二进制内容的保存。
 */
public class FileTransferManager {

    public static final String IMAGE_DIR = "uploads/images/";
    public static final String FILE_DIR = "uploads/files/";
    public static final String VOICE_DIR = "uploads/voices/";
    public static final String AVATAR_DIR = "uploads/avatars/";
    public static final long MAX_IMAGE_BYTES = ImageUtil.MAX_IMAGE_BYTES;
    public static final long MAX_FILE_BYTES = 10L * 1024L * 1024L;
    public static final long MAX_VOICE_BYTES = 10L * 1024L * 1024L;

    /**
     * 将消息中的图片字节保存到服务器目录。
     *
     * @return 服务器上的保存路径
     */
    public String saveImage(Message message) throws IOException {
        if (message == null) {
            throw new IOException("消息不能为空");
        }
        byte[] data = message.getFileBytes();
        if (data == null || data.length == 0) {
            throw new IOException("图片数据为空");
        }
        if (data.length > MAX_IMAGE_BYTES) {
            throw new IOException("图片过大，最大允许 " + FileUtil.humanSize(MAX_IMAGE_BYTES));
        }
        String fileName = message.getFileName();
        if (fileName == null || fileName.trim().isEmpty()) {
            fileName = "image.png";
        }
        FileUtil.ensureDir(IMAGE_DIR);
        String savedName = FileUtil.uniqueName(fileName);
        String path = IMAGE_DIR + savedName;
        FileUtil.writeFileBytes(path, data);
        return path;
    }

    /**
     * 将消息中的普通文件字节保存到服务器目录。
     *
     * @return 服务器上的保存路径
     */
    public String saveFile(Message message) throws IOException {
        if (message == null) {
            throw new IOException("消息不能为空");
        }
        byte[] data = message.getFileBytes();
        if (data == null || data.length == 0) {
            throw new IOException("文件数据为空");
        }
        if (data.length > MAX_FILE_BYTES) {
            throw new IOException("文件过大，最大允许 " + FileUtil.humanSize(MAX_FILE_BYTES));
        }
        String fileName = message.getFileName();
        if (fileName == null || fileName.trim().isEmpty()) {
            fileName = "file.bin";
        }
        FileUtil.ensureDir(FILE_DIR);
        String savedName = FileUtil.uniqueName(fileName);
        String path = FILE_DIR + savedName;
        FileUtil.writeFileBytes(path, data);
        return path;
    }

    /**
     * 将消息中的语音字节保存到服务器目录。
     *
     * @return 服务器上的保存路径
     */
    public String saveVoice(Message message) throws IOException {
        if (message == null) {
            throw new IOException("消息不能为空");
        }
        byte[] data = message.getFileBytes();
        if (data == null || data.length == 0) {
            throw new IOException("语音数据为空");
        }
        if (data.length > MAX_VOICE_BYTES) {
            throw new IOException("语音文件过大，最大允许 " + FileUtil.humanSize(MAX_VOICE_BYTES));
        }
        String fileName = message.getFileName();
        if (fileName == null || fileName.trim().isEmpty()) {
            fileName = "voice.wav";
        }
        FileUtil.ensureDir(VOICE_DIR);
        String savedName = FileUtil.uniqueName(fileName);
        String path = VOICE_DIR + savedName;
        FileUtil.writeFileBytes(path, data);
        return path;
    }

    /**
     * 将用户头像保存到 uploads/avatars/，文件名为 userId.扩展名。
     *
     * @return 服务器上的保存路径
     */
    public String saveAvatar(String userId, Message message) throws IOException {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IOException("用户 ID 不能为空");
        }
        if (message == null) {
            throw new IOException("消息不能为空");
        }
        byte[] data = message.getFileBytes();
        if (data == null || data.length == 0) {
            throw new IOException("头像数据为空");
        }
        if (data.length > MAX_IMAGE_BYTES) {
            throw new IOException("头像过大，最大允许 " + FileUtil.humanSize(MAX_IMAGE_BYTES));
        }
        String ext = FileUtil.extensionOf(message.getFileName());
        if (ext.isEmpty()) {
            ext = "png";
        }
        FileUtil.ensureDir(AVATAR_DIR);
        String path = AVATAR_DIR + userId.trim() + "." + ext;
        FileUtil.writeFileBytes(path, data);
        return path;
    }
}
