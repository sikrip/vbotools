package org.sikrip.vboeditor.gui;


import javafx.beans.value.ObservableValue;
import javafx.util.Duration;

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

    private final VboEditorApplication application;

    private javafx.beans.value.ChangeListener<Duration> telemetryPlayerListener;

    // positive number indicates that gps data start after video
    private long telemetryDataOffset;

    SynchronizationPanel(VboEditorApplication application) {
        this.application = application;
        setLayout(new BorderLayout());

        videoPlayer = new VideoPlayer(this);
        telemetryPlayer = new TelemetryPlayer(this);

        final JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, videoPlayer, telemetryPlayer);
        add(splitPane, BorderLayout.CENTER);

        final JPanel southPanel = new JPanel();
        southPanel.setLayout(new BoxLayout(southPanel, BoxLayout.X_AXIS));
        southPanel.add(syncLock);
        syncLock.setEnabled(false);

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

        videoPlayer.setPreferredSize(new Dimension(450, 300));
        telemetryPlayer.setPreferredSize(new Dimension(350, 300));

        telemetryPlayerListener = new javafx.beans.value.ChangeListener<Duration>() {
            @Override
            public void changed(ObservableValue<? extends Duration> observable, Duration oldValue, Duration newValue) {
                telemetryPlayer.seekByTime((long) newValue.toMillis() - telemetryDataOffset);
            }
        };
    }

    private void playPause() {
        videoPlayer.playPause();
        playing = !playing;

        playPauseAll.setText(playing ? "Pause" : "Play");

        prev2.setEnabled(!playing);
        prev.setEnabled(!playing);
        next.setEnabled(!playing);
        next2.setEnabled(!playing);
    }

    private void toggleLock() {
        if (syncLock.isSelected()) {
            lock();
            application.appendLog(String.format("Offset is %sms", getTelemetryDataOffset()));
        } else {
            unlock();
        }
        application.enableIntegrationAction(syncLock.isSelected());
        controlsPanel.setVisible(syncLock.isSelected());
    }

    private void unlock() {
        forcePause();
        videoPlayer.removeTelemetryListener(telemetryPlayerListener);
        telemetryPlayer.showControls(true);
        videoPlayer.showControls(true);
    }

    private void lock() {
        calculateTelemetryOffset();
        videoPlayer.addTelemetryListener(telemetryPlayerListener);
        telemetryPlayer.showControls(false);
        telemetryPlayer.pause();
        videoPlayer.showControls(false);
        videoPlayer.pause();
    }

    void checkCanLock(){
        syncLock.setEnabled(videoPlayer.isLoaded() && telemetryPlayer.isLoaded());
    }

    void forcePause() {
        if (playing) {
            playPause();
        }
    }

    long getTelemetryDataOffset() {
        return telemetryDataOffset;
    }

    private void calculateTelemetryOffset(){
        telemetryDataOffset = videoPlayer.getCurrentTime() - telemetryPlayer.getCurrentTime();
    }

    String getVideoFilePath() {
        return videoPlayer.getFilePath();
    }

    String getTelemetryFilePath() {
        return telemetryPlayer.getFilePath();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        final Object source = e.getSource();
        if (source == syncLock) {
            toggleLock();
        } else if (source == playPauseAll) {
            playPause();
        } else if (source == prev2) {
            videoPlayer.step(-100);
            telemetryPlayer.step(-2);
        } else if (source == prev) {
            videoPlayer.step(-50);
            telemetryPlayer.step(-1);
        } else if (source == next) {
            videoPlayer.step(50);
            telemetryPlayer.step(1);
        } else if (source == next2) {
            videoPlayer.step(100);
            telemetryPlayer.step(2);
        }
    }
}
