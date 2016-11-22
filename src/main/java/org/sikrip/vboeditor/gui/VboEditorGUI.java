package org.sikrip.vboeditor.gui;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.sikrip.vboeditor.VboEditor;

import com.google.common.base.Strings;

final class VboEditorGUI extends JFrame implements ActionListener {

    private final static String VERSION_TAG = "0.3Beta";

    private static final String APP_TITLE = "GPS(vbo) and Video data integrator";

    private final static String[] OFFSET_TYPES = new String[]{"After", "Before"};

    private final JPanel mainPanel = new JPanel(new BorderLayout());

    private final JPanel inputControlsPanel = new JPanel();

    private final JButton sourceVboFileChoose = new JButton("...");
    private final JTextField sourceVboFilePath = new JTextField("/home/sikripefg/sample-vbo-from-dbn.vbo");

    private final JButton sourceVideoFileChoose = new JButton("...");
    private final JTextField sourceVideoFilePath = new JTextField("/home/sikripefg/provlima-sasman.MP4");

    private final JButton outputDirChoose = new JButton("...");
    private final JTextField outputDirPath = new JTextField();

    private final JTextField sessionName = new JTextField();

    private final JPanel gpsVideoSynchPanel = new JPanel();

    private final JSpinner gpsDataOffsetMinutes = new JSpinner(new SpinnerNumberModel(
            0, // initial
            0, //min
            9999, //max
            1)); // step

    private final JSpinner gpsDataOffsetSeconds = new JSpinner(new SpinnerNumberModel(
            0, // initial
            0, //min
            59, //max
            1)); // step

    private final JSpinner gpsDataOffsetMillis = new JSpinner(new SpinnerNumberModel(
            0, // initial
            0, //min
            9999, //max
            50)); // step

    private final JComboBox offsetType = new JComboBox(OFFSET_TYPES);

    private final JButton resetSynchronizationButton = new JButton("(reset)");
    private final JButton synchronizationDialogButton = new JButton("Help me synchronize this!");

    private final JPanel actionControlsPanel = new JPanel();

    private final JTextArea logText = new JTextArea();
    private final JButton vboVideoIntegrate = new JButton("Integrate GPS and video data");
    private final JButton clearAll = new JButton("Clear all");
    private final JButton about = new JButton("About");

    private void createGui() {
        createInputControlsPanel();
        createGpsVideoSynchPanel();
        createActionControlsPanel();

        mainPanel.setPreferredSize(new Dimension(680, 440));
        mainPanel.add(inputControlsPanel, BorderLayout.NORTH);
        mainPanel.add(gpsVideoSynchPanel, BorderLayout.CENTER);
        mainPanel.add(actionControlsPanel, BorderLayout.SOUTH);

        setTitle(APP_TITLE + " (" + VERSION_TAG + ")");
        setContentPane(mainPanel);
        setResizable(false);
    }

    private void createInputControlsPanel() {
        inputControlsPanel.setLayout(new BoxLayout(inputControlsPanel, BoxLayout.PAGE_AXIS));

        final Icon questionIcon = UIManager.getIcon("OptionPane.questionIcon");

        JPanel panel;
        JLabel infoLabel;

        panel = new JPanel();
        infoLabel = new JLabel();
        infoLabel.setIcon(questionIcon);
        infoLabel.setToolTipText("Select a .vbo file that do not contain any video related data.");
        inputControlsPanel.add(panel);
        panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder("Source vbo file"));
        panel.add(infoLabel);
        panel.add(sourceVboFilePath);
        panel.add(sourceVboFileChoose);

