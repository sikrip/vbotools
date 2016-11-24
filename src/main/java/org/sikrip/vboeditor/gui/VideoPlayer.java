package org.sikrip.vboeditor.gui;

import javafx.embed.swing.JFXPanel;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.util.Duration;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.MalformedURLException;


final class VideoPlayer extends JPanel implements ActionListener {

    private final JButton fileChoose = new JButton("...");
    private final JTextField filePath = new JTextField(/*"/home/sikripefg/provlima-sasman.MP4"*/);

    private final JFXPanel videoPanel;
    private final JPanel controlsPanel = new JPanel();

    private MediaPlayer mediaPlayer;
    private final JButton playPause;
    private final JButton reset;

    private final JButton fw50;
    private final JButton fw100;
    private final JButton bw50;
    private final JButton bw100;

    VideoPlayer() {

        setBorder(BorderFactory.createTitledBorder("Video"));

        // inits Java FX toolkit
        videoPanel = new JFXPanel();

        setLayout(new BorderLayout());
        add(videoPanel, BorderLayout.CENTER);

        final JPanel northPanel = new JPanel();
        northPanel.setLayout(new BoxLayout(northPanel, BoxLayout.LINE_AXIS));
        northPanel.add(filePath);
        northPanel.add(fileChoose);
        fileChoose.setToolTipText("Select a video file. Supported video types are .avi and .mp4.");
        fileChoose.addActionListener(this);
        add(northPanel, BorderLayout.NORTH);


        add(controlsPanel, BorderLayout.SOUTH);
        bw100 = new JButton("<<");
        bw100.addActionListener(this);
        controlsPanel.add(bw100);

        bw50 = new JButton("<");
        bw50.addActionListener(this);
        controlsPanel.add(bw50);

        playPause = new JButton("Play");
        playPause.addActionListener(this);
        controlsPanel.add(playPause);

        fw50 = new JButton(">");
        fw50.addActionListener(this);
        controlsPanel.add(fw50);

        fw100 = new JButton(">>");
        fw100.addActionListener(this);
        controlsPanel.add(fw100);

        reset = new JButton("Reset");
        reset.addActionListener(this);
        controlsPanel.add(reset);

        enableControls(false);
    }

    private void loadVideo() {
        try {

            Dimension size = getSize();
            final File videoFile = new File(filePath.getText());
            final Media media = new Media(videoFile.toURI().toURL().toString());
            mediaPlayer = new MediaPlayer(media);
            MediaView mediaView = new MediaView(mediaPlayer);
            final Scene scene = new Scene(new Group(mediaView), size.getWidth(), size.getHeight());
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
            pause();
        } else {
            play();
        }
    }

    private void play() {
        mediaPlayer.play();
        playPause.setText("Pause");
        enableSeekControls(false);
    }

    void pause() {
        if (mediaPlayer != null) {
            mediaPlayer.pause();
            playPause.setText("Play");
            enableSeekControls(true);
        }
    }

    private void reset() {
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

    private void enableControls(boolean enable) {
        enableSeekControls(enable);
        reset.setEnabled(enable);
        playPause.setEnabled(enable);
    }

    void showControls(boolean show) {
        controlsPanel.setVisible(show);
    }

    long getCurrentTime() {
        return (long) mediaPlayer.getCurrentTime().toMillis();
    }

    String getFilePath() {
        return filePath.getText();
    }

    void seek(double durationMillis) {
        if (durationMillis < 0) {
            mediaPlayer.seek(mediaPlayer.getCurrentTime().subtract(new Duration(Math.abs(durationMillis))));
        } else {
            mediaPlayer.seek(mediaPlayer.getCurrentTime().add(new Duration(durationMillis)));
        }
    }

    private void chooseSourceVideo() {
        final JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter(
                "Video files", "mp4", "avi"));

        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            filePath.setText(fileChooser.getSelectedFile().getAbsolutePath());
            try {
                //appendLog("Video file type is " + getVideoType());
            } catch (Exception e) {
                // ignore at this stage
            }
            loadVideo();
            enableControls(true);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        final Object source = e.getSource();

        if (source == playPause) {
            playPause();
        } else if (source == reset) {
            reset();
        } else if (source == fw50) {
            seek(50);
        } else if (source == fw100) {
            seek(100);
        } else if (source == bw50) {
            seek(-50);
        } else if (source == bw100) {
            seek(-100);
        } else if (source == fileChoose) {
            chooseSourceVideo();
        }
    }
}
