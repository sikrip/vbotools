package org.sikrip.vboeditor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

public class VboEditor {

	private static final String VIDEO_FILE_SUFFIX = "0001";
	private static final String NO_VIDEO_SYNCH_TIME = "-00000001";
	private final static String[] DATA_SEPARATORS = { " ", ",", "\t" };
	private static final String FINAL_VBO_FILE_SUFFIX = "Data.vbo";

	public enum VideoType {
		MP4, AVI
	}

	/**
	 * Creates a video file that can be used for video/data analysis on Circuit Tools.
	 *
	 * @param outputDirBasePath
	 * 		the path of the working directory for the output files
	 * @param originalVideoPath
	 * 		the path of the original file video
	 * @param sessionName
	 * 		the name of the session
	 */
	public static void createVideoFile(String outputDirBasePath, String originalVideoPath, String sessionName) throws IOException {
		if (createOutputDirectory(outputDirBasePath + "/" + sessionName)) {
			final String videoExtension = originalVideoPath.substring(originalVideoPath.lastIndexOf('.'));
			final File sourceVideo = new File(originalVideoPath);
			final File finalVideo = new File(outputDirBasePath + "/" + sessionName + "/" + sessionName + VIDEO_FILE_SUFFIX + videoExtension);
			FileUtils.copyFile(sourceVideo, finalVideo);
		} else {
			throw new RuntimeException("Could not create output directory");
		}
	}

