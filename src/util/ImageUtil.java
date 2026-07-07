package util;

import common.FilePacket;

import javax.swing.ImageIcon;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * 图片读取、校验与缩略图生成工具。
 */
public final class ImageUtil {

    public static final int THUMB_MAX_WIDTH = 200;
    public static final int THUMB_MAX_HEIGHT = 200;
    public static final int LIST_AVATAR_SIZE = 32;
    public static final int PROFILE_AVATAR_SIZE = 80;
    public static final long MAX_IMAGE_BYTES = 5L * 1024L * 1024L;
    public static final String CLIENT_AVATAR_DIR = "downloads/avatars/";

    private static final String[] IMAGE_EXTENSIONS = {"jpg", "jpeg", "png", "gif"};

    private ImageUtil() {
    }

    public static boolean isImageFile(File file) {
        if (file == null || !file.isFile()) {
            return false;
        }
        String ext = FileUtil.extensionOf(file.getName());
        for (String allowed : IMAGE_EXTENSIONS) {
            if (allowed.equalsIgnoreCase(ext)) {
                return true;
            }
        }
        return false;
    }

    public static FilePacket readImage(File file) throws IOException {
        if (!isImageFile(file)) {
            throw new IOException("不支持的图片格式，仅支持 jpg/png/gif");
        }
        byte[] data = FileUtil.readFileBytes(file);
        String ext = FileUtil.extensionOf(file.getName());
        return new FilePacket(file.getName(), data.length, ext, data);
    }

    public static ImageIcon toIcon(byte[] data) {
        if (data == null || data.length == 0) {
            return null;
        }
        return new ImageIcon(data);
    }

    public static ImageIcon toThumbnail(byte[] data, int maxW, int maxH) {
        ImageIcon icon = toIcon(data);
        if (icon == null) {
            return null;
        }
        int width = icon.getIconWidth();
        int height = icon.getIconHeight();
        if (width <= 0 || height <= 0) {
            return icon;
        }
        double scale = Math.min((double) maxW / width, (double) maxH / height);
        if (scale >= 1.0) {
            return icon;
        }
        int newW = Math.max(1, (int) Math.round(width * scale));
        int newH = Math.max(1, (int) Math.round(height * scale));
        Image scaled = icon.getImage().getScaledInstance(newW, newH, Image.SCALE_SMOOTH);
        return new ImageIcon(scaled);
    }

    public static ImageIcon toThumbnail(byte[] data) {
        return toThumbnail(data, THUMB_MAX_WIDTH, THUMB_MAX_HEIGHT);
    }

    /** 生成默认灰色圆形头像。 */
    public static ImageIcon defaultAvatar(int size) {
        int s = Math.max(16, size);
        BufferedImage img = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(0xB0B0B0));
        g.fillOval(0, 0, s, s);
        g.setColor(Color.WHITE);
        int head = s / 3;
        g.fillOval((s - head) / 2, s / 5, head, head);
        g.fillRoundRect(s / 4, s / 2, s / 2, s / 3, s / 6, s / 6);
        g.dispose();
        return new ImageIcon(img);
    }

    public static ImageIcon defaultAvatar() {
        return defaultAvatar(LIST_AVATAR_SIZE);
    }

    /** 将图标缩放到指定边长。 */
    public static ImageIcon scaleIcon(ImageIcon icon, int size) {
        if (icon == null || size <= 0) {
            return defaultAvatar(size);
        }
        Image scaled = icon.getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH);
        return new ImageIcon(scaled);
    }

    /** 从本地路径加载头像，失败时返回默认图。 */
    public static ImageIcon avatarFromPath(String path, int size) {
        if (path == null || path.trim().isEmpty()) {
            return defaultAvatar(size);
        }
        File file = new File(path.trim());
        if (!file.isFile()) {
            return defaultAvatar(size);
        }
        try {
            ImageIcon icon = new ImageIcon(FileUtil.readFileBytes(file));
            if (icon.getIconWidth() <= 0 || icon.getIconHeight() <= 0) {
                return defaultAvatar(size);
            }
            return scaleIcon(icon, size);
        } catch (IOException e) {
            return defaultAvatar(size);
        }
    }

    /** 按用户 ID 在客户端缓存目录查找头像。 */
    public static ImageIcon avatarForUser(String userId, String avatarPath, int size) {
        if (avatarPath != null && !avatarPath.trim().isEmpty()) {
            File file = new File(avatarPath.trim());
            if (file.isFile()) {
                return avatarFromPath(avatarPath, size);
            }
        }
        if (userId != null && !userId.trim().isEmpty()) {
            File cached = findCachedAvatar(userId.trim());
            if (cached != null) {
                return avatarFromPath(cached.getPath(), size);
            }
        }
        return defaultAvatar(size);
    }

    private static File findCachedAvatar(String userId) {
        File dir = new File(CLIENT_AVATAR_DIR);
        if (!dir.isDirectory()) {
            return null;
        }
        String prefix = userId + ".";
        File[] files = dir.listFiles();
        if (files == null) {
            return null;
        }
        for (File file : files) {
            if (file.isFile() && file.getName().startsWith(prefix)) {
                return file;
            }
        }
        return null;
    }

    /** 将头像字节保存到客户端缓存目录，返回本地路径。 */
    public static String saveClientAvatar(String userId, byte[] data, String fileName) throws IOException {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IOException("用户 ID 不能为空");
        }
        if (data == null || data.length == 0) {
            throw new IOException("头像数据为空");
        }
        String ext = FileUtil.extensionOf(fileName);
        if (ext.isEmpty()) {
            ext = "png";
        }
        FileUtil.ensureDir(CLIENT_AVATAR_DIR);
        String path = CLIENT_AVATAR_DIR + userId.trim() + "." + ext;
        FileUtil.writeFileBytes(path, data);
        return path;
    }
}
