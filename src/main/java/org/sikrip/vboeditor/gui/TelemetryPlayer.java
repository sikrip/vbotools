package org.sikrip.vboeditor.gui;


import org.sikrip.vboeditor.VboEditor;
import org.sikrip.vboeditor.model.TraveledRouteCoordinate;
import org.sikrip.vboeditor.model.TraveledRoutePoint;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

final class TelemetryPlayer extends JPanel implements ActionListener, ChangeListener {

    private static final int MINIMUM_IMAGE_PADDING_IN_PX = 50;
    public static final int CURRENT_POSITION_MARKER_SIZE = 8;

    private final JButton fileChoose = new JButton("...");
    private final JTextField filePath = new JTextField(/*"/home/sikripefg/sample-vbo-from-dbn.vbo"*/);

    private final TraveledRoutePanel traveledRoutePanel;

    private final JPanel controlsPanel = new JPanel(new BorderLayout());
    private final JButton prev2 = new JButton("<<");
    private final JButton prev = new JButton("<");
    private final JButton next = new JButton(">");
    private final JButton next2 = new JButton(">>");
    private final JButton playPause = new JButton("Play");
    private final JButton reset = new JButton("Reset");
    private final JSlider seekSlider = new JSlider();

    private final AtomicBoolean playFlag = new AtomicBoolean(false);
    private long gpsDataIntervalMillis;
    private int currentPositionIdx = 0;
    private final List<TraveledRoutePoint> traveledRoutePoints = new ArrayList<>();
    private double startTime;

    public TelemetryPlayer() {

        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Telemetry"));

        add(createFileInputPanel(), BorderLayout.NORTH);

        traveledRoutePanel = new TraveledRoutePanel();
        add(traveledRoutePanel, BorderLayout.CENTER);

        createControlsPanel();
        add(controlsPanel, BorderLayout.SOUTH);

        enableControls(false);
    }

