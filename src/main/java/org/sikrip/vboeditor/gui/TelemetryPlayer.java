package org.sikrip.vboeditor.gui;


import org.sikrip.vboeditor.VboEditor;
import org.sikrip.vboeditor.model.TraveledRouteCoordinate;
import org.sikrip.vboeditor.model.TraveledRoutePoint;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

final class TelemetryPlayer extends JPanel implements ActionListener {

    private static final int MINIMUM_IMAGE_PADDING_IN_PX = 50;
    public static final int CURRENT_POSITION_MARKER_SIZE = 8;

    private final JButton prev2;
    private final JButton prev;
    private final JButton next;
    private final JButton next2;
    private final JButton playPause;
    private final JButton reset;

    private final JButton fileChoose = new JButton("...");
    private final JTextField filePath = new JTextField(/*"/home/sikripefg/sample-vbo-from-dbn.vbo"*/);

    private final AtomicBoolean playFlag = new AtomicBoolean(false);

    private long gpsDataIntervalMillis;

    private final TraveledRoutePanel traveledRoutePanel;

    private final JPanel controlsPanel = new JPanel();


    private int currentPositionIdx = 0;
    private final List<TraveledRoutePoint> traveledRoutePoints = new ArrayList<>();
    private double startTime;

    public TelemetryPlayer() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Telemetry"));

        traveledRoutePanel = new TraveledRoutePanel();

        add(traveledRoutePanel, BorderLayout.CENTER);

        final JPanel northPanel = new JPanel();
        northPanel.setLayout(new BoxLayout(northPanel, BoxLayout.LINE_AXIS));
        northPanel.add(filePath);
        northPanel.add(fileChoose);
        fileChoose.setToolTipText("Select a .vbo file that do not contain any video related data.");
        add(northPanel, BorderLayout.NORTH);

        fileChoose.addActionListener(this);

        prev2 = new JButton("<<");
        prev = new JButton("<");
        next = new JButton(">");
        next2 = new JButton(">>");
        reset = new JButton("Reset");
        playPause = new JButton("Play");

        controlsPanel.add(prev2);
        controlsPanel.add(prev);
        controlsPanel.add(playPause);
        controlsPanel.add(next);
        controlsPanel.add(next2);
        controlsPanel.add(reset);

        prev2.addActionListener(this);
        prev.addActionListener(this);
        playPause.addActionListener(this);
        reset.addActionListener(this);
        next.addActionListener(this);
        next2.addActionListener(this);

        add(controlsPanel, BorderLayout.SOUTH);

        enableControls(false);
    }

    private void loadTraveledRoute() {
        try {
            createRoutePoints(VboEditor.getTraveledRoute(filePath.getText()));
            repaint();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void chooseSourceVbo() {
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
            loadTraveledRoute();
            enableControls(true);
        }
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
            chooseSourceVbo();
        }
    }

    private void reset() {
        playFlag.set(false);
        currentPositionIdx = 0;
        playPause.setText("Play");
        enableScanControls(true);
        repaint();
    }

    private void enableScanControls(boolean enable) {
        prev.setEnabled(enable);
        prev2.setEnabled(enable);
        next.setEnabled(enable);
        next2.setEnabled(enable);
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
            playPause.setText("Play");
            enableScanControls(true);
            playFlag.set(false);
        } else {
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
    }

    void seek(int amount) {
        currentPositionIdx += amount;
        if (currentPositionIdx < 0) {
            currentPositionIdx = 0;
        } else if (currentPositionIdx >= traveledRoutePoints.size()) {
            currentPositionIdx = traveledRoutePoints.size() - 1;
        }
        repaint();
    }

    private void createRoutePoints(final List<TraveledRouteCoordinate> traveledRouteCoordinates) {
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
                g.drawString(getTime(), 5, 50);
            }
        }

        private String getSpeed() {
            return "Speed: " + traveledRoutePoints.get(currentPositionIdx).getSpeed();
        }

        private String getTime() {
            final long timeMillis = (long) (traveledRoutePoints.get(currentPositionIdx).getTime() * 1000 - startTime * 1000);

            long minutes = TimeUnit.MILLISECONDS.toMinutes(timeMillis);
            long seconds = TimeUnit.MILLISECONDS.toSeconds(timeMillis) - TimeUnit.MINUTES.toSeconds(minutes);
            long millis = timeMillis - TimeUnit.SECONDS.toMillis(seconds) - TimeUnit.MINUTES.toMillis(minutes);
            return String.format("Time: %02d:%02d.%03d", minutes, seconds, millis);
        }
    }

}
