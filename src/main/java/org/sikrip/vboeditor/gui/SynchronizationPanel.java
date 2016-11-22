package org.sikrip.vboeditor.gui;


import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

final class SynchronizationPanel extends JPanel implements ActionListener {

    private final VideoPlayer videoPlayer;
    private final TelemetryPlayer telemetryPlayer;
    private final JButton playPauseBoth;
    private boolean playingBoth = false;

    public SynchronizationPanel() {
        setLayout(new BorderLayout());

        videoPlayer = new VideoPlayer();
        telemetryPlayer = new TelemetryPlayer();

        final JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, videoPlayer, telemetryPlayer);
        add(splitPane, BorderLayout.CENTER);

        final JPanel southPanel = new JPanel();
        playPauseBoth = new JButton("Play both");
        playPauseBoth.addActionListener(this);
        southPanel.add(playPauseBoth);
        add(southPanel, BorderLayout.SOUTH);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        final Object source = e.getSource();
        if (source == playPauseBoth) {
            playPauseAll();
        }
    }

    private void playPauseAll() {
        videoPlayer.playPause();
        telemetryPlayer.playPause();
        playingBoth = !playingBoth;

        if (playingBoth) {
            playPauseBoth.setText("Pause both");
            telemetryPlayer.enableControls(false);
            videoPlayer.enableControls(false);
        } else {
            playPauseBoth.setText("Play both");
            telemetryPlayer.enableControls(true);
            videoPlayer.enableControls(true);
        }
    }

    long getOffset() {
        // positive number indicates that gps data start after video
        return videoPlayer.getCurrentTime() - telemetryPlayer.getCurrentTime();
    }

    String getVideoFilePath(){
        return videoPlayer.getFilePath();
    }

    String getTelemetryFilePath(){
        return telemetryPlayer.getFilePath();
    }

    public void clear() {
        throw new UnsupportedOperationException("Implement me!");
    }
}
