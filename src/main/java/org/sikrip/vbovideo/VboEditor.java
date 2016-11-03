package org.sikrip.vbovideo;

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

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

public class VboEditor {

	private final static String[] DATA_SEPARATORS = { " ", ",", "\t" };

	public enum VideoType {
		MP4, AVI
	}

	/**
	 * Creates a new vbo file with video metadata.
	 *
	 * @param basePath
	 * 		the path of the working directory
	 * @param vboFileName
	 * 		the name of the original vbo file
	 * @param videoType
	 * 		the type of the video (AVI or MP4)
	 * @param sessionName
	 * 		the name of the session (used to name the final vbo file)
	 * @param videoSynchInterval
	 * 		the interval in ms used to synch the video with each log entry
	 * @param videoOffset
	 * 		the offset of the video start time relative to the vbo file
	 * @throws IOException
	 */
	public static void addVideoMetadata(String basePath, String vboFileName, VideoType videoType, String sessionName, int videoSynchInterval, int videoOffset)
			throws IOException {
		Map<String, List<String>> vboSections = readVboSections(basePath + vboFileName);

		List<String> headerData = vboSections.get("[header]");

		// add entries in header
		headerData.add("avifileindex");
		headerData.add("avisynctime");

		final String dataSeparator = getDataSeparator(vboSections);
		if (dataSeparator == null) {
			throw new RuntimeException(String.format("Could not find data separator. Supported separators are %s", DATA_SEPARATORS));
		}

		// add column names
		List<String> columnNamesSection = vboSections.get("[column names]");
		if (columnNamesSection != null && !columnNamesSection.isEmpty()) {
			columnNamesSection.set(0, columnNamesSection.get(0) + dataSeparator + "avifileindex" + dataSeparator + "avisynctime");
		}

		// add avi section
		vboSections.put("[avi]", Lists.newArrayList(sessionName, videoType.name()));

		// add video metadata

		final List<String> dataLines = vboSections.get("[data]");
		for (int i = 0; i < dataLines.size(); i++) {
			final String initialData = dataLines.get(i);
			final String finalData = String.format(initialData + dataSeparator + "0001" + dataSeparator + "%1$08d", (videoOffset + i * videoSynchInterval));
			dataLines.set(i, finalData);
		}

		// Create the final vbo file
		if (createOutputDirectory(basePath + "/" + sessionName)) {
			try (final BufferedWriter writer = new BufferedWriter(new FileWriter(basePath + "/" + sessionName + "/" + sessionName + "_data.vbo"))) {
				writer.write(String.format("File created on %s using VBO Editor\n\n", new Date()));

				writeSection(vboSections, writer, "[header]");
				writer.write("\n");
				writeSection(vboSections, writer, "[channel units]");
				writer.write("\n");
				writeSection(vboSections, writer, "[comments]");
				writer.write("\n");
				writeSection(vboSections, writer, "[avi]");
				writer.write("\n");
				writeSection(vboSections, writer, "[column names]");
				writer.write("\n");
				writeSection(vboSections, writer, "[data]");
			}
		} else {
			throw new RuntimeException("Cannot create output directory");
		}
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
			writer.write("\n");
			for (String sectionEntry : vboSections.get(sectionName)) {
				writer.write(sectionEntry);
				writer.write("\n");
			}
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
