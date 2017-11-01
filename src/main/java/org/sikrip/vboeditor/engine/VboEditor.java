package org.sikrip.vboeditor.engine;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.apache.commons.io.FileUtils;
import org.sikrip.vboeditor.model.TraveledRouteCoordinate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.sikrip.vboeditor.engine.VboIOUtils.readVboSections;
import static org.sikrip.vboeditor.engine.VboIOUtils.writeSection;
import static org.sikrip.vboeditor.engine.VboUtils.AVIFILEINDEX;
import static org.sikrip.vboeditor.engine.VboUtils.AVISYNCTIME;
import static org.sikrip.vboeditor.engine.VboUtils.COLUMN_NAMES_SECTION;
import static org.sikrip.vboeditor.engine.VboUtils.DATA_SECTION;
import static org.sikrip.vboeditor.engine.VboUtils.DATA_SEPARATORS;
import static org.sikrip.vboeditor.engine.VboUtils.HEADER_SECTION;
import static org.sikrip.vboeditor.engine.VboUtils.convertToMillis;
import static org.sikrip.vboeditor.engine.VboUtils.getDataLines;
import static org.sikrip.vboeditor.engine.VboUtils.getDataSeparator;
import static org.sikrip.vboeditor.engine.VboUtils.getGpsDataInterval;

public class VboEditor {

    private final static Logger LOGGER = LoggerFactory.getLogger(VboEditor.class);

    private static final String VIDEO_FILE_SUFFIX = "0001";
    private static final String NO_VIDEO_SYNCH_TIME = "-00000001";
    private static final String FINAL_VBO_FILE_SUFFIX = "Data.vbo";
    private static final String AVI_SECTION = "[avi]";

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

        final List<String> header = vboSections.get(HEADER_SECTION);
        final int timeIdx = header.indexOf("time");
        final int satellitesIdx = header.indexOf("satellites");
        final int latitudeIdx = header.indexOf("latitude");
        final int longitudeIdx = header.indexOf("longitude");
        final int speedIdx = header.indexOf("velocity kmh");

        final List<TraveledRouteCoordinate> coordinates = new ArrayList<>();
        for (String dataLine : vboSections.get(DATA_SECTION)) {
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

        final List<String> headerData = vboSections.get(HEADER_SECTION);
        final int aviIndexPosition = headerData.indexOf(AVIFILEINDEX);
        if (aviIndexPosition > -1) {
            LOGGER.warn("Source vbo file already contains AVIFILEINDEX information. AVIFILEINDEX data will be replaced in the new file.");
        } else {
            headerData.add(AVIFILEINDEX);
        }
        final int aviSyncPosition = headerData.indexOf(AVISYNCTIME);
        if (aviSyncPosition > -1) {
            LOGGER.warn("Source vbo file already contains AVISYNCTIME information. AVISYNCTIME data will be replaced in the new file.");
        } else  {
            headerData.add(AVISYNCTIME);
        }
        final String dataSeparator = getDataSeparator(vboSections);
        if (Strings.isNullOrEmpty(dataSeparator)) {
            throw new RuntimeException(
                    String.format("Could not find data separator. Supported separators are %s", DATA_SEPARATORS));
        }

        // add column names
        final List<String> columnNamesSection = vboSections.get(COLUMN_NAMES_SECTION);
        if (columnNamesSection != null && !columnNamesSection.isEmpty()) {
            // Make sure column names for video data are in place
            final String columnNames = columnNamesSection.get(0).replaceAll(AVIFILEINDEX, "").replaceAll(AVISYNCTIME, "").trim();
            columnNamesSection.set(0, columnNames + dataSeparator + AVIFILEINDEX + dataSeparator + AVISYNCTIME);
        }

        // add avi section
        vboSections.put(AVI_SECTION, Lists.newArrayList(sessionName, videoType.name()));

        // add video metadata
        final List<String> dataLines = getDataLines(vboSections, dataSeparator, aviIndexPosition, aviSyncPosition);
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
        // Update the data section with the lines containing the video data
        vboSections.put(DATA_SECTION, dataLines);

        // Create the final vbo file
        if (createOutputDirectory(outputDirBasePath + "/" + sessionName)) {
            try (final BufferedWriter writer = new BufferedWriter(
                    new FileWriter(
                            outputDirBasePath + "/" + sessionName + "/" + sessionName + FINAL_VBO_FILE_SUFFIX))) {
                writer.write(String.format("File created on %s using VBO Editor", new Date()));
                writer.newLine();
                writeSection(vboSections, writer, HEADER_SECTION);
                writeSection(vboSections, writer, "[comments]");
                //
                writeSection(vboSections, writer, "[channel units]");
                writeSection(vboSections, writer, "[session data]");
                writeSection(vboSections, writer, "[laptiming]");
                //
                writeSection(vboSections, writer, AVI_SECTION);
                writeSection(vboSections, writer, COLUMN_NAMES_SECTION);
                writeSection(vboSections, writer, DATA_SECTION);
            }
        } else {
            throw new RuntimeException("Cannot create output directory");
        }
    }

    private static boolean createOutputDirectory(String path) {
        File outDir = new File(path);
        return outDir.exists() || outDir.mkdir();
    }
}
