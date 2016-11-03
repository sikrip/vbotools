package org.sikrip.vbovideo;

import static junit.framework.Assert.assertTrue;
import static junit.framework.TestCase.assertEquals;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import org.junit.Test;

public class VboEditorTest {

	@Test
	public void verifyVideoMetadataIntegration() throws IOException {

		VboEditor.addVideoMetadata(getTestResourceUrl("/").getPath(), "sample-vbo-from-dbn.vbo", VboEditor.VideoType.MP4, "my-session", 200, 2000);

		Map<String, List<String>> vboWithVideoSections = VboEditor.readVboSections(getTestResourceUrl("/my-session/my-session_data.vbo").getPath());

		List<String> headers = vboWithVideoSections.get("[header]");
		assertTrue(headers.contains("avifileindex"));
		assertTrue(headers.contains("avisynctime"));

		List<String> videoMetadata = vboWithVideoSections.get("[avi]");
		assertTrue(videoMetadata.contains("my-session"));
		assertTrue(videoMetadata.contains("MP4"));
		assertEquals(2, videoMetadata.size());

		List<String> columnNames = vboWithVideoSections.get("[column names]");
		assertEquals(1, columnNames.size());
		assertTrue(columnNames.get(0).contains("avifileindex"));
		assertTrue(columnNames.get(0).contains("avisynctime"));

		List<String> data = vboWithVideoSections.get("[data]");
		assertEquals(8374, data.size());
		assertTrue(data.get(0).contains("0001"));
		assertTrue(data.get(0).contains("00002000"));
		assertTrue(data.get(1).contains("0001"));
		assertTrue(data.get(1).contains("00002200"));
	}

	@Test
	public void verifyVboHeadersReading() throws IOException {
		Map<String, List<String>> vboSections = VboEditor.readVboSections(getTestResourceUrl("/sample.vbo").getPath());

		assertEquals(6, vboSections.size());

		assertEquals(7, vboSections.get("[header]").size());
		assertEquals(1, vboSections.get("[column names]").size());
		assertEquals(4420, vboSections.get("[data]").size());
	}

	public static URL getTestResourceUrl(String filename) {
		URL resource = VboEditorTest.class.getResource(filename);
		if (resource == null) {
			throw new RuntimeException("Cannot find resource:" + filename);
		}
		return resource;
	}
}
