package org.sikrip.vboeditor.gui;

import com.google.common.base.Strings;
import org.sikrip.vboeditor.VboEditor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

final class VboEditorApplication extends JFrame implements ActionListener {

    private final static String VERSION_TAG = "0.6Beta";

    private static final String APP_TITLE = "Vbo Tools";

    private final JTabbedPane tabs = new JTabbedPane();
    private final JPanel telemetryVideoIntegrationPanel = new JPanel(new BorderLayout());
    private final JDialog waitDialog;

    private final SynchronizationPanel synchronizationPanel;

    private final JTextField outputDirPath = new JTextField();
    private final JButton outputDirChoose = new JButton("...");
    private final JTextField sessionName = new JTextField();
    private final JCheckBox syncLock = new JCheckBox("Lock video / telemetry data");
    private final JButton performIntegration = new JButton("Integrate telemetry / video data");

    private final JTextArea logText = new JTextArea();

    private VboEditorApplication() throws HeadlessException {
        this.synchronizationPanel = new SynchronizationPanel(this);
        waitDialog = new JDialog(this, true);
    }

    private void createGui() {

        waitDialog.getContentPane().add(new JLabel("<html><h2>Working, please wait...</h2></html>"));

        telemetryVideoIntegrationPanel.setPreferredSize(new Dimension(840, 580));
        telemetryVideoIntegrationPanel.add(synchronizationPanel, BorderLayout.CENTER);
        telemetryVideoIntegrationPanel.add(createSouthPanel(), BorderLayout.SOUTH);

        tabs.add("Telemetry/video integration", telemetryVideoIntegrationPanel);
        tabs.add("About", createAboutPanel());

        setTitle(APP_TITLE + " (" + VERSION_TAG + ")");
        getContentPane().add(tabs);
    }

