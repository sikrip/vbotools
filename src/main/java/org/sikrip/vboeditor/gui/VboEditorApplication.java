package org.sikrip.vboeditor.gui;

import com.google.common.base.Strings;
import org.sikrip.vboeditor.VboEditor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

final class VboEditorApplication extends JFrame implements ActionListener {

    private final static String VERSION_TAG = "0.5Beta";

    private static final String APP_TITLE = "Vbo Tools";

    private final JPanel mainPanel = new JPanel(new BorderLayout());
    private final JDialog waitDialog;

    private final JButton outputDirChoose = new JButton("...");
    private final JTextField outputDirPath = new JTextField();

    private final JTextField sessionName = new JTextField();

    private final SynchronizationPanel synchronizationPanel;


    private final JButton performIntegration = new JButton("Integrate telemetry / video data");
    private final JButton about = new JButton("About");

    private final JTextArea logText = new JTextArea();

    private VboEditorApplication() throws HeadlessException {
        this.synchronizationPanel = new SynchronizationPanel(this);
        waitDialog = new JDialog(this);
    }

    private void createGui() {

        waitDialog.getContentPane().add(new JLabel("<html><h2>Working, please wait...</h2></html>"));

        mainPanel.setPreferredSize(new Dimension(840, 560));
        mainPanel.add(synchronizationPanel, BorderLayout.CENTER);
        mainPanel.add(createSouthPanel(), BorderLayout.SOUTH);

        setTitle(APP_TITLE + " (" + VERSION_TAG + ")");
        getContentPane().add(mainPanel);
    }

    private JPanel createSouthPanel() {

        final JPanel southPanel = new JPanel(new BorderLayout());

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));
        JLabel label = new JLabel("Output folder");
        panel.add(label);
        panel.add(outputDirPath);
        panel.add(outputDirChoose);

        String toolTipText = "Select a folder to place the result files.";
        label.setToolTipText(toolTipText);
        outputDirPath.setToolTipText(toolTipText);
        outputDirChoose.setToolTipText(toolTipText);

        panel.add(Box.createRigidArea(new Dimension(5, 0)));

        label = new JLabel("Session Name");
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
        panel.add(performIntegration);
        performIntegration.setEnabled(false);
        panel.add(about);
        southPanel.add(panel, BorderLayout.CENTER);

        logText.setEditable(false);
        final JScrollPane logScroll = new JScrollPane(logText);
        logScroll.setPreferredSize(new Dimension(600, 100));
        logScroll.setBorder(BorderFactory.createTitledBorder("Log"));
        southPanel.add(logScroll, BorderLayout.SOUTH);

        return southPanel;
    }

    private void addActionListeners() {
        outputDirChoose.addActionListener(this);
        performIntegration.addActionListener(this);
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

    private void integrateGpsAndVideo() {

        try {
            synchronizationPanel.forcePause();

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
            appendLog(String.format("Offset is %s", gpsDataTotalOffsetMillis));


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
                    } catch (IOException e) {
                        JOptionPane.showMessageDialog(messageDialogParent, e.getMessage(), "An error occurred", JOptionPane.ERROR_MESSAGE);
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
            waitDialog.setVisible(true);
            worker.execute();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "An error occurred", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void appendLog(String log) {
        logText.append(log);
        logText.append("\n");
    }

    private void showAboutDialog() {
        final String aboutMessage = "<html>" +
                "<h2>" + APP_TITLE + "</h2>" +
                "<h2>Version " + VERSION_TAG + "</h2>" +
                "A toolset for the .vbo telemetry format including:" +
                "<ul>" +
                "<li>A tool that can help you sync and integrate Telemetry and Video data so you can do video analysis on Circuit Tools.</li>" +
                "<li>More to came!</li>" +
                "</ul>" +
                "<p>Author George Sikalias (sikrip)</p>" +
                "<p>Contact Info: " +
                "sikrip@gmail.com, " +
                "facebook.com/sikrip, " +
                "twitter @sikrip</p>" +
                "</html>";
        JOptionPane.showMessageDialog(this, aboutMessage, "About this software", JOptionPane.INFORMATION_MESSAGE);
    }

    void enableIntegrationAction(boolean enable) {
        performIntegration.setEnabled(enable);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        final Object source = e.getSource();

        if (source == outputDirChoose) {
            chooseOutputDirectory();
        } else if (source == performIntegration) {
            integrateGpsAndVideo();
        } else if (source == about) {
            showAboutDialog();
        }
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
