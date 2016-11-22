package org.sikrip.vboeditor.gui;


import org.sikrip.vboeditor.VboEditor;
import org.sikrip.vboeditor.model.TraveledRouteCoordinate;
import org.sikrip.vboeditor.model.TraveledRouteXY;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

final class GPSViewer extends JPanel implements ActionListener {

    private static final int MINIMUM_IMAGE_PADDING_IN_PX = 50;
    public static final int CURRENT_POSITION_MARKER_SIZE = 8;

    private final int width;
    private final int height;
    private final JButton prev2;
    private final JButton prev;
    private final JButton next;
    private final JButton next2;
    private final JButton playPause;
    private final JButton stop;
    private final JLabel speed;

    private final AtomicBoolean playFlag = new AtomicBoolean(false);

    // TODO detect interval from vbo file
    private final long interval = 100; //ms

    private final TraveledRoutePanel traveledRoutePanel;


    private int currentPositionIdx = 0;
    private final List<TraveledRouteXY> traveledRoutePoints = new ArrayList<>();

    public GPSViewer(int width, int height) {
        this.width = width;
        this.height = height;

        setLayout(new BorderLayout());
        traveledRoutePanel = new TraveledRoutePanel();

        add(traveledRoutePanel, BorderLayout.CENTER);

        final JPanel buttonsPanel = new JPanel();

        prev2 = new JButton("<<");
        prev = new JButton("<");
        next = new JButton(">");
        next2 = new JButton(">>");
        stop = new JButton("Stop");
        playPause = new JButton("Play");
        speed = new JLabel();

        buttonsPanel.add(prev2);
        buttonsPanel.add(prev);
        buttonsPanel.add(playPause);
        buttonsPanel.add(next);
        buttonsPanel.add(next2);
        buttonsPanel.add(stop);

        prev2.addActionListener(this);
        prev.addActionListener(this);
        playPause.addActionListener(this);
        stop.addActionListener(this);
        next.addActionListener(this);
        next2.addActionListener(this);

        add(speed, BorderLayout.NORTH);
        add(buttonsPanel, BorderLayout.SOUTH);
    }

    public Dimension getPreferredSize() {
        return new Dimension(width, height);
    }

    public void loadTraveledRoute(String vboFilePath) {
        try {
            createRoutePoints(VboEditor.getTraveledRoute(vboFilePath));
            speed.setText("Speed:" + traveledRoutePoints.get(currentPositionIdx).getSpeed());
            repaint();
        } catch (IOException e) {
            e.printStackTrace();
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
        } else if (source == stop) {
            stop();
        } else if (source == next) {
            seek(1);
        } else if (source == next2) {
            seek(2);
        }
    }

    private void stop() {
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

    void enableControls(boolean enable) {
        enableScanControls(enable);
        stop.setEnabled(enable);
        playPause.setEnabled(enable);
    }

    double getCurrentTime() {
        return currentPositionIdx * interval;
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
                                Thread.sleep(interval);
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

    public boolean isPlaying() {
        return playFlag.get();
    }

    private void seek(int amout) {
        currentPositionIdx += amout;
        if (currentPositionIdx < 0) {
            currentPositionIdx = 0;
        } else if (currentPositionIdx >= traveledRoutePoints.size()) {
            currentPositionIdx = traveledRoutePoints.size() - 1;
        }

        speed.setText("Speed:" + traveledRoutePoints.get(currentPositionIdx).getSpeed());
        repaint();
    }

    private void createRoutePoints(final List<TraveledRouteCoordinate> traveledRouteCoordinates) {
        final Dimension size = getSize();

        traveledRoutePoints.clear();

        final double actualWidth = size.getWidth();
        final double actualHeight = size.getHeight();

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

            traveledRoutePoints.add(new TraveledRouteXY(adjustedX, adjustedY, coordinate.getTime(), coordinate.getSpeed()));
        }
    }

    private class TraveledRoutePanel extends JPanel {

        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);

            final Polygon traveledRoute = new Polygon();

            for (TraveledRouteXY traveledRoutePoint : traveledRoutePoints) {
                traveledRoute.addPoint(traveledRoutePoint.getX(), traveledRoutePoint.getY());
            }
            g.drawPolygon(traveledRoute);
            g.setColor(Color.red);

            TraveledRouteXY currentPosition = traveledRoutePoints.get(currentPositionIdx);
            g.fillOval(currentPosition.getX() - CURRENT_POSITION_MARKER_SIZE / 2,
                    currentPosition.getY() - CURRENT_POSITION_MARKER_SIZE / 2,
                    CURRENT_POSITION_MARKER_SIZE, CURRENT_POSITION_MARKER_SIZE);
        }
    }

}
