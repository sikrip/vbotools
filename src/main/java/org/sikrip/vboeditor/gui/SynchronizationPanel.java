package org.sikrip.vboeditor.gui;


import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

final class SynchronizationPanel extends JPanel implements ActionListener {

    private final VideoPlayer videoPlayer;
    private final TelemetryPlayer telemetryPlayer;

    private final JPanel controlsPanel;
    private final JButton playPauseAll = new JButton("Play");
    private final JButton prev2 = new JButton("<<");
    private final JButton prev = new JButton("<");
    private final JButton next = new JButton(">");
    private final JButton next2 = new JButton(">>");

    private final JCheckBox syncLock = new JCheckBox("Lock video/telemetry data");
    private boolean playing = false;

    private final VboEditorGUI editorGUI;

    public SynchronizationPanel(VboEditorGUI editorGUI) {
        this.editorGUI = editorGUI;
        setLayout(new BorderLayout());

        videoPlayer = new VideoPlayer();
        telemetryPlayer = new TelemetryPlayer();

        final JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, videoPlayer, telemetryPlayer);
        add(splitPane, BorderLayout.CENTER);

        final JPanel southPanel = new JPanel();
        southPanel.setLayout(new BoxLayout(southPanel, BoxLayout.X_AXIS));
        southPanel.add(syncLock);

        controlsPanel = new JPanel();
        controlsPanel.add(prev2);
        controlsPanel.add(prev);
        controlsPanel.add(playPauseAll);
        controlsPanel.add(next);
        controlsPanel.add(next2);
        controlsPanel.setVisible(false);

        southPanel.add(controlsPanel);

        add(southPanel, BorderLayout.SOUTH);

        syncLock.addActionListener(this);
        prev2.addActionListener(this);
        prev.addActionListener(this);
        next.addActionListener(this);
        next2.addActionListener(this);
        playPauseAll.addActionListener(this);
    }

    private void playPause() {
        videoPlayer.playPause();
        telemetryPlayer.playPause();
        playing = !playing;

        playPauseAll.setText(playing ? "Pause" : "Play");

        prev2.setEnabled(!playing);
        prev.setEnabled(!playing);
        next.setEnabled(!playing);
        next2.setEnabled(!playing);
    }

    private void toggleLock() {
        if (syncLock.isSelected()) {
            // unlock
            telemetryPlayer.showControls(false);
            videoPlayer.showControls(false);
        } else {
            // lock
            forcePause();
            telemetryPlayer.showControls(true);
            videoPlayer.showControls(true);
        }
        editorGUI.enableIntegrationAction(syncLock.isSelected());
        controlsPanel.setVisible(syncLock.isSelected());
    }

    void forcePause() {
        if (playing) {
            playPause();
        }
    }

    long getOffset() {
        // positive number indicates that gps data start after video
        return videoPlayer.getCurrentTime() - telemetryPlayer.getCurrentTime();
    }

    String getVideoFilePath() {
        return videoPlayer.getFilePath();
    }

    String getTelemetryFilePath() {
        return telemetryPlayer.getFilePath();
    }

    void clear() {
        throw new UnsupportedOperationException("Implement me!");
    }


    @Override
    public void actionPerformed(ActionEvent e) {
        final Object source = e.getSource();
        if (source == syncLock) {
            toggleLock();
        } else if (source == playPauseAll) {
            playPause();
        } else if (source == prev2) {
            videoPlayer.seek(-100);
            telemetryPlayer.seek(-2);
        } else if (source == prev) {
            videoPlayer.seek(-50);
            telemetryPlayer.seek(-1);
        } else if (source == next) {
            videoPlayer.seek(50);
            telemetryPlayer.seek(1);
        } else if (source == next2) {
            videoPlayer.seek(100);
            telemetryPlayer.seek(2);
        }
    }
}
