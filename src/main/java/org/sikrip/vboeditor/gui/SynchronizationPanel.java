package org.sikrip.vboeditor.gui;


import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;

final class SynchronizationPanel extends JPanel {

    private final Logger LOGGER = LoggerFactory.getLogger(SynchronizationPanel.class);

    private final VideoPlayer videoPlayer;
    private final TelemetryPlayer telemetryPlayer;
    private InvalidationListener telemetryPlayerListener;
    // positive number indicates that gps data start after video
    private long telemetryDataOffset;

    private final VboEditorApplication editor;

    SynchronizationPanel(VboEditorApplication editor) {
        this.editor = editor;
        setLayout(new BorderLayout());

        videoPlayer = new VideoPlayer(this);
        telemetryPlayer = new TelemetryPlayer(this);

        final JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, videoPlayer, telemetryPlayer);
        add(splitPane, BorderLayout.CENTER);

        videoPlayer.setPreferredSize(new Dimension(450, 300));
        telemetryPlayer.setPreferredSize(new Dimension(350, 300));

        telemetryPlayerListener = new InvalidationListener() {
            @Override
            public void invalidated(Observable observable) {
                telemetryPlayer.seekByTime(videoPlayer.getCurrentTime() - telemetryDataOffset);
            }
        };
    }

    void requestPause() {
        videoPlayer.requestPause();
        telemetryPlayer.requestPause();
    }

    void unlock() {
        videoPlayer.removePlayListener(telemetryPlayerListener);
        videoPlayer.enableFileControls(true);
        telemetryPlayer.enableFileControls(true);
        telemetryPlayer.enableControls(true);
    }

    void lock() {
        calculateTelemetryOffset();
        videoPlayer.addPlayListener(telemetryPlayerListener);
        telemetryPlayer.enableControls(false);
        videoPlayer.enableFileControls(false);
        telemetryPlayer.enableFileControls(false);
    }

    void stepTelemetry(long amount) {
        if (editor.isDataLocked()) {
            telemetryPlayer.seekByTime(videoPlayer.getCurrentTime() - telemetryDataOffset + amount);
        }
    }

    void seekTelemetryByTime(long time) {
        if (editor.isDataLocked()) {
            telemetryPlayer.seekByTime(time - telemetryDataOffset);
        }
    }

    void checkCanLock() {
        editor.enableDataLock(videoPlayer.isLoaded() && telemetryPlayer.isLoaded());
    }

    boolean isDataLocked(){
        return editor.isDataLocked();
    }

    long getTelemetryDataOffset() {
        return telemetryDataOffset;
    }

    String getVideoFilePath() {
        return videoPlayer.getFilePath();
    }

    String getTelemetryFilePath() {
        return telemetryPlayer.getFilePath();
    }

    private void calculateTelemetryOffset() {
        telemetryDataOffset = videoPlayer.getCurrentTime() - telemetryPlayer.getCurrentTime();
        LOGGER.debug("Video time is {}", videoPlayer.getCurrentTime());
        LOGGER.debug("Telemetry time is {}", telemetryPlayer.getCurrentTime());
        LOGGER.debug("Offset is {}", telemetryDataOffset);
    }
}
