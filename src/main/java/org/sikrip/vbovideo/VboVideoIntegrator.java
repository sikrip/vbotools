package org.sikrip.vbovideo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

public class VboVideoIntegrator {

	private final static String[] DATA_SEPARATORS = { " ", ",", "\t" };

	public enum VideoType {
		MP4, AVI
	}

	public void integrate(String vboFilePath, String videoFilePath, VideoType videoType, String sessionName, int dataCaptureInterval, int videoOffset)
			throws IOException {
		Map<String, List<String>> vboSections = readVboSections(vboFilePath);

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
			final String finalData = String.format(initialData + dataSeparator + "0001" + dataSeparator + "%8d", (videoOffset + i * dataCaptureInterval));
			dataLines.set(i, finalData);
		}

		//try (final BufferedWriter writer = new BufferedWriter(new FileWriter(sessionName + "_data.vbo"))) {
	}

	static Map<String, List<String>> readVboSections(String vboFilePath) throws IOException {
		try (final InputStream vboFileStream = new FileInputStream(new File(vboFilePath))) {

			final BufferedReader vboReader = new BufferedReader(new InputStreamReader(vboFileStream));
			final Map<String, List<String>> vboFileSections = new HashMap<>();

			readAllSections(vboReader, null, vboFileSections);

			return vboFileSections;
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
}