    private JPanel createFileInputPanel() {
        final JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));
        panel.add(filePath);
        panel.add(fileChoose);
        fileChoose.setToolTipText("Select a .vbo file that do not contain any video related data.");
        fileChoose.addActionListener(this);
        return panel;
    }

    private void createControlsPanel() {

        final JPanel panel = new JPanel();

        panel.add(prev2);
        panel.add(prev);
        panel.add(playPause);
        panel.add(next);
        panel.add(next2);
        panel.add(reset);

        controlsPanel.add(panel, BorderLayout.CENTER);

        seekSlider.setValue(0);
        controlsPanel.add(seekSlider, BorderLayout.NORTH);

        prev2.addActionListener(this);
        prev.addActionListener(this);
        playPause.addActionListener(this);
        reset.addActionListener(this);
        next.addActionListener(this);
        next2.addActionListener(this);
        seekSlider.addChangeListener(this);
    }

    private void paintTraveledRoute() {
        try {
            calculateTraveledRoute(VboEditor.getTraveledRoute(filePath.getText()));
            traveledRoutePanel.repaint();
        } catch (Exception e) {
            throw new RuntimeException("Cannot draw traveled route", e);
        }
    }

    private void loadTelemetry() {
        final JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter(
                "VBox data files", "vbo"));

        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            filePath.setText(fileChooser.getSelectedFile().getAbsolutePath());

            try {
                //appendLog("GPS refresh rate is " + VboEditor.identifyGPSRefreshRate(sourceVboFilePath.getText()) + "HZ");
            } catch (Exception e) {
                // ignore at this stage
            }
            paintTraveledRoute();
            setupSlider();
            enableControls(true);
        }
    }

    private void setupSlider() {
        seekSlider.setMinimum(0);
        seekSlider.setMaximum(traveledRoutePoints.size() - 1);
        seekSlider.setValue(0);
        Hashtable<Integer, JComponent> labelTable = new Hashtable<>();
        labelTable.put(0, new JLabel("0"));
        labelTable.put(seekSlider.getMaximum(), new JLabel(getTime(traveledRoutePoints.size() - 1)));
        seekSlider.setLabelTable(labelTable);
        seekSlider.setPaintLabels(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        final Object source = e.getSource();

        if (source == prev2) {
            seek(-2);
        } else if (source == prev) {
            seek(-1);
        } else if (source == playPause) {
            playPause();
        } else if (source == reset) {
            reset();
        } else if (source == next) {
            seek(1);
        } else if (source == next2) {
            seek(2);
        } else if (source == fileChoose) {
            loadTelemetry();
        }
    }

    private void reset() {
        playFlag.set(false);
        currentPositionIdx = 0;
        playPause.setText("Play");
        seekSlider.setValue(currentPositionIdx);
        traveledRoutePanel.repaint();
        enableScanControls(true);
    }

    private void enableScanControls(boolean enable) {
        prev.setEnabled(enable);
        prev2.setEnabled(enable);
        next.setEnabled(enable);
        next2.setEnabled(enable);
        seekSlider.setEnabled(enable);
    }

    private void enableControls(boolean enable) {
        enableScanControls(enable);
        reset.setEnabled(enable);
        playPause.setEnabled(enable);
    }

    void showControls(boolean show) {
        controlsPanel.setVisible(show);
    }

    long getCurrentTime() {
        return currentPositionIdx * gpsDataIntervalMillis;
    }

    String getFilePath() {
        return filePath.getText();
    }

    void playPause() {
        if (playFlag.get()) {
            pause();
        } else {
            play();
        }
    }

    private void play() {
        playPause.setText("Pause");
        enableScanControls(false);
        playFlag.set(true);
        final Thread playThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (currentPositionIdx < traveledRoutePoints.size()) {
                    if (playFlag.get()) {
                        seek(1);
                        try {
                            Thread.sleep(gpsDataIntervalMillis);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    } else {
                        break;
                    }
                }
            }
        });
        playThread.start();
    }

    void pause() {
        playPause.setText("Play");
        enableScanControls(true);
        playFlag.set(false);
    }

    void seek(int amount) {
        currentPositionIdx += amount;
        if (currentPositionIdx < 0) {
            currentPositionIdx = 0;
        } else if (currentPositionIdx >= traveledRoutePoints.size()) {
            currentPositionIdx = traveledRoutePoints.size() - 1;
        }
        seekSlider.setValue(currentPositionIdx);
        traveledRoutePanel.repaint();
    }

    private void goTo(int position) {
        currentPositionIdx = position;
        if (currentPositionIdx < 0) {
            currentPositionIdx = 0;
        } else if (currentPositionIdx >= traveledRoutePoints.size()) {
            currentPositionIdx = traveledRoutePoints.size() - 1;
        }
        traveledRoutePanel.repaint();
    }

    private void calculateTraveledRoute(final List<TraveledRouteCoordinate> traveledRouteCoordinates) {
        if (traveledRouteCoordinates.isEmpty()) {
            throw new RuntimeException("Cannot read travelled route");
        }

        final Dimension traveledRouteComponentSize = traveledRoutePanel.getSize();
        gpsDataIntervalMillis = traveledRouteCoordinates.get(0).getGpsDataInterval();

        traveledRoutePoints.clear();

        final double actualWidth = traveledRouteComponentSize.getWidth();
        final double actualHeight = traveledRouteComponentSize.getHeight();

        // min and max coordinates, used in the computation below
        Point2D.Double minXY = new Point2D.Double(-1, -1);
        Point2D.Double maxXY = new Point2D.Double(-1, -1);

        List<Point2D.Double> routePoints = new ArrayList<>();

        for (TraveledRouteCoordinate coordinate : traveledRouteCoordinates) {

            // convert to radian
            double longitude = coordinate.getLongitude() * Math.PI / 180;
            double latitude = coordinate.getLatitude() * Math.PI / 180;


            // Actually this is not 100% acurate as it does not take into account the earth curvature
            // but for the area of a track we assume that the earth is flat
            Point2D.Double xy = new Point2D.Double(longitude, latitude);

            // The reason we need to determine the min X and Y values is because in order to draw the map,
            // we need to offset the position so that there will be no negative X and Y values
            minXY.x = (minXY.x == -1) ? xy.x : Math.min(minXY.x, xy.x);
            minXY.y = (minXY.y == -1) ? xy.y : Math.min(minXY.y, xy.y);

            routePoints.add(xy);
        }

        for (Point2D.Double point : routePoints) {
            point.x = point.x - minXY.x;
            point.y = point.y - minXY.y;

            // now, we need to keep track the max X and Y values
            maxXY.x = (maxXY.x == -1) ? point.x : Math.max(maxXY.x, point.x);
            maxXY.y = (maxXY.y == -1) ? point.y : Math.max(maxXY.y, point.y);
        }

        double paddingBothSides = MINIMUM_IMAGE_PADDING_IN_PX * 2;

        // the actual drawing space for the map on the image
        double mapWidth = actualWidth - paddingBothSides;
        double mapHeight = actualHeight - paddingBothSides;

        // determine the width and height ratio because we need to magnify the map to fit into the given image dimension
        double mapWidthRatio = mapWidth / maxXY.x;
        double mapHeightRatio = mapHeight / maxXY.y;

        // using different ratios for width and height will cause the map to be stretched. So, we have to determine
        // the global ratio that will perfectly fit into the given image dimension
        double globalRatio = Math.min(mapWidthRatio, mapHeightRatio);

        // now we need to readjust the padding to ensure the map is always drawn on the center of the given image dimension
        double heightPadding = (actualHeight - (globalRatio * maxXY.y)) / 2;
        double widthPadding = (actualWidth - (globalRatio * maxXY.x)) / 2;

        for (int i = 0; i < routePoints.size(); i++) {
            final TraveledRouteCoordinate coordinate = traveledRouteCoordinates.get(i);
            final Point2D.Double point = routePoints.get(i);

            // actual XY without transformation
            //int adjustedX = (int) (widthPadding + (point.getX() * globalRatio));
            //int adjustedY = (int) (heightPadding + (point.getY() * globalRatio));

            // need to invert the XY since 0,0 starts at top left
            int adjustedX = (int) (actualWidth - widthPadding - (point.getX() * globalRatio));
            int adjustedY = (int) (actualHeight - heightPadding - (point.getY() * globalRatio));

            traveledRoutePoints.add(new TraveledRoutePoint(adjustedX, adjustedY, coordinate.getTime(), coordinate.getSpeed()));
        }
        startTime = traveledRouteCoordinates.get(0).getTime();
    }

    private String getTime(int position) {
        final long timeMillis = (long) (traveledRoutePoints.get(position).getTime() * 1000 - startTime * 1000);

        long minutes = TimeUnit.MILLISECONDS.toMinutes(timeMillis);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(timeMillis) - TimeUnit.MINUTES.toSeconds(minutes);
        long millis = timeMillis - TimeUnit.SECONDS.toMillis(seconds) - TimeUnit.MINUTES.toMillis(minutes);
        return String.format("%02d:%02d.%03d", minutes, seconds, millis);
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        goTo(seekSlider.getValue());
    }

    private class TraveledRoutePanel extends JPanel {

        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);

            if (!traveledRoutePoints.isEmpty()) {
                final Polygon traveledRoute = new Polygon();

                for (TraveledRoutePoint traveledRoutePoint : traveledRoutePoints) {
                    traveledRoute.addPoint(traveledRoutePoint.getX(), traveledRoutePoint.getY());
                }
                g.drawPolygon(traveledRoute);
                g.setColor(Color.red);

                TraveledRoutePoint currentPosition = traveledRoutePoints.get(currentPositionIdx);
                g.fillOval(currentPosition.getX() - CURRENT_POSITION_MARKER_SIZE / 2,
                        currentPosition.getY() - CURRENT_POSITION_MARKER_SIZE / 2,
                        CURRENT_POSITION_MARKER_SIZE, CURRENT_POSITION_MARKER_SIZE);

                g.setColor(Color.BLUE);
                g.drawString(getSpeed(), 5, 25);
                g.drawString("Time: " + getTime(currentPositionIdx), 5, 50);
            }
        }

        private String getSpeed() {
            return "Speed: " + traveledRoutePoints.get(currentPositionIdx).getSpeed();
        }
    }

}
