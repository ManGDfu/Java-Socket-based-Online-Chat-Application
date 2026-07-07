package util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

/**
 * 文件读写与路径工具。
 */
public final class FileUtil {

    private FileUtil() {
    }

    public static byte[] readFileBytes(File file) throws IOException {
        if (file == null || !file.isFile()) {
            throw new IOException("文件不存在或不可读：" + file);
        }
        try (FileInputStream in = new FileInputStream(file)) {
            long length = file.length();
            if (length > Integer.MAX_VALUE) {
                throw new IOException("文件过大：" + file.getName());
            }
            byte[] data = new byte[(int) length];
            int offset = 0;
            int read;
            while (offset < data.length && (read = in.read(data, offset, data.length - offset)) >= 0) {
                offset += read;
            }
            return data;
        }
    }

    public static void writeFileBytes(String path, byte[] data) throws IOException {
        if (path == null || path.trim().isEmpty()) {
            throw new IOException("保存路径不能为空");
        }
        if (data == null) {
            throw new IOException("文件数据不能为空");
        }
        File file = new File(path);
        ensureDir(file.getParent());
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(data);
            out.flush();
        }
    }

    public static void ensureDir(String dir) throws IOException {
        if (dir == null || dir.trim().isEmpty()) {
            return;
        }
        File folder = new File(dir);
        if (!folder.exists() && !folder.mkdirs()) {
            throw new IOException("无法创建目录：" + dir);
        }
    }

    public static String uniqueName(String original) {
        String safe = original != null ? original.trim() : "file";
        if (safe.isEmpty()) {
            safe = "file";
        }
        String shortId = UUID.randomUUID().toString().substring(0, 8);
        return System.currentTimeMillis() + "_" + shortId + "_" + safe;
    }

    public static String extensionOf(String name) {
        if (name == null) {
            return "";
        }
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            return "";
        }
        return name.substring(dot + 1).toLowerCase();
    }

    public static String humanSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        }
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }
}