    private JPanel createSouthPanel() {

        final JPanel southPanel = new JPanel(new BorderLayout());

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));
        JLabel label = new JLabel("Output folder: ");
        panel.add(label);
        panel.add(outputDirPath);
        panel.add(outputDirChoose);

        String toolTipText = "Select a folder to place the result files.";
        label.setToolTipText(toolTipText);
        outputDirPath.setToolTipText(toolTipText);
        outputDirChoose.setToolTipText(toolTipText);

        panel.add(Box.createRigidArea(new Dimension(5, 0)));

        label = new JLabel("Session Name: ");
        sessionName.setToolTipText(toolTipText);
        panel.add(label);
        panel.add(sessionName);

        toolTipText = "<html>Select a name for the session of the .vbo and video data. "
                + "A folder with this name will be created under the output directory " +
                "and will contain the final .vbo and video files.</html>";
        label.setToolTipText(toolTipText);
        sessionName.setToolTipText(toolTipText);
        southPanel.add(panel, BorderLayout.NORTH);

        panel = new JPanel();
        panel.add(syncLock);
        panel.add(performIntegration);
        performIntegration.setEnabled(false);
        southPanel.add(panel, BorderLayout.CENTER);

        logText.setEditable(false);
        final JScrollPane logScroll = new JScrollPane(logText);
        logScroll.setPreferredSize(new Dimension(600, 100));
        logScroll.setBorder(BorderFactory.createTitledBorder("Log"));
        southPanel.add(logScroll, BorderLayout.SOUTH);

        southPanel.setBorder(BorderFactory.createEtchedBorder());

        return southPanel;
    }

    private void addActionListeners() {
        outputDirChoose.addActionListener(this);
        performIntegration.addActionListener(this);
        syncLock.addActionListener(this);
    }

    private void chooseOutputDirectory() {
        final JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            outputDirPath.setText(fileChooser.getSelectedFile().getAbsolutePath());
        }
    }

    private VboEditor.VideoType getVideoType() {
        final String videoExtension;
        final String videoFilePath = synchronizationPanel.getVideoFilePath();
        try {
            videoExtension = videoFilePath.substring(videoFilePath.lastIndexOf(".")).toLowerCase();
        } catch (StringIndexOutOfBoundsException e) {
            throw new IllegalStateException("Please select a valid video file");
        }
        final VboEditor.VideoType videoType;
        switch (videoExtension) {
            case ".mp4":
                videoType = VboEditor.VideoType.MP4;
                break;
            case ".avi":
                videoType = VboEditor.VideoType.AVI;
                break;
            default:
                throw new RuntimeException(String.format("Video of type %s is not supported", videoExtension));
        }
        return videoType;
    }

    private void integrateGpsAndVideo() {

        try {
            synchronizationPanel.pause();

            final String outputDir = outputDirPath.getText();
            final String vboFilePath = synchronizationPanel.getTelemetryFilePath();
            final String sessionName = this.sessionName.getText();
            final String videoFilePath = synchronizationPanel.getVideoFilePath();

            validateInput(outputDir, vboFilePath, sessionName, videoFilePath);

            final VboEditor.VideoType videoType = getVideoType();

            final long gpsDataTotalOffsetMillis = synchronizationPanel.getTelemetryDataOffset();

            final Component messageDialogParent = this;
            waitDialog.setUndecorated(true);
            waitDialog.pack();
            waitDialog.setLocationRelativeTo(this);
            SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() {
                    try {
                        VboEditor.createVboWithVideoMetadata(outputDir, vboFilePath, videoType, sessionName, (int) gpsDataTotalOffsetMillis);
                        VboEditor.createVideoFile(outputDir, videoFilePath, sessionName);
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(messageDialogParent, e.getMessage(), "An error occurred", JOptionPane.ERROR_MESSAGE);
                        return null;
                    }
                    appendLog("Vbo and video files created under " + outputDir + "/" + sessionName);
                    appendLog("\n");
                    JOptionPane.showMessageDialog(messageDialogParent,
                            "Check " + outputDir + "/" + sessionName + " for video and vbo files!", "Done!", JOptionPane.INFORMATION_MESSAGE);
                    return null;
                }

                @Override
                protected void done() {
                    waitDialog.dispose();
                }
            };
            worker.execute();
            waitDialog.setVisible(true);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "An error occurred", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void validateInput(String outputDir, String vboFilePath, String sessionName, String videoFilePath) {
        if (Strings.isNullOrEmpty(vboFilePath)) {
            throw new IllegalStateException("Please select a valid vbo file");
        }
        if (Strings.isNullOrEmpty(videoFilePath)) {
            throw new IllegalStateException("Please select a valid video file");
        }
        final File outputDirFile = new File(outputDir);
        if (!outputDirFile.exists() || !outputDirFile.isDirectory()) {
            throw new IllegalStateException("Please select a valid output directory");
        }
        if (Strings.isNullOrEmpty(sessionName)) {
            throw new IllegalStateException("Please select a valid session name");
        } else {
            final File finalPath = new File(outputDir + "/" + sessionName);
            if (!finalPath.exists() && !finalPath.mkdir()) {
                throw new IllegalStateException("Session name should be a valid folder name");
            }
        }
    }

    boolean isDataLocked(){
        return syncLock.isSelected();
    }

    private void appendLog(String log) {
        logText.append(log);
        logText.append("\n");
    }

    private JPanel createAboutPanel() {
        final String aboutMessage = "<html>" +
                "<h2>" + APP_TITLE + "</h2>" +
                "<h2>Version " + VERSION_TAG + "</h2>" +
                "A toolset for the .vbo telemetry format including:" +
                "<ul>" +
                "<li>A tool that can help you sync and integrate Telemetry and Video data so you can do video analysis on Circuit Tools.</li>" +
                "<li>More to come!</li>" +
                "</ul>" +
                "<p>Author George Sikalias (sikrip)</p>" +
                "<p>Contact Info: " +
                "sikrip@gmail.com, " +
                "facebook.com/sikrip, " +
                "twitter @sikrip</p>" +
                "</html>";
        final JPanel aboutPanel = new JPanel();

        aboutPanel.add(new JLabel(aboutMessage));
        return aboutPanel;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        final Object source = e.getSource();

        if (source == outputDirChoose) {
            chooseOutputDirectory();
        } else if (source == performIntegration) {
            integrateGpsAndVideo();
        } else if (source == syncLock) {
            toggleSyncLock();
        }
    }

    /*private void playPause() {
        videoPlayer.playPause();
        playing = !playing;

        playPauseAll.setText(playing ? "Pause" : "Play");

        prev2.setEnabled(!playing);
        prev.setEnabled(!playing);
        next.setEnabled(!playing);
        next2.setEnabled(!playing);
    }*/

    private void toggleSyncLock() {
        if (syncLock.isSelected()) {
            synchronizationPanel.lock();
            appendLog(String.format("Offset is %sms", synchronizationPanel.getTelemetryDataOffset()));
        } else {
            synchronizationPanel.unlock();
        }
        performIntegration.setEnabled(syncLock.isSelected());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                final VboEditorApplication editorGui = new VboEditorApplication();
                editorGui.createGui();
                editorGui.addActionListeners();
                editorGui.pack();
                editorGui.setLocationRelativeTo(null);
                editorGui.setResizable(false);
                editorGui.setVisible(true);
                editorGui.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            }
        });
    }
}
