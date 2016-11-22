package org.sikrip.vboeditor.gui;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.sikrip.vboeditor.VboEditor;

import com.google.common.base.Strings;

final class VboEditorGUI extends JFrame implements ActionListener {

    private final static String VERSION_TAG = "0.4Beta";

    private static final String APP_TITLE = "Telemetry and Video data integrator";

    private final JPanel mainPanel = new JPanel(new BorderLayout());

    private final JPanel inputControlsPanel = new JPanel();

    private final JButton outputDirChoose = new JButton("...");
    private final JTextField outputDirPath = new JTextField();

    private final JTextField sessionName = new JTextField();

    private final SynchronizationPanel synchronizationPanel = new SynchronizationPanel();

    private final JPanel actionControlsPanel = new JPanel();

    private final JTextArea logText = new JTextArea();
    private final JButton vboVideoIntegrate = new JButton("Integrate telemetry / video data");
    private final JButton clearAll = new JButton("Clear all");
    private final JButton about = new JButton("About");

    private void createGui() {
        createInputControlsPanel();
        createActionControlsPanel();

        mainPanel.setPreferredSize(new Dimension(800, 600));
        mainPanel.add(inputControlsPanel, BorderLayout.NORTH);
        mainPanel.add(synchronizationPanel, BorderLayout.CENTER);
        mainPanel.add(actionControlsPanel, BorderLayout.SOUTH);

        setTitle(APP_TITLE + " (" + VERSION_TAG + ")");
        setContentPane(mainPanel);
    }

    private void createInputControlsPanel() {
        inputControlsPanel.setLayout(new BoxLayout(inputControlsPanel, BoxLayout.LINE_AXIS));

        JLabel label;

        label = new JLabel("Output folder");
        inputControlsPanel.add(label);
        inputControlsPanel.add(outputDirPath);
        inputControlsPanel.add(outputDirChoose);

        String toolTipText = "Select a folder to place the result files.";
        label.setToolTipText(toolTipText);
        outputDirPath.setToolTipText(toolTipText);
        outputDirChoose.setToolTipText(toolTipText);

        inputControlsPanel.add(Box.createRigidArea(new Dimension(5, 0)));

        label = new JLabel("Session Name");
        sessionName.setToolTipText(toolTipText);
        inputControlsPanel.add(label);
        inputControlsPanel.add(sessionName);

        toolTipText = "<html>Select a name for the session of the .vbo and video data. "
                + "A folder with this name will be created under the output directory " +
                "and will contain the final .vbo and video files.</html>";
        label.setToolTipText(toolTipText);
        sessionName.setToolTipText(toolTipText);
    }

    private void createActionControlsPanel() {
        actionControlsPanel.setLayout(new BorderLayout());
        actionControlsPanel.setBorder(BorderFactory.createEtchedBorder());

        logText.setEditable(false);
        final JScrollPane logScroll = new JScrollPane(logText);
        logScroll.setPreferredSize(new Dimension(600, 100));
        logScroll.setBorder(BorderFactory.createTitledBorder("Log"));
        actionControlsPanel.add(logScroll, BorderLayout.CENTER);

        final JPanel panel = new JPanel();
        panel.add(vboVideoIntegrate);
        panel.add(clearAll);
        panel.add(about);
        actionControlsPanel.add(panel, BorderLayout.NORTH);
    }

