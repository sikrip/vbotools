package org.sikrip.vboeditor.gui;

import javafx.embed.swing.JFXPanel;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.util.Duration;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.MalformedURLException;


final class VideoPlayer implements ActionListener {

    private final int width;
    private final int height;

    final JPanel videoPlayerPanel;
    private final JFXPanel videoPanel;
    private MediaPlayer mediaPlayer;
    private final JButton playPause;
    private final JButton stop;

    private final JButton fw50;
    private final JButton fw100;
    private final JButton bw50;
    private final JButton bw100;

    VideoPlayer(int width, int height) {
        this.width = width;
        this.height = height;

        // inits Java FX toolkit
        videoPanel = new JFXPanel();

        videoPlayerPanel = new JPanel(new BorderLayout());
        videoPlayerPanel.add(videoPanel, BorderLayout.CENTER);

        final JPanel buttonsPanel = new JPanel();
        videoPlayerPanel.add(buttonsPanel, BorderLayout.SOUTH);

        playPause = new JButton("Play");
        playPause.addActionListener(this);
        buttonsPanel.add(playPause);

        stop = new JButton("Stop");
        stop.addActionListener(this);
        buttonsPanel.add(stop);

        bw100 = new JButton("-100ms");
        bw100.addActionListener(this);
        buttonsPanel.add(bw100);

        bw50 = new JButton("-50ms");
        bw50.addActionListener(this);
        buttonsPanel.add(bw50);

        fw50 = new JButton("+50ms");
        fw50.addActionListener(this);
        buttonsPanel.add(fw50);

        fw100 = new JButton("+100ms");
        fw100.addActionListener(this);
        buttonsPanel.add(fw100);

        enableSeekControls(true);
    }

    JPanel getPanel() {
        return videoPlayerPanel;
    }

    void loadVideo(String filePath) {
        try {
            final File videoFile = new File(filePath);
            final Media media = new Media(videoFile.toURI().toURL().toString());
            mediaPlayer = new MediaPlayer(media);
            MediaView mediaView = new MediaView(mediaPlayer);
            final Scene scene = new Scene(new Group(mediaView), width, height);
            videoPanel.setScene(scene);

            mediaView.setFitWidth(scene.getWidth());
            mediaView.setFitHeight(scene.getHeight());

        } catch (MalformedURLException e) {
            // TODO
            e.printStackTrace();
        }
    }

    void playPause() {
        final MediaPlayer.Status status = mediaPlayer.getStatus();
        if (MediaPlayer.Status.PLAYING.equals(status)) {
            mediaPlayer.pause();
            playPause.setText("Play");
            enableSeekControls(true);
        } else {
            mediaPlayer.play();
            playPause.setText("Pause");
            enableSeekControls(false);
        }
    }

    void dispose(){
        mediaPlayer.dispose();
    }

    private void stop(){
        mediaPlayer.stop();
        playPause.setText("Play");
        enableSeekControls(true);
    }

    private void enableSeekControls(boolean enable) {
        fw50.setEnabled(enable);
        fw100.setEnabled(enable);
        bw50.setEnabled(enable);
        bw100.setEnabled(enable);
    }

    private void seek(double durationMillis) {
        if (durationMillis < 0) {
            mediaPlayer.seek(mediaPlayer.getCurrentTime().subtract(new Duration(Math.abs(durationMillis))));
        } else {
            mediaPlayer.seek(mediaPlayer.getCurrentTime().add(new Duration(durationMillis)));
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        final Object source = e.getSource();

        if (source == playPause) {
            playPause();
        } else if (source == stop) {
            stop();
        } else if (source == fw50) {
            seek(50);
        } else if (source == fw100) {
            seek(100);
        } else if (source == bw50) {
            seek(-50);
        } else if (source == bw100) {
            seek(-100);
        }
    }
}
