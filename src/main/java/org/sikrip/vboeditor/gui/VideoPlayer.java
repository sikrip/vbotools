package org.sikrip.vboeditor.gui;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.util.Duration;
import org.sikrip.vboeditor.helper.TimeHelper;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.MalformedURLException;
import java.util.Hashtable;


final class VideoPlayer extends JPanel implements ActionListener, ChangeListener {

    private final JButton fileChoose = new JButton("...");
    private final JTextField filePath = new JTextField();

    private final SynchronizationPanel synchronizationPanel;
    private MediaPlayer mediaPlayer;
    private final JFXPanel videoPanel;

    private final JPanel controlsPanel = new JPanel(new BorderLayout());
    private final JLabel timeLabel = new JLabel();
    private final JSlider seekSlider = new JSlider();
    private final JButton playPause = new JButton("Play");
    private final JButton reset = new JButton("Reset");
    private final JButton prev2 = new JButton("<<");
    private final JButton prev = new JButton("<");
    private final JButton next = new JButton(">");
    private final JButton next2 = new JButton(">>");

    VideoPlayer(SynchronizationPanel synchronizationPanel) {
        this.synchronizationPanel = synchronizationPanel;
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

        createControlsPanel();
        add(controlsPanel, BorderLayout.SOUTH);
        enableControls(false);
    }

    private void createControlsPanel() {

        final JPanel panel = new JPanel();

        prev2.addActionListener(this);
        panel.add(prev2);
        prev.addActionListener(this);
        panel.add(prev);
        playPause.addActionListener(this);
        panel.add(playPause);
        next.addActionListener(this);
        panel.add(next);
        next2.addActionListener(this);
        panel.add(next2);
        reset.addActionListener(this);
        panel.add(reset);

        seekSlider.setValue(0);
        seekSlider.addChangeListener(this);
        controlsPanel.add(timeLabel, BorderLayout.NORTH);
        controlsPanel.add(seekSlider, BorderLayout.CENTER);
        controlsPanel.add(panel, BorderLayout.SOUTH);
    }

    private void loadVideoPanel() {
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

            mediaPlayer.currentTimeProperty().addListener(new InvalidationListener() {
                @Override
                public void invalidated(Observable observable) {
                    timeLabel.setText("Time: " + TimeHelper.getTimeString((long) mediaPlayer.getCurrentTime().toMillis()));
                }
            });
            timeLabel.setText("Time: " + TimeHelper.getTimeString(0));
        } catch (MalformedURLException e) {
            throw new RuntimeException("Cannot load video", e);
        }
    }

    private void setupSlider() {

        final Duration duration = mediaPlayer.getMedia().getDuration();

        seekSlider.setMinimum(0);
        seekSlider.setMaximum((int) duration.toSeconds());
        seekSlider.setValue(0);
        Hashtable<Integer, JComponent> labelTable = new Hashtable<>();
        labelTable.put(0, new JLabel("0"));

        labelTable.put(seekSlider.getMaximum(), new JLabel(TimeHelper.getTimeString((long) duration.toMillis())));
        seekSlider.setLabelTable(labelTable);
        seekSlider.setPaintLabels(true);
    }

    private void play() {
        mediaPlayer.play();
        playPause.setText("Pause");
        seekSlider.setVisible(false);
        enableFileControls(false);
        enableSeekControls(false);
    }

    private void enableFileControls(boolean b) {
        fileChoose.setEnabled(b);
        filePath.setEnabled(b);
    }

    private void reset() {
        mediaPlayer.stop();
        playPause.setText("Play");
        seekSlider.setValue(0);
        enableSeekControls(true);
    }

    private void enableSeekControls(boolean enable) {
        next.setEnabled(enable);
        next2.setEnabled(enable);
        prev.setEnabled(enable);
        prev2.setEnabled(enable);
        seekSlider.setEnabled(enable);
        reset.setEnabled(enable);
    }

    private void enableControls(boolean enable) {
        enableSeekControls(enable);
        reset.setEnabled(enable);
        playPause.setEnabled(enable);
    }

    private void loadVideo() {
        final JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter(
                "Video files", "mp4", "avi"));

        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            filePath.setText(fileChooser.getSelectedFile().getAbsolutePath());
            loadVideoPanel();
            setupSlider();

            enableControls(true);
            synchronizationPanel.checkCanLock();
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

    void pause() {
        if (mediaPlayer != null) {
            mediaPlayer.pause();
            playPause.setText("Play");
            seekSlider.setVisible(true);
            seekSlider.setValue((int) mediaPlayer.getCurrentTime().toSeconds());
            enableFileControls(true);
            enableSeekControls(true);
        }
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

    void step(double durationMillis) {
        if (durationMillis < 0) {
            mediaPlayer.seek(mediaPlayer.getCurrentTime().subtract(new Duration(Math.abs(durationMillis))));
        } else {
            mediaPlayer.seek(mediaPlayer.getCurrentTime().add(new Duration(durationMillis)));
        }
    }

    boolean isLoaded() {
        return !filePath.getText().isEmpty();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        final Object source = e.getSource();

        if (source == playPause) {
            playPause();
        } else if (source == reset) {
            reset();
        } else if (source == next) {
            step(50);
        } else if (source == next2) {
            step(100);
        } else if (source == prev) {
            step(-50);
        } else if (source == prev2) {
            step(-100);
        } else if (source == fileChoose) {
            loadVideo();
        }
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        if (mediaPlayer != null) {
            mediaPlayer.seek(new Duration(seekSlider.getValue() * 1000));
        }
    }
}
