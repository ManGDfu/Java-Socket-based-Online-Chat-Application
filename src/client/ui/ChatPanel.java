package client.ui;

import util.FileUtil;
import util.ImageUtil;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.UnsupportedAudioFileException;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 聊天内容区：上方只读历史消息（支持文字、图片与文件），下方输入框、图片/文件按钮与发送按钮。
 */
public class ChatPanel extends JPanel {

    /** 用户点击发送或回车时的回调。 */
    public interface SendListener {
        void onSend(String text);
    }

    /** 用户点击「图片」按钮时的回调。 */
    public interface ImageListener {
        void onPickImage();
    }

    /** 用户点击「文件」按钮时的回调。 */
    public interface FileListener {
        void onPickFile();
    }

    /** 用户点击「语音」按钮时的回调。 */
    public interface VoiceListener {
        void onPickVoice();
    }

    /** 用户点击「录音」按钮时的回调。 */
    public interface RecordVoiceListener {
        void onRecordVoice();
    }

    private static final Color BG = new Color(0xF2F2F2);

    private final JTextPane chatArea = new JTextPane();
    private final JTextField inputField = new JTextField();
    private final JButton imageButton = new JButton("图片");
    private final JButton fileButton = new JButton("文件");
    private final JButton voiceButton = new JButton("语音");
    private final JButton recordButton = new JButton("录音");
    private final JButton sendButton = new JButton("发送");

    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
    private static final SimpleAttributeSet SAVE_LINK_STYLE = new SimpleAttributeSet();

    static {
        StyleConstants.setForeground(SAVE_LINK_STYLE, new Color(0x0066CC));
        StyleConstants.setUnderline(SAVE_LINK_STYLE, true);
    }

    private String peerUserId;
    private SendListener sendListener;
    private ImageListener imageListener;
    private FileListener fileListener;
    private VoiceListener voiceListener;
    private RecordVoiceListener recordVoiceListener;
    private final List<ImageClickTarget> imageClickTargets = new ArrayList<ImageClickTarget>();
    private final List<FileSaveTarget> fileSaveTargets = new ArrayList<FileSaveTarget>();
    private final List<VoicePlayTarget> voicePlayTargets = new ArrayList<VoicePlayTarget>();
    private Clip currentClip;

    public ChatPanel() {
        initUI();
    }

    private static final class ImageClickTarget {
        private final javax.swing.ImageIcon icon;
        private final byte[] fullImage;
        private final String fileName;

        private ImageClickTarget(javax.swing.ImageIcon icon, byte[] fullImage, String fileName) {
            this.icon = icon;
            this.fullImage = fullImage;
            this.fileName = fileName;
        }
    }

    private static final class FileSaveTarget {
        private final int start;
        private final int end;
        private final byte[] fileBytes;
        private final String fileName;

        private FileSaveTarget(int start, int end, byte[] fileBytes, String fileName) {
            this.start = start;
            this.end = end;
            this.fileBytes = fileBytes;
            this.fileName = fileName;
        }
    }

    private static final class VoicePlayTarget {
        private final int start;
        private final int end;
        private final byte[] voiceBytes;
        private final String fileName;

        private VoicePlayTarget(int start, int end, byte[] voiceBytes, String fileName) {
            this.start = start;
            this.end = end;
            this.voiceBytes = voiceBytes;
            this.fileName = fileName;
        }
    }

