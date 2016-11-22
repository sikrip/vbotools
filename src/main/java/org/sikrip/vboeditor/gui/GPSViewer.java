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

final class GPSViewer extends JPanel implements ActionListener {

    private static final int MINIMUM_IMAGE_PADDING_IN_PX = 50;
    public static final int CURRENT_POSITION_MARKER_SIZE = 8;

    private final int width;
    private final int height;
    private final JButton next;
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

        next = new JButton(">");
        buttonsPanel.add(next);
        next.addActionListener(this);

        add(buttonsPanel, BorderLayout.SOUTH);
    }

    public Dimension getPreferredSize() {
        return new Dimension(width, height);
    }

    public void loadTraveledRoute(String vboFilePath) {
        try {
            createRoutePoints(VboEditor.getTraveledRoute(vboFilePath));
            repaint();
            invalidate();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        final Object source = e.getSource();
        if (source == next) {
            next();
        }
    }

    private void next() {
        currentPositionIdx++;
        repaint();
        invalidate();
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
