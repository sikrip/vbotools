package org.sikrip.vboeditor.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Helper functions for the vbo file format.
 */
final class VboUtils {

    private VboUtils(){/*hidden*/}

    static final String HEADER_SECTION = "[header]";
    static final String COLUMN_NAMES_SECTION = "[column names]";
    static final String DATA_SECTION = "[data]";

    static final String[] DATA_SEPARATORS = { " ", ",", "\t" };
    static final String AVIFILEINDEX = "avifileindex";
    static final String AVISYNCTIME = "avisynctime";


    static String getDataSeparator(Map<String, List<String>> vboSections) {
        String dataLine = vboSections.get(DATA_SECTION).get(0);
        for (String separator : DATA_SEPARATORS) {
            if (dataLine.split(separator).length > 0) {
                return separator;
            }
        }
        return null;
    }

    /**
     * Converts the vbo format of time (UTC time since midnight in the form HH:MM:SS.SS) to milliseconds.
     *
     * @param time the time in the format used by the vbo files
     * @return the equivalent time in milliseconds
     */
    static long convertToMillis(String time) {
        // Time: This is UTC time since midnight in the form HH:MM:SS.SS,
        if (time.length() != 9) {
            throw new IllegalArgumentException(String.format("Unexpected VBO time value %s", time));
        }
        final long hh = Long.valueOf(time.substring(0, 2));
        final long mm = Long.valueOf(time.substring(2, 4));
        final long ss = Long.valueOf(time.substring(4, 6));
        final long millis = Long.valueOf(time.substring(7, 9)) * 10;
        return millis + ss * 1000 + mm * 60 * 1000 + hh * 60 * 60 * 1000;
    }

    /**
     * Gets the data sampling interval in milliseconds for the provided gps data.
     *
     * @param vboFileSections the gps data
     * @param dataSeparator   the separator of the gps data
     * @return the data sampling interval of the gps data
     */
    static int getGpsDataInterval(Map<String, List<String>> vboFileSections, String dataSeparator) {
        //FIXME
        final int numberOfSamples = 10;

        // skip some entries at the start of the data because such entries do not contain stable time info
        final int entriesToSkip = 10;

        if (vboFileSections.get(DATA_SECTION).size() < entriesToSkip + numberOfSamples) {
            throw new IllegalArgumentException("Data sample to small");
        }

        final int timeColumnIdx = vboFileSections.get(HEADER_SECTION).indexOf("time");

        long time = -1;
        long sumOfIntervals = 0;
        for (int i = entriesToSkip; i < entriesToSkip + numberOfSamples; i++) {

            long currentTime = convertToMillis(
                    vboFileSections.get(DATA_SECTION).get(i).split(dataSeparator)[timeColumnIdx]);

            if (time != -1) {
                sumOfIntervals += currentTime - time;
            }
            time = currentTime;
        }

        // find mean interval
        long intervalMillis = sumOfIntervals / (numberOfSamples - 1);

        int intervals[] = { 1000/*1hz*/, 200/*5hz*/, 100/*10hz*/, 50/*20hz*/ };

        long diffsPerInterval[] = { Math.abs(1000L - intervalMillis),
                Math.abs(200L - intervalMillis),
                Math.abs(100L - intervalMillis),
                Math.abs(50L - intervalMillis) };

        long minDiff = diffsPerInterval[0];
        int minDiffIdx = 0;
        for (int i = 1; i < diffsPerInterval.length; i++) {
            if (diffsPerInterval[i] < minDiff) {
                minDiff = diffsPerInterval[i];
                minDiffIdx = i;
            }
        }

        return intervals[minDiffIdx];
    }

    /**
     * Gets the data lines from the provided sections.
     * {@link #AVIFILEINDEX} and {@link #AVISYNCTIME} are excluded.
     */
    static List<String> getDataLines(Map<String, List<String>> vboSections, String separator, int aviIndexPosition, int aviSyncPosition ) {
        final List<String> dataLinesNoVideoData = new ArrayList<>();
        for (String line : vboSections.get(DATA_SECTION)) {
            final StringBuilder lineBuilder = new StringBuilder();
            final String[] dataArray = line.split(separator);
            for (int i = 0; i < dataArray.length; i++) {
                if (i != aviIndexPosition && i != aviSyncPosition) {
                    // add all data except the avi related
                    lineBuilder.append(dataArray[i]).append(separator);
                }
            }
            dataLinesNoVideoData.add(lineBuilder.toString().trim());
        }
        return dataLinesNoVideoData;
    }

    static List<Double> getAccelerationData(List<String> data, String separator, int timeDataIdx, int velocityDataIdx) {
        final List<Double> accelerationData = new ArrayList<>();

        // No acceleration data for the first row of data
        accelerationData.add(0.0);

        // Calculate the acceleration for all data lines.
        for (int i = 0; i < data.size()-1; i++) {
            final String currentData[] = data.get(i).split(separator);
            final String nextData[] = data.get(i+1).split(separator);;

            final double velocity = Double.valueOf(currentData[velocityDataIdx]);
            final double nextVelocity = Double.valueOf(nextData[velocityDataIdx]);

            final long time = convertToMillis(currentData[timeDataIdx]);
            final long nextTime = convertToMillis(nextData[timeDataIdx]);

            final double acceleration = (nextVelocity - velocity) / (nextTime - time);

            accelerationData.add(acceleration);
        }
        return accelerationData;
    }

    static List<Double> getBrakeChannel(List<Double> accelerationData, double maxBrake) {
        final List<Double> brakeChannel = new ArrayList<>();
        for (final Double acceleration : accelerationData) {
            if (acceleration < 0.0) {
                brakeChannel.add((maxBrake / acceleration) * 100.0);
            } else {
                brakeChannel.add(0.0);
            }
        }
        return brakeChannel;
    }
}