    private void addActionListeners() {
        outputDirChoose.addActionListener(this);
        vboVideoIntegrate.addActionListener(this);
        clearAll.addActionListener(this);
        about.addActionListener(this);
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

    private void showSynchronizationDialog() {
        final Frame parentFrame = this;
        /*SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                final SynchronizationPanel synchronizerDialog = new SynchronizationPanel(parentFrame, true);
                synchronizerDialog.pack();
                synchronizerDialog.setLocationRelativeTo(parentFrame);
                synchronizerDialog.loadVideo(sourceVideoFilePath.getText());
                synchronizerDialog.loadTravelledRout(sourceVboFilePath.getText());
                synchronizerDialog.setVisible(true);

                Long offset = synchronizerDialog.getOffset();//ms
                if (offset != null) {

                    // Positive offset indicates that gps data start after video
                    offsetType.setSelectedIndex(offset >= 0 ? 0 : 1);

                    offset = Math.abs(offset);

                    long minutes = offset / (1000 * 60);
                    long seconds = offset / 1000;
                    long millis = offset - (minutes * 1000 * 60) - (seconds * 1000);

                    gpsDataOffsetMinutes.setValue(minutes);
                    gpsDataOffsetSeconds.setValue(seconds);
                    gpsDataOffsetMillis.setValue(millis);
                }
            }
        });*/
    }

    private void integrateGpsAndVideo() {

        try {
            final String outputDir = outputDirPath.getText();
            final String vboFilePath = synchronizationPanel.getTelemetryFilePath();
            final String sessionName = this.sessionName.getText();
            final String videoFilePath = synchronizationPanel.getVideoFilePath();

            if (Strings.isNullOrEmpty(vboFilePath)) {
                throw new IllegalStateException("Please select a valid vbo file");
            }
            if (Strings.isNullOrEmpty(videoFilePath)) {
                throw new IllegalStateException("Please select a valid video file");
            }
            if (Strings.isNullOrEmpty(outputDir)) {
                throw new IllegalStateException("Please select a valid output directory");
            }
            if (Strings.isNullOrEmpty(sessionName)) {
                throw new IllegalStateException("Please select a valid session name");
            }

            final VboEditor.VideoType videoType = getVideoType();

            final long gpsDataTotalOffsetMillis = synchronizationPanel.getOffset();
            appendLog(String.format("Offeset is %s", gpsDataTotalOffsetMillis));

            VboEditor.createVboWithVideoMetadata(outputDir, vboFilePath, videoType, sessionName, (int) gpsDataTotalOffsetMillis);
            VboEditor.createVideoFile(outputDir, videoFilePath, sessionName);

            appendLog("Vbo and video files created under " + outputDir + "/" + sessionName);
            appendLog("\n");

            JOptionPane.showMessageDialog(this,
                    "Check " + outputDir + "/" + sessionName + " for video and vbo files!", "Done!", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Could not integrate data", JOptionPane.ERROR_MESSAGE);
        }
    }


    private void appendLog(String log) {
        logText.append(log);
        logText.append("\n");
    }

    private void clearAll() {
        synchronizationPanel.clear();
        outputDirPath.setText("");
        sessionName.setText("");
        logText.setText("");
    }

    private void showAboutDialog() {
        final String aboutMessage = "<html>" +
                "<h2>" + APP_TITLE + "</h2>" +
                "<h2> Version " + VERSION_TAG + "</h2>" +
                "<h4>A little tool that can help you synch and integrate GPS and Video data so you can do video analysis on Circuit Tools!</h4>" +
                "<p>Author George Sikalias (sikrip)</p>" +
                "<p>Contact Info: " +
                "sikrip@gmail.com, " +
                "facebook.com/sikrip, " +
                "@sikrip on twitter</p>" +
                "</html>";
        JOptionPane.showMessageDialog(this, aboutMessage, "About this software", JOptionPane.INFORMATION_MESSAGE);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        final Object source = e.getSource();

        if (source == outputDirChoose) {
            chooseOutputDirectory();
        } else if (source == vboVideoIntegrate) {
            integrateGpsAndVideo();
        } else if (source == clearAll) {
            clearAll();
        } else if (source == about) {
            showAboutDialog();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                final VboEditorGUI editorGui = new VboEditorGUI();
                editorGui.createGui();
                editorGui.addActionListeners();
                editorGui.pack();
                editorGui.setLocationRelativeTo(null);
                editorGui.setVisible(true);
                editorGui.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            }
        });
    }
}
