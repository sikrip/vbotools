package org.sikrip.vboeditor.gui;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.sikrip.vboeditor.VboEditor;

import com.google.common.base.Strings;

final class VboEditorGUI extends JFrame implements ActionListener {

	private final static String[] OFFSET_TYPES = new String[] { "After", "Before" };

	private final JPanel mainPanel = new JPanel(new BorderLayout());

	private final JPanel inputControlsPanel = new JPanel();

	private final JButton sourceVboFileChoose = new JButton("...");
	private final JTextField sourceVboFilePath = new JTextField();

	private final JButton sourceVideoFileChoose = new JButton("...");
	private final JTextField sourceVideoFilePath = new JTextField();

	private final JButton outputDirChoose = new JButton("...");
	private final JTextField outputDirPath = new JTextField();

	private final JTextField sessionName = new JTextField();

	private final JPanel gpsDataAndVideoPanel = new JPanel();

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

	private final JButton resetOffset = new JButton("(reset)");

	private final JPanel actionControlsPanel = new JPanel();

	private final JTextField infoField = new JTextField("Created by George Sikalias (@sikrip)");
	private final JButton vboVideoIntegrate = new JButton("Integrate GPS and video data");

	private void createGui() {

		inputControlsPanel.setLayout(new BoxLayout(inputControlsPanel, BoxLayout.PAGE_AXIS));

		JPanel panel;

		panel = new JPanel();
		inputControlsPanel.add(panel);
		panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));
		panel.setBorder(BorderFactory.createTitledBorder("Source vbo file"));
		panel.add(sourceVboFilePath);
		panel.add(sourceVboFileChoose);

		panel = new JPanel();
		inputControlsPanel.add(panel);
		panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));
		panel.setBorder(BorderFactory.createTitledBorder("Source video file"));
		panel.add(sourceVideoFilePath);
		panel.add(sourceVideoFileChoose);

		panel = new JPanel();
		inputControlsPanel.add(panel);
		panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));
		panel.setBorder(BorderFactory.createTitledBorder("Output directory"));
		panel.add(outputDirPath);
		panel.add(outputDirChoose);

		panel = new JPanel();
		inputControlsPanel.add(panel);
		panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));
		panel.setBorder(BorderFactory.createTitledBorder("Session name"));
		panel.add(sessionName);

		gpsDataAndVideoPanel.setBorder(BorderFactory.createTitledBorder("GPS and video synchronization"));
		gpsDataAndVideoPanel.add(new JLabel("GPS data start "));
		gpsDataAndVideoPanel.add(gpsDataOffsetMinutes);
		gpsDataAndVideoPanel.add(new JLabel("m"));
		gpsDataAndVideoPanel.add(gpsDataOffsetSeconds);
		gpsDataAndVideoPanel.add(new JLabel("s"));
		gpsDataAndVideoPanel.add(gpsDataOffsetMillis);
		gpsDataAndVideoPanel.add(new JLabel("ms"));
		gpsDataAndVideoPanel.add(offsetType);
		gpsDataAndVideoPanel.add(new JLabel(" video data."));
		gpsDataAndVideoPanel.add(resetOffset);

		actionControlsPanel.setLayout(new BoxLayout(actionControlsPanel, BoxLayout.LINE_AXIS));
		actionControlsPanel.setBorder(BorderFactory.createEtchedBorder());
		actionControlsPanel.add(infoField);
		infoField.setEditable(false);
		actionControlsPanel.add(vboVideoIntegrate);

		mainPanel.setPreferredSize(new Dimension(680, 270));
		mainPanel.add(inputControlsPanel, BorderLayout.NORTH);
		mainPanel.add(actionControlsPanel, BorderLayout.SOUTH);
		mainPanel.add(gpsDataAndVideoPanel, BorderLayout.CENTER);

		setTitle("GPS and Video data integrator");
		setContentPane(mainPanel);
		setResizable(false);
	}

	private void addActionListeners() {
		sourceVboFileChoose.addActionListener(this);
		sourceVideoFileChoose.addActionListener(this);
		outputDirChoose.addActionListener(this);
		vboVideoIntegrate.addActionListener(this);
	}

	private void chooseSourceVbo() {
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setFileFilter(new FileNameExtensionFilter(
				"Vbo GPS data files", "vbo"));

		if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			sourceVboFilePath.setText(fileChooser.getSelectedFile().getAbsolutePath());
		}
	}

	private void chooseSourceVideo() {
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setFileFilter(new FileNameExtensionFilter(
				"Video files", "mp4", "avi"));

		if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			sourceVideoFilePath.setText(fileChooser.getSelectedFile().getAbsolutePath());
		}
	}

	private void chooseOutputDirectory() {
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

		if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			outputDirPath.setText(fileChooser.getSelectedFile().getAbsolutePath());
		}
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

			final String videoExtension;
			try {
				videoExtension = videoFilePath.substring(videoFilePath.lastIndexOf(".")).toLowerCase();
			} catch (StringIndexOutOfBoundsException e) {
				throw new IllegalStateException("Please select a valid video file");
			}
			// Start with positive number, indicating the GPS data start AFTER video
			int gpsDataOffsetMillis = (int) this.gpsDataOffsetMillis.getValue() +
					1000 * (int) gpsDataOffsetSeconds.getValue() +
					60 * 1000 * (int) gpsDataOffsetMinutes.getValue();
			if (offsetType.getSelectedIndex() == 1) {
				// GPS data starts BEFORE video
				gpsDataOffsetMillis = -gpsDataOffsetMillis;
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

			VboEditor.createVboWithVideoMetadata(outputDir, vboFilePath, videoType, sessionName, gpsDataOffsetMillis);
			VboEditor.createVideoFile(outputDir, videoFilePath, sessionName);

			JOptionPane.showMessageDialog(this, "Check " + outputDir + "/" + sessionName+" for video and vbo files!", "Done!", JOptionPane.INFORMATION_MESSAGE);
		} catch (Exception e) {
			JOptionPane.showMessageDialog(this, e.getMessage(), "Could not integrate data", JOptionPane.ERROR_MESSAGE);
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		Object source = e.getSource();

		if (source == sourceVboFileChoose) {
			chooseSourceVbo();
		} else if (source == sourceVideoFileChoose) {
			chooseSourceVideo();
		} else if (source == outputDirChoose) {
			chooseOutputDirectory();
		} else if (source == vboVideoIntegrate) {
			integrateGpsAndVideo();
		}
	}

	public static void main(String[] args) {
		VboEditorGUI editorGui = new VboEditorGUI();
		editorGui.createGui();
		editorGui.addActionListeners();
		editorGui.pack();
		editorGui.setLocationRelativeTo(null);
		editorGui.setVisible(true);
		editorGui.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
	}
}
