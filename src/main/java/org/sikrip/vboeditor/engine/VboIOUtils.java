package org.sikrip.vboeditor.engine;

import com.google.common.base.Strings;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utils for reading and writing vbo files.
 */
final class VboIOUtils {

    private VboIOUtils() {/*hidden*/}

    static Map<String, List<String>> readVboSections(String vboFilePath) throws IOException {
        try (final InputStream vboFileStream = new FileInputStream(new File(vboFilePath))) {

            final BufferedReader vboReader = new BufferedReader(new InputStreamReader(vboFileStream));
            final Map<String, List<String>> vboFileSections = new HashMap<>();

            readAllSections(vboReader, null, vboFileSections);

            return vboFileSections;
        }
    }

    static void writeSection(Map<String, List<String>> vboSections, BufferedWriter writer, String sectionName)
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
}