	/**
	 * Creates a new vbo file with video metadata.
	 *
	 * <p>New vbo file is created under {basePath}/{sessionName}/{sessionName}_data.vbo</p>
	 *
	 * @param outputDirBasePath
	 * 		the path of the working directory for the output files
	 * @param originalVboPath
	 * 		the name of the original vbo file
	 * @param videoType
	 * 		the type of the video (AVI or MP4)
	 * @param sessionName
	 * 		the name of the session (used to name the final vbo file)
	 * @param gpsDataOffset
	 * 		the offset of the gps data start time relative to the video
	 * 		(positive values indicate that GPS data start AFTER the video,
	 * 		negative values indicate that the GPS data start BEFORE the video)
	 * @throws IOException
	 */
	public static void createVboWithVideoMetadata(String outputDirBasePath, String originalVboPath,
			VideoType videoType, String sessionName, int gpsDataOffset)
			throws IOException {

		final Map<String, List<String>> vboSections = readVboSections(originalVboPath);

		final List<String> headerData = vboSections.get("[header]");
		if(headerData.contains("avifileindex") || headerData.contains("avisynctime")){
			throw new IllegalArgumentException("Source vbo file already contains video information, please select a file without video information.");
		}

		// add entries in header
		headerData.add("avifileindex");
		headerData.add("avisynctime");

		final String dataSeparator = getDataSeparator(vboSections);
		if (dataSeparator == null) {
			throw new RuntimeException(String.format("Could not find data separator. Supported separators are %s", DATA_SEPARATORS));
		}

		// add column names
		final List<String> columnNamesSection = vboSections.get("[column names]");
		if (columnNamesSection != null && !columnNamesSection.isEmpty()) {
			columnNamesSection.set(0, columnNamesSection.get(0) + dataSeparator + "avifileindex" + dataSeparator + "avisynctime");
		}

		// add avi section
		vboSections.put("[avi]", Lists.newArrayList(sessionName, videoType.name()));

		// add video metadata
		final List<String> dataLines = vboSections.get("[data]");
		final int gpsDataInterval = findGpsDataInterval(vboSections, dataSeparator);

		int logLine = 0;
		int numberOfInvalidLogLines = 0;
		int firstValidOffset = 0;
		if (gpsDataOffset < 0) {
			// GPS data start before video
			numberOfInvalidLogLines = Math.abs(gpsDataOffset) / gpsDataInterval;
			firstValidOffset = Math.abs(gpsDataOffset) % gpsDataInterval;
			for (; logLine < numberOfInvalidLogLines; logLine++) {
				final String initialData = dataLines.get(logLine);
				final String finalData = initialData + dataSeparator + VIDEO_FILE_SUFFIX + dataSeparator + NO_VIDEO_SYNCH_TIME;
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
			final String finalData = String.format(initialData + dataSeparator + VIDEO_FILE_SUFFIX + dataSeparator + "%1$08d", logLineOffsetMS);
			dataLines.set(logLine, finalData);
		}

		// Create the final vbo file
		if (createOutputDirectory(outputDirBasePath + "/" + sessionName)) {
			try (final BufferedWriter writer = new BufferedWriter(
					new FileWriter(outputDirBasePath + "/" + sessionName + "/" + sessionName + FINAL_VBO_FILE_SUFFIX))) {
				writer.write(String.format("File created on %s using VBO Editor", new Date()));
				writer.newLine();
				writeSection(vboSections, writer, "[header]");
				writeSection(vboSections, writer, "[comments]");
				//
				writeSection(vboSections, writer, "[channel units]");
				writeSection(vboSections, writer, "[session data]");
				writeSection(vboSections, writer, "[laptiming]");
				//
				writeSection(vboSections, writer, "[avi]");
				writeSection(vboSections, writer, "[column names]");
				writeSection(vboSections, writer, "[data]");
			}
		} else {
			throw new RuntimeException("Cannot create output directory");
		}
	}

	public static int identifyGPSRefreshRate(String vboFilePath) throws IOException {

		final Map<String, List<String>> vboSections = readVboSections(vboFilePath);
		final int gpsDataInterval = findGpsDataInterval(vboSections, getDataSeparator(vboSections));

		switch (gpsDataInterval) {
		case 1000:
			return 1;
		case 200:
			return 5;
		case 100:
			return 10;
		case 50:
			return 20;
		default:
			throw new RuntimeException("Cannot identify GPS refresh rate");
		}
	}

	static int findGpsDataInterval(Map<String, List<String>> vboFileSections, String dataSeparator) {

		final int numberOfSamples = 10;

		// skip soma entries at the start of the data because sush entries do not contain stable time info
		final int entriesToSkip = 10;

		if (vboFileSections.get("[data]").size() < entriesToSkip + numberOfSamples) {
			throw new RuntimeException("Data sample to small");
		}

		final int timeColumnIdx = vboFileSections.get("[header]").indexOf("time");

		Double time = null;
		Double sumOfIntervals = 0.0;
		for (int i = entriesToSkip; i < entriesToSkip + numberOfSamples; i++) {
			Double currentTime = Double.valueOf(vboFileSections.get("[data]").get(i).split(dataSeparator)[timeColumnIdx]);

			if (time != null) {
				sumOfIntervals += currentTime - time;
			}
			time = currentTime;
		}

		// convert to millis
		Double intervalMillis = 1000 * (sumOfIntervals / (numberOfSamples - 1));

		int intervals[] = { 1000/*1hz*/, 200/*5hz*/, 100/*10hz*/, 50/*20hz*/ };

		int diffsPerInterval[] = { Math.abs(1000 - intervalMillis.intValue()),
				Math.abs(200 - intervalMillis.intValue()),
				Math.abs(100 - intervalMillis.intValue()),
				Math.abs(50 - intervalMillis.intValue()) };

		int minDiff = diffsPerInterval[0];
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

	private static void writeSection(Map<String, List<String>> vboSections, BufferedWriter writer, String sectionName) throws IOException {
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
		String dataLine = vboSections.get("[data]").get(0);
		for (String separator : DATA_SEPARATORS) {
			if (dataLine.split(separator).length > 0) {
				return separator;
			}
		}
		return null;
	}

	private static void readAllSections(BufferedReader vboReader, String sectionName, Map<String, List<String>> vboFileSections) throws IOException {
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