    private void initUI() {
        setLayout(new BorderLayout());
        setBackground(BG);

        chatArea.setEditable(false);
        chatArea.setBackground(Color.WHITE);
        chatArea.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        add(new JScrollPane(chatArea), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(6, 0));
        bottom.setBackground(BG);
        bottom.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        bottom.add(inputField, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        actions.setOpaque(false);
        actions.add(imageButton);
        actions.add(fileButton);
        actions.add(voiceButton);
        actions.add(recordButton);
        actions.add(sendButton);
        bottom.add(actions, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);

        ActionListener sendAction = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doSend();
            }
        };
        sendButton.addActionListener(sendAction);
        inputField.addActionListener(sendAction);
        imageButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (imageListener != null) {
                    imageListener.onPickImage();
                }
            }
        });
        fileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (fileListener != null) {
                    fileListener.onPickFile();
                }
            }
        });
        voiceButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (voiceListener != null) {
                    voiceListener.onPickVoice();
                }
            }
        });
        recordButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (recordVoiceListener != null) {
                    recordVoiceListener.onRecordVoice();
                }
            }
        });

        chatArea.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                handleImageClick(e);
                handleFileSaveClick(e);
                handleVoicePlayClick(e);
            }
        });
        chatArea.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                updateSaveLinkCursor(e);
            }
        });
    }

    public void setSendListener(SendListener listener) {
        this.sendListener = listener;
    }

    public void setImageListener(ImageListener listener) {
        this.imageListener = listener;
    }

    public void setFileListener(FileListener listener) {
        this.fileListener = listener;
    }

    public void setVoiceListener(VoiceListener listener) {
        this.voiceListener = listener;
    }

    public void setRecordVoiceListener(RecordVoiceListener listener) {
        this.recordVoiceListener = listener;
    }

    public String getPeerUserId() {
        return peerUserId;
    }

    /** 切换当前会话对象，并加载该会话的历史记录。 */
    public void showConversation(String peerUserId, List<ChatItem> items) {
        this.peerUserId = peerUserId;
        chatArea.setText("");
        imageClickTargets.clear();
        fileSaveTargets.clear();
        voicePlayTargets.clear();
        if (items != null) {
            for (ChatItem item : items) {
                renderItem(item);
            }
        }
        scrollToBottom();
    }

    /** 在当前会话末尾追加一行文字聊天消息。 */
    public void appendChatLine(String senderLabel, long timestamp, String content) {
        appendTextItem(senderLabel, timestamp, content);
    }

    /** 在当前会话末尾追加一条图片消息。 */
    public void appendImageLine(String senderLabel, long timestamp,
                                javax.swing.ImageIcon thumbnail, byte[] fullImage, String fileName) {
        appendImageItem(senderLabel, timestamp, thumbnail, fullImage, fileName);
    }

    /** 在当前会话末尾追加一条文件消息。 */
    public void appendFileLine(String senderLabel, long timestamp, String fileName,
                               long fileSize, byte[] fileBytes) {
        appendFileItem(senderLabel, timestamp, fileName, fileSize, fileBytes);
    }

    /** 在当前会话末尾追加一条语音消息。 */
    public void appendVoiceLine(String senderLabel, long timestamp, String fileName,
                                long fileSize, byte[] voiceBytes) {
        appendVoiceItem(senderLabel, timestamp, fileName, fileSize, voiceBytes);
    }

    /** 追加一条系统提示。 */
    public void appendSystemMessage(String content) {
        appendSystemItem(content);
    }

    public void clearInput() {
        inputField.setText("");
    }

    public void setInputEnabled(boolean enabled) {
        inputField.setEnabled(enabled);
        sendButton.setEnabled(enabled);
        imageButton.setEnabled(enabled);
        fileButton.setEnabled(enabled);
        voiceButton.setEnabled(enabled);
        recordButton.setEnabled(enabled);
    }

    private void doSend() {
        String text = inputField.getText().trim();
        if (text.isEmpty() || sendListener == null) {
            return;
        }
        sendListener.onSend(text);
    }

    private void appendTextItem(String senderLabel, long timestamp, String content) {
        if (content == null) {
            return;
        }
        String time = timeFormat.format(new Date(timestamp));
        insertText("[" + time + "] " + senderLabel + "：" + content + "\n");
        scrollToBottom();
    }

    private void appendImageItem(String senderLabel, long timestamp,
                                 javax.swing.ImageIcon thumbnail, byte[] fullImage, String fileName) {
        if (thumbnail == null) {
            appendTextItem(senderLabel, timestamp, "[图片加载失败]");
            return;
        }
        String time = timeFormat.format(new Date(timestamp));
        insertText("[" + time + "] " + senderLabel + "：");
        insertIcon(thumbnail, fullImage, fileName);
        insertText("\n");
        scrollToBottom();
    }

    private void appendFileItem(String senderLabel, long timestamp, String fileName,
                                long fileSize, byte[] fileBytes) {
        if (fileName == null || fileName.trim().isEmpty()) {
            fileName = "未命名文件";
        }
        String time = timeFormat.format(new Date(timestamp));
        long actualSize = fileSize > 0 ? fileSize : (fileBytes != null ? fileBytes.length : 0);
        String sizeText = FileUtil.humanSize(actualSize);
        insertText("[" + time + "] " + senderLabel + "：[文件] " + fileName + " (" + sizeText + ")");
        boolean canSave = fileBytes != null && fileBytes.length > 0
                && senderLabel != null && !"我".equals(senderLabel);
        if (canSave) {
            insertText(" ");
            int start = chatArea.getStyledDocument().getLength();
            insertStyledText("[保存到本地]", SAVE_LINK_STYLE);
            int end = chatArea.getStyledDocument().getLength();
            fileSaveTargets.add(new FileSaveTarget(start, end, fileBytes, fileName));
        }
        insertText("\n");
        scrollToBottom();
    }

    private void appendVoiceItem(String senderLabel, long timestamp, String fileName,
                                 long fileSize, byte[] voiceBytes) {
        if (fileName == null || fileName.trim().isEmpty()) {
            fileName = "未命名语音";
        }
        String time = timeFormat.format(new Date(timestamp));
        long actualSize = fileSize > 0 ? fileSize : (voiceBytes != null ? voiceBytes.length : 0);
        String sizeText = FileUtil.humanSize(actualSize);
        insertText("[" + time + "] " + senderLabel + "：[语音] " + fileName + " (" + sizeText + ")");
        boolean canPlay = voiceBytes != null && voiceBytes.length > 0;
        if (canPlay) {
            insertText(" ");
            int start = chatArea.getStyledDocument().getLength();
            insertStyledText("[播放]", SAVE_LINK_STYLE);
            int end = chatArea.getStyledDocument().getLength();
            voicePlayTargets.add(new VoicePlayTarget(start, end, voiceBytes, fileName));
        }
        insertText("\n");
        scrollToBottom();
    }

    private void appendSystemItem(String content) {
        if (content == null || content.isEmpty()) {
            return;
        }
        insertText("[系统] " + content + "\n");
        scrollToBottom();
    }

    private void renderItem(ChatItem item) {
        if (item == null) {
            return;
        }
        switch (item.getKind()) {
            case TEXT:
                appendTextItem(item.getSenderLabel(), item.getTimestamp(), item.getText());
                break;
            case IMAGE:
                appendImageItem(item.getSenderLabel(), item.getTimestamp(),
                        item.getThumbnail(), item.getFullImage(), item.getFileName());
                break;
            case FILE:
                appendFileItem(item.getSenderLabel(), item.getTimestamp(),
                        item.getFileName(), item.getFileSize(), item.getFileBytes());
                break;
            case VOICE:
                appendVoiceItem(item.getSenderLabel(), item.getTimestamp(),
                        item.getFileName(), item.getFileSize(), item.getFileBytes());
                break;
            case SYSTEM:
                appendSystemItem(item.getText());
                break;
            default:
                break;
        }
    }

    private void insertText(String text) {
        StyledDocument doc = chatArea.getStyledDocument();
        try {
            doc.insertString(doc.getLength(), text, null);
        } catch (BadLocationException ignored) {
        }
    }

    private void insertStyledText(String text, SimpleAttributeSet style) {
        StyledDocument doc = chatArea.getStyledDocument();
        try {
            doc.insertString(doc.getLength(), text, style);
        } catch (BadLocationException ignored) {
        }
    }

    private void insertIcon(javax.swing.ImageIcon icon, byte[] fullImage, String fileName) {
        chatArea.setCaretPosition(chatArea.getStyledDocument().getLength());
        chatArea.insertIcon(icon);
        imageClickTargets.add(new ImageClickTarget(icon, fullImage, fileName));
    }

    private void handleImageClick(MouseEvent e) {
        int offset = chatArea.viewToModel(e.getPoint());
        if (offset < 0) {
            return;
        }
        StyledDocument doc = chatArea.getStyledDocument();
        javax.swing.text.Element element = doc.getCharacterElement(offset);
        if (element == null) {
            return;
        }
        Object attr = element.getAttributes().getAttribute(StyleConstants.IconAttribute);
        if (!(attr instanceof javax.swing.ImageIcon)) {
            return;
        }
        javax.swing.ImageIcon clicked = (javax.swing.ImageIcon) attr;
        for (ImageClickTarget target : imageClickTargets) {
            if (target.icon == clicked) {
                showImagePreview(target.fullImage, target.fileName);
                return;
            }
        }
    }

    private void showImagePreview(byte[] fullImage, String fileName) {
        if (fullImage == null || fullImage.length == 0) {
            return;
        }
        javax.swing.ImageIcon fullIcon = ImageUtil.toIcon(fullImage);
        if (fullIcon == null) {
            return;
        }
        JDialog dialog = new JDialog(javax.swing.SwingUtilities.getWindowAncestor(this), "图片预览", JDialog.ModalityType.APPLICATION_MODAL);
        JLabel label = new JLabel(fullIcon);
        label.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        dialog.add(label, BorderLayout.CENTER);
        if (fileName != null && !fileName.isEmpty()) {
            JLabel nameLabel = new JLabel(fileName);
            nameLabel.setBorder(BorderFactory.createEmptyBorder(4, 8, 8, 8));
            dialog.add(nameLabel, BorderLayout.SOUTH);
        }
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void handleFileSaveClick(MouseEvent e) {
        int offset = chatArea.viewToModel(e.getPoint());
        if (offset < 0) {
            return;
        }
        for (FileSaveTarget target : fileSaveTargets) {
            if (offset >= target.start && offset < target.end) {
                saveFileToLocal(target.fileBytes, target.fileName);
                return;
            }
        }
    }

    private void handleVoicePlayClick(MouseEvent e) {
        int offset = chatArea.viewToModel(e.getPoint());
        if (offset < 0) {
            return;
        }
        for (VoicePlayTarget target : voicePlayTargets) {
            if (offset >= target.start && offset < target.end) {
                playVoice(target.voiceBytes, target.fileName);
                return;
            }
        }
    }

    private void updateSaveLinkCursor(MouseEvent e) {
        int offset = chatArea.viewToModel(e.getPoint());
        boolean onLink = false;
        if (offset >= 0) {
            for (FileSaveTarget target : fileSaveTargets) {
                if (offset >= target.start && offset < target.end) {
                    onLink = true;
                    break;
                }
            }
            if (!onLink) {
                for (VoicePlayTarget target : voicePlayTargets) {
                    if (offset >= target.start && offset < target.end) {
                        onLink = true;
                        break;
                    }
                }
            }
        }
        chatArea.setCursor(onLink ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
    }

    private void playVoice(byte[] bytes, String fileName) {
        if (bytes == null || bytes.length == 0) {
            JOptionPane.showMessageDialog(this, "语音数据为空，无法播放。", "错误",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        stopCurrentClip();
        try {
            AudioInputStream audioIn = AudioSystem.getAudioInputStream(new ByteArrayInputStream(bytes));
            Clip clip = AudioSystem.getClip();
            clip.addLineListener(new LineListener() {
                @Override
                public void update(LineEvent event) {
                    if (event.getType() == LineEvent.Type.STOP) {
                        clip.close();
                        if (currentClip == clip) {
                            currentClip = null;
                        }
                    }
                }
            });
            clip.open(audioIn);
            currentClip = clip;
            clip.start();
        } catch (UnsupportedAudioFileException ex) {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "当前格式无法直接播放，是否保存到本地？",
                    "无法播放", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) {
                saveFileToLocal(bytes, fileName);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "播放失败：" + ex.getMessage(), "错误",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void stopCurrentClip() {
        if (currentClip != null) {
            try {
                if (currentClip.isRunning()) {
                    currentClip.stop();
                }
                currentClip.close();
            } catch (Exception ignored) {
            }
            currentClip = null;
        }
    }

    private void saveFileToLocal(byte[] fileBytes, String fileName) {
        if (fileBytes == null || fileBytes.length == 0) {
            JOptionPane.showMessageDialog(this, "文件数据为空，无法保存。", "错误",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("选择保存目录");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setApproveButtonText("选择");
        int result = chooser.showSaveDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File dir = chooser.getSelectedFile();
        if (dir == null || !dir.isDirectory()) {
            JOptionPane.showMessageDialog(this, "请选择有效的目录。", "提示",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        File target = new File(dir, fileName);
        if (target.exists()) {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "目录中已存在同名文件，是否覆盖？", "确认覆盖",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm != JOptionPane.YES_OPTION) {
                return;
            }
        }
        try {
            FileUtil.writeFileBytes(target.getAbsolutePath(), fileBytes);
            JOptionPane.showMessageDialog(this, "文件已保存到：\n" + target.getAbsolutePath(),
                    "保存成功", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "保存失败：" + ex.getMessage(), "错误",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void scrollToBottom() {
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }
}
