package org.sikrip.vboeditor;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.apache.commons.io.FileUtils;
import org.sikrip.vboeditor.model.TraveledRouteCoordinate;

import java.io.*;
import java.util.*;

public class VboEditor {

    private static final String VIDEO_FILE_SUFFIX = "0001";
    private static final String NO_VIDEO_SYNCH_TIME = "-00000001";
    private final static String[] DATA_SEPARATORS = { " ", ",", "\t" };
    private static final String FINAL_VBO_FILE_SUFFIX = "Data.vbo";
    private static final String HEADER_KEY = "[header]";
    private static final String DATA_KEY = "[data]";

    public enum VideoType {
        MP4, AVI
    }

    /**
     * Gets a list of {@link TraveledRouteCoordinate}s depicting the traveled route of the given vbo file.
     *
     * @param vboFilePath the telemetry file
     * @return a list of {@link TraveledRouteCoordinate}s depicting the traveled route of the given vbo file.
     * @throws IOException when the telemetry file cannot be read
     */
    public static List<TraveledRouteCoordinate> getTraveledRoute(String vboFilePath) throws IOException {
        final Map<String, List<String>> vboSections = readVboSections(vboFilePath);
        final String dataSeparator = getDataSeparator(vboSections);

        if (Strings.isNullOrEmpty(dataSeparator)) {
            throw new IllegalArgumentException("Cannot determine data separator (check vbo file for corruption)");
        }
        final int gpsDataInterval = getGpsDataInterval(vboSections, dataSeparator);

        final List<String> header = vboSections.get(HEADER_KEY);
        final int timeIdx = header.indexOf("time");
        final int satellitesIdx = header.indexOf("satellites");
        final int latitudeIdx = header.indexOf("latitude");
        final int longitudeIdx = header.indexOf("longitude");
        final int speedIdx = header.indexOf("velocity kmh");

        final List<TraveledRouteCoordinate> coordinates = new ArrayList<>();
        for (String dataLine : vboSections.get(DATA_KEY)) {
            final String[] data = dataLine.split(dataSeparator);
            final Double latitude = Double.valueOf(data[latitudeIdx]);
            final Double longitude = Double.valueOf(data[longitudeIdx]);
            if (Integer.valueOf(data[satellitesIdx]) > 0 && latitude != 0 && longitude != 0) {
                final long time = convertToMillis(data[timeIdx]);
                final Double speed = Double.valueOf(data[speedIdx]);
                coordinates.add(new TraveledRouteCoordinate(latitude, longitude, time, speed, gpsDataInterval));
            }
        }
        return coordinates;
    }

    /**
     * Creates a video file that can be used for video/data analysis on Circuit Tools.
     *
     * @param outputDirBasePath the path of the working directory for the output files
     * @param originalVideoPath the path of the original file video
     * @param sessionName       the name of the session
     */
    public static void createVideoFile(String outputDirBasePath, String originalVideoPath, String sessionName)
            throws IOException {
        if (createOutputDirectory(outputDirBasePath + "/" + sessionName)) {
            final String videoExtension = originalVideoPath.substring(originalVideoPath.lastIndexOf('.'));
            final File sourceVideo = new File(originalVideoPath);
            final File finalVideo = new File(
                    outputDirBasePath + "/" + sessionName + "/" + sessionName + VIDEO_FILE_SUFFIX + videoExtension);
            FileUtils.copyFile(sourceVideo, finalVideo);
        } else {
            throw new RuntimeException("Could not create output directory");
        }
    }

