package org.sikrip.vboeditor.gui;


import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

final class VideoTelemetrySynchronizer extends JDialog implements ActionListener {

    private final VideoPlayer videoPlayer;
    private final GPSViewer gpsViewer;
    private final JButton playPauseBoth;
    private final JButton okButton;
    private final JButton cancelButton;
    private boolean playingBoth = false;
    private Double offset;

    public VideoTelemetrySynchronizer(Frame owner, boolean modal) {
        super(owner, modal);

        setTitle("Video / Telemetry Synchronization");

        videoPlayer = new VideoPlayer(450, 250);

        gpsViewer = new GPSViewer(450, 250);

        final JPanel buttonPanel = new JPanel();
        playPauseBoth = new JButton("Play both");
        playPauseBoth.addActionListener(this);

        okButton = new JButton("OK, video and telemetry is synchronized");
        okButton.addActionListener(this);

        cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(this);

        buttonPanel.add(playPauseBoth);
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);

        final JPanel mainPanel = new JPanel(new BorderLayout());

        mainPanel.add(videoPlayer.getPanel(), BorderLayout.WEST);
        mainPanel.add(gpsViewer, BorderLayout.EAST);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        setContentPane(mainPanel);

        //Handle window closing correctly.
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
                cancelSynchronization();
            }
        });
        setResizable(false);
        setPreferredSize(new Dimension(900, 300));
    }

    void loadVideo(String videoFilePath) {
        videoPlayer.loadVideo(videoFilePath);
    }

    void loadTravelledRout(String vboFilePath) {
        gpsViewer.loadTraveledRoute(vboFilePath);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        final Object source = e.getSource();
        if (source == okButton) {
            acceptSynchronization();
        } else if (source == cancelButton) {
            cancelSynchronization();
        } else if (source == playPauseBoth) {
            playPauseAll();
        }
    }

    private void playPauseAll() {
        videoPlayer.playPause();
        gpsViewer.playPause();
        playingBoth = !playingBoth;

        if (playingBoth) {
            playPauseBoth.setText("Pause both");
            gpsViewer.enableControls(false);
            videoPlayer.enableControls(false);
            okButton.setEnabled(false);
            cancelButton.setEnabled(false);
        } else {
            playPauseBoth.setText("Play both");
            gpsViewer.enableControls(true);
            videoPlayer.enableControls(true);
            okButton.setEnabled(true);
            cancelButton.setEnabled(true);
        }
    }

    private void cancelSynchronization() {
        offset = null;
        closeWindow();
    }

    private void acceptSynchronization() {
        offset = gpsViewer.getCurrentTime() - videoPlayer.getCurrentTime();
        closeWindow();
    }

    private void closeWindow() {
        dispose();
        videoPlayer.dispose();
    }
}