        panel = new JPanel();
        infoLabel = new JLabel();
        infoLabel.setIcon(questionIcon);
        infoLabel.setToolTipText("Select a video file. Supported video types are .avi and .mp4.");
        inputControlsPanel.add(panel);
        panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder("Source video file"));
        panel.add(infoLabel);
        panel.add(sourceVideoFilePath);
        panel.add(sourceVideoFileChoose);

        panel = new JPanel();
        infoLabel = new JLabel();
        infoLabel.setIcon(questionIcon);
        infoLabel.setToolTipText("Select a folder to place the result files.");
        inputControlsPanel.add(panel);
        panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder("Output directory"));
        panel.add(infoLabel);
        panel.add(outputDirPath);
        panel.add(outputDirChoose);

        panel = new JPanel();
        infoLabel = new JLabel();
        infoLabel.setIcon(questionIcon);
        infoLabel.setToolTipText("Select a name for the session of the .vbo and video data. "
                + "A folder with this name will be created under the output directory and will contain the final .vbo and video files.");
        inputControlsPanel.add(panel);
        panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder("Session name"));
        panel.add(infoLabel);
        panel.add(sessionName);
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

    private void createGpsVideoSynchPanel() {
        gpsVideoSynchPanel.setBorder(BorderFactory.createTitledBorder("GPS and video synchronization"));
        gpsVideoSynchPanel.add(new JLabel("GPS data start "));
        gpsVideoSynchPanel.add(gpsDataOffsetMinutes);
        gpsVideoSynchPanel.add(new JLabel("m"));
        gpsVideoSynchPanel.add(gpsDataOffsetSeconds);
        gpsVideoSynchPanel.add(new JLabel("s"));
        gpsVideoSynchPanel.add(gpsDataOffsetMillis);
        gpsVideoSynchPanel.add(new JLabel("ms"));
        gpsVideoSynchPanel.add(offsetType);
        gpsVideoSynchPanel.add(new JLabel(" video data."));
        gpsVideoSynchPanel.add(resetSynchronizationButton);
        gpsVideoSynchPanel.add(synchronizationDialogButton);
    }

    private void addActionListeners() {
        sourceVboFileChoose.addActionListener(this);
        sourceVideoFileChoose.addActionListener(this);
        outputDirChoose.addActionListener(this);
        vboVideoIntegrate.addActionListener(this);
        clearAll.addActionListener(this);
        about.addActionListener(this);
        resetSynchronizationButton.addActionListener(this);
        synchronizationDialogButton.addActionListener(this);
    }

    private void chooseSourceVbo() {
        final JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter(
                "VBox data files", "vbo"));

        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            sourceVboFilePath.setText(fileChooser.getSelectedFile().getAbsolutePath());

            try {
                appendLog("GPS refresh rate is " + VboEditor.identifyGPSRefreshRate(sourceVboFilePath.getText()) + "HZ");
            } catch (Exception e) {
                // ignore at this stage
            }
        }
    }

    private void chooseSourceVideo() {
        final JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter(
                "Video files", "mp4", "avi"));

        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            sourceVideoFilePath.setText(fileChooser.getSelectedFile().getAbsolutePath());
            try {
                appendLog("Video file type is " + getVideoType());
            } catch (Exception e) {
                // ignore at this stage
            }
        }
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
        final String videoFilePath = sourceVideoFilePath.getText();
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
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                final VideoTelemetrySynchronizer synchronizerDialog = new VideoTelemetrySynchronizer(parentFrame, true);
                synchronizerDialog.pack();
                synchronizerDialog.setLocationRelativeTo(parentFrame);
                synchronizerDialog.loadVideo(sourceVideoFilePath.getText());
                synchronizerDialog.loadTravelledRout(sourceVboFilePath.getText());
                synchronizerDialog.setVisible(true);
            }
        });
    }

    private void integrateGpsAndVideo() {

        try {
            final String outputDir = outputDirPath.getText();
            final String vboFilePath = sourceVboFilePath.getText();
            final String sessionName = this.sessionName.getText();
            final String videoFilePath = sourceVideoFilePath.getText();

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

            final int gpsDataTotalOffsetMillis = getGpsDataTotalOffset();

            VboEditor.createVboWithVideoMetadata(outputDir, vboFilePath, videoType, sessionName, gpsDataTotalOffsetMillis);
            VboEditor.createVideoFile(outputDir, videoFilePath, sessionName);

            appendLog("Vbo and video files created under " + outputDir + "/" + sessionName);
            appendLog("\n");

            JOptionPane.showMessageDialog(this,
                    "Check " + outputDir + "/" + sessionName + " for video and vbo files!", "Done!", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Could not integrate data", JOptionPane.ERROR_MESSAGE);
        }
    }

    private int getGpsDataTotalOffset() {
        // Start with positive number, indicating the GPS data start AFTER video
        int gpsDataTotalOffsetMillis = (int) gpsDataOffsetMillis.getValue() +
                1000 * (int) gpsDataOffsetSeconds.getValue() +
                60 * 1000 * (int) gpsDataOffsetMinutes.getValue();
        if (offsetType.getSelectedIndex() == 1) {
            // GPS data starts BEFORE video
            gpsDataTotalOffsetMillis = -gpsDataTotalOffsetMillis;
        }
        appendLog("Total gps data offset is " + gpsDataTotalOffsetMillis + "ms");
        return gpsDataTotalOffsetMillis;
    }

    private void appendLog(String log) {
        logText.append(log);
        logText.append("\n");
    }

    private void clearAll() {
        resetOffset();
        sourceVideoFilePath.setText("");
        sourceVboFilePath.setText("");
        outputDirPath.setText("");
        sessionName.setText("");
        logText.setText("");
    }

    private void resetOffset() {
        gpsDataOffsetMillis.setValue(0);
        gpsDataOffsetSeconds.setValue(0);
        gpsDataOffsetMinutes.setValue(0);
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

        if (source == sourceVboFileChoose) {
            chooseSourceVbo();
        } else if (source == sourceVideoFileChoose) {
            chooseSourceVideo();
        } else if (source == outputDirChoose) {
            chooseOutputDirectory();
        } else if (source == vboVideoIntegrate) {
            integrateGpsAndVideo();
        } else if (source == resetSynchronizationButton) {
            resetOffset();
        } else if (source == clearAll) {
            clearAll();
        } else if (source == about) {
            showAboutDialog();
        } else if (source == synchronizationDialogButton) {
            showSynchronizationDialog();
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