    /**
     * Creates a new vbo file with video metadata.
     * <p/>
     * <p>New vbo file is created under {basePath}/{sessionName}/{sessionName}_data.vbo</p>
     *
     * @param outputDirBasePath the path of the working directory for the output files
     * @param originalVboPath   the name of the original vbo file
     * @param videoType         the type of the video (AVI or MP4)
     * @param sessionName       the name of the session (used to name the final vbo file)
     * @param gpsDataOffset     the offset of the gps data start time relative to the video
     *                          (positive values indicate that GPS data start AFTER the video,
     *                          negative values indicate that the GPS data start BEFORE the video)
     * @throws IOException if a file related error occurs
     */
    public static void createVboWithVideoMetadata(String outputDirBasePath, String originalVboPath,
            VideoType videoType, String sessionName, int gpsDataOffset)
            throws IOException {

        final Map<String, List<String>> vboSections = readVboSections(originalVboPath);

        final List<String> headerData = vboSections.get(HEADER_KEY);
        if (headerData.contains("avifileindex") || headerData.contains("avisynctime")) {
            throw new IllegalArgumentException(
                    "Source vbo file already contains video information, please select a file without video information.");
        }

        // add entries in header
        headerData.add("avifileindex");
        headerData.add("avisynctime");

        final String dataSeparator = getDataSeparator(vboSections);
        if (Strings.isNullOrEmpty(dataSeparator)) {
            throw new RuntimeException(
                    String.format("Could not find data separator. Supported separators are %s", DATA_SEPARATORS));
        }

        // add column names
        final List<String> columnNamesSection = vboSections.get("[column names]");
        if (columnNamesSection != null && !columnNamesSection.isEmpty()) {
            columnNamesSection
                    .set(0, columnNamesSection.get(0) + dataSeparator + "avifileindex" + dataSeparator + "avisynctime");
        }

        // add avi section
        vboSections.put("[avi]", Lists.newArrayList(sessionName, videoType.name()));

        // add video metadata
        final List<String> dataLines = vboSections.get(DATA_KEY);
        final int gpsDataInterval = getGpsDataInterval(vboSections, dataSeparator);

        int logLine = 0;
        int numberOfInvalidLogLines = 0;
        int firstValidOffset = 0;
        if (gpsDataOffset < 0) {
            // GPS data start before video
            numberOfInvalidLogLines = Math.abs(gpsDataOffset) / gpsDataInterval;
            firstValidOffset = Math.abs(gpsDataOffset) % gpsDataInterval;
            for (; logLine < numberOfInvalidLogLines; logLine++) {
                final String initialData = dataLines.get(logLine);
                final String finalData =
                        initialData + dataSeparator + VIDEO_FILE_SUFFIX + dataSeparator + NO_VIDEO_SYNCH_TIME;
                dataLines.set(logLine, finalData);
            }
        }

        for (; logLine < dataLines.size(); logLine++) {
            final String initialData = dataLines.get(logLine);
            final int logLineOffsetMS;
            if (gpsDataOffset < 0) {
                // GPS data start before video
                logLineOffsetMS = firstValidOffset + (logLine - numberOfInvalidLogLines) * gpsDataInterval;
            } else {
                logLineOffsetMS = gpsDataOffset + logLine * gpsDataInterval;
            }
            final String finalData = String
                    .format(initialData + dataSeparator + VIDEO_FILE_SUFFIX + dataSeparator + "%1$08d",
                            logLineOffsetMS);
            dataLines.set(logLine, finalData);
        }

        // Create the final vbo file
        if (createOutputDirectory(outputDirBasePath + "/" + sessionName)) {
            try (final BufferedWriter writer = new BufferedWriter(
                    new FileWriter(
                            outputDirBasePath + "/" + sessionName + "/" + sessionName + FINAL_VBO_FILE_SUFFIX))) {
                writer.write(String.format("File created on %s using VBO Editor", new Date()));
                writer.newLine();
                writeSection(vboSections, writer, HEADER_KEY);
                writeSection(vboSections, writer, "[comments]");
                //
                writeSection(vboSections, writer, "[channel units]");
                writeSection(vboSections, writer, "[session data]");
                writeSection(vboSections, writer, "[laptiming]");
                //
                writeSection(vboSections, writer, "[avi]");
                writeSection(vboSections, writer, "[column names]");
                writeSection(vboSections, writer, DATA_KEY);
            }
        } else {
            throw new RuntimeException("Cannot create output directory");
        }
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

        if (vboFileSections.get(DATA_KEY).size() < entriesToSkip + numberOfSamples) {
            throw new IllegalArgumentException("Data sample to small");
        }

        final int timeColumnIdx = vboFileSections.get(HEADER_KEY).indexOf("time");

        long time = -1;
        long sumOfIntervals = 0;
        for (int i = entriesToSkip; i < entriesToSkip + numberOfSamples; i++) {

            long currentTime = convertToMillis(
                    vboFileSections.get(DATA_KEY).get(i).split(dataSeparator)[timeColumnIdx]);

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

    static Map<String, List<String>> readVboSections(String vboFilePath) throws IOException {
        try (final InputStream vboFileStream = new FileInputStream(new File(vboFilePath))) {

            final BufferedReader vboReader = new BufferedReader(new InputStreamReader(vboFileStream));
            final Map<String, List<String>> vboFileSections = new HashMap<>();

            readAllSections(vboReader, null, vboFileSections);

            return vboFileSections;
        }
    }

    /**
     * Converts the vbo format of time (UTC time since midnight in the form HH:MM:SS.SS) to milliseconds.
     *
     * @param time the time in the format used by the vbo files
     * @return the equivalent time in milliseconds
     */
    private static long convertToMillis(String time) {
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

    private static void writeSection(Map<String, List<String>> vboSections, BufferedWriter writer, String sectionName)
            throws IOException {
        if (vboSections.containsKey(sectionName)) {
            writer.write(sectionName);
            writer.newLine();
            for (String sectionEntry : vboSections.get(sectionName)) {
                writer.write(sectionEntry);
                writer.newLine();
            }
            writer.newLine();
        }
    }

    private static String getDataSeparator(Map<String, List<String>> vboSections) {
        String dataLine = vboSections.get(DATA_KEY).get(0);
        for (String separator : DATA_SEPARATORS) {
            if (dataLine.split(separator).length > 0) {
                return separator;
            }
        }
        return null;
    }

    private static void readAllSections(BufferedReader vboReader, String sectionName,
            Map<String, List<String>> vboFileSections) throws IOException {
        String vboLine;
        final List<String> sectionData = new ArrayList<>();
        while ((vboLine = vboReader.readLine()) != null) {
            if (vboLine.startsWith("[")) {
                readAllSections(vboReader, vboLine, vboFileSections);
            } else if (!Strings.isNullOrEmpty(sectionName) && !Strings.isNullOrEmpty(vboLine)) {
                sectionData.add(vboLine);
            }
        }
        if (!Strings.isNullOrEmpty(sectionName)) {
            vboFileSections.put(sectionName, sectionData);
        }
    }

    private static boolean createOutputDirectory(String path) {
        File outDir = new File(path);
        return outDir.exists() || outDir.mkdir();
    }
}
