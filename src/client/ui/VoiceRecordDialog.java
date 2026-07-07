package client.ui;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Window;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * 简单录音对话框：使用麦克风录制 wav 并发送。
 */
public class VoiceRecordDialog extends JDialog {

    public interface SendListener {
        void onSend(byte[] wavBytes, String fileName);
    }

    private static final AudioFormat FORMAT = new AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            16000.0f,
            16,
            1,
            2,
            16000.0f,
            false);

    private final JLabel statusLabel = new JLabel("点击「开始录音」录制语音");
    private final JButton startButton = new JButton("开始录音");
    private final JButton stopButton = new JButton("停止");
    private final JButton sendButton = new JButton("发送");
    private final JButton cancelButton = new JButton("取消");

    private TargetDataLine line;
    private Thread recordThread;
    private ByteArrayOutputStream pcmBuffer;
    private volatile boolean recording;
    private byte[] recordedWav;
    private SendListener sendListener;

    public VoiceRecordDialog(Window owner) {
        super(owner, "录音", ModalityType.APPLICATION_MODAL);
        initUI();
    }

    public void setSendListener(SendListener listener) {
        this.sendListener = listener;
    }

    private void initUI() {
        setSize(360, 160);
        setLocationRelativeTo(getOwner());

        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        statusLabel.setHorizontalAlignment(JLabel.CENTER);
        root.add(statusLabel, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        stopButton.setEnabled(false);
        sendButton.setEnabled(false);
        actions.add(startButton);
        actions.add(stopButton);
        actions.add(sendButton);
        actions.add(cancelButton);
        root.add(actions, BorderLayout.SOUTH);

        setContentPane(root);

        startButton.addActionListener(e -> startRecording());
        stopButton.addActionListener(e -> stopRecording());
        sendButton.addActionListener(e -> doSend());
        cancelButton.addActionListener(e -> dispose());
    }

    private void startRecording() {
        if (recording) {
            return;
        }
        try {
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, FORMAT);
            if (!AudioSystem.isLineSupported(info)) {
                JOptionPane.showMessageDialog(this, "当前系统不支持麦克风录音。", "错误",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            line = (TargetDataLine) AudioSystem.getLine(info);
            line.open(FORMAT);
            line.start();
            pcmBuffer = new ByteArrayOutputStream();
            recording = true;
            recordedWav = null;

            recordThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    byte[] buf = new byte[4096];
                    while (recording) {
                        int read = line.read(buf, 0, buf.length);
                        if (read > 0) {
                            pcmBuffer.write(buf, 0, read);
                        }
                    }
                }
            }, "voice-record");
            recordThread.setDaemon(true);
            recordThread.start();

            statusLabel.setText("正在录音…");
            startButton.setEnabled(false);
            stopButton.setEnabled(true);
            sendButton.setEnabled(false);
        } catch (LineUnavailableException ex) {
            JOptionPane.showMessageDialog(this, "麦克风不可用：" + ex.getMessage(), "错误",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void stopRecording() {
        if (!recording) {
            return;
        }
        recording = false;
        if (line != null) {
            line.stop();
            line.close();
            line = null;
        }
        if (recordThread != null) {
            try {
                recordThread.join(2000);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            recordThread = null;
        }
        try {
            recordedWav = pcmToWav(pcmBuffer.toByteArray());
            statusLabel.setText("录音完成，可点击「发送」");
            sendButton.setEnabled(recordedWav != null && recordedWav.length > 0);
        } catch (IOException ex) {
            recordedWav = null;
            statusLabel.setText("录音失败");
            JOptionPane.showMessageDialog(this, "录音处理失败：" + ex.getMessage(), "错误",
                    JOptionPane.ERROR_MESSAGE);
        }
        startButton.setEnabled(true);
        stopButton.setEnabled(false);
    }

    private byte[] pcmToWav(byte[] pcmData) throws IOException {
        if (pcmData == null || pcmData.length == 0) {
            throw new IOException("录音数据为空");
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        AudioInputStream audioIn = new AudioInputStream(
                new ByteArrayInputStream(pcmData), FORMAT, pcmData.length / FORMAT.getFrameSize());
        AudioSystem.write(audioIn, AudioFileFormat.Type.WAVE, out);
        return out.toByteArray();
    }

    private void doSend() {
        if (recordedWav == null || recordedWav.length == 0) {
            JOptionPane.showMessageDialog(this, "请先完成录音。", "提示",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (sendListener != null) {
            String fileName = "录音_" + System.currentTimeMillis() + ".wav";
            sendListener.onSend(recordedWav, fileName);
        }
        dispose();
    }
}
