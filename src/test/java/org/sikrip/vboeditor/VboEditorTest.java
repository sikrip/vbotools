package org.sikrip.vboeditor;

import static junit.framework.Assert.assertTrue;
import static junit.framework.TestCase.assertEquals;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import org.junit.Test;

public class VboEditorTest {

	@Test
	public void verifyDataIntervalIdentification() throws IOException {
		assertEquals(100, VboEditor.findGpsDataInterval(VboEditor.readVboSections(getTestResourceUrl("/sample-vbo-from-dbn.vbo").getPath()), " "));
		assertEquals(200, VboEditor.findGpsDataInterval(VboEditor.readVboSections(getTestResourceUrl("/sample.vbo").getPath()), " "));
	}

	@Test
	public void verifyVideoFileCreation() throws IOException {
		final String basePath = getTestResourceUrl("/").getPath();

		VboEditor.createVideoFile(basePath, basePath + "/sample.avi", "my-session");

		final File sourceVideo = new File(basePath + "/sample.avi");
		final File finalVideo = new File(basePath + "/my-session/my-session0001.avi");

		assertTrue(finalVideo.exists());
		assertEquals(sourceVideo.length(), finalVideo.length());
	}

	@Test
	public void verifyVideoMetadataIntegration_WhenGpsDataStartBeforeVideo() throws IOException {

		final String basePath = getTestResourceUrl("/").getPath();

		VboEditor.createVboWithVideoMetadata(basePath, basePath + "/sample-vbo-from-dbn.vbo", VboEditor.VideoType.MP4, "my-session", -2020);

		Map<String, List<String>> vboWithVideoSections = VboEditor.readVboSections(basePath + "/my-session/my-sessionData.vbo");

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

		// Expect 20 invalid data lines: 2020(offset) / 100(refresh rate)
		for (int i = 0; i < 20; i++) {
			assertTrue(data.get(i).contains("0001"));
			assertTrue(data.get(i).contains("-00000001"));
		}

		// Expect first valid data line to be 20: 2020 % 100
		assertTrue(data.get(20).contains("0001"));
		assertTrue(data.get(20).contains("00000020"));

		assertTrue(data.get(21).contains("0001"));
		assertTrue(data.get(21).contains("00000120"));

		assertTrue(data.get(22).contains("0001"));
		assertTrue(data.get(22).contains("00000220"));
	}

	@Test
	public void verifyVideoMetadataIntegration_WhenGpsDataStartBeforeVideoButOffsetIsLessThanGpsDataInterval() throws IOException {

		final String basePath = getTestResourceUrl("/").getPath();

		VboEditor.createVboWithVideoMetadata(basePath, basePath + "/sample-vbo-from-dbn.vbo", VboEditor.VideoType.MP4, "my-session", -20);

		Map<String, List<String>> vboWithVideoSections = VboEditor.readVboSections(basePath + "/my-session/my-sessionData.vbo");

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

		// Expect no invalid data lines and first line to start at 20: 20 % 100
		assertTrue(data.get(0).contains("0001"));
		assertTrue(data.get(0).contains("00000020"));

		assertTrue(data.get(1).contains("0001"));
		assertTrue(data.get(1).contains("00000120"));

		assertTrue(data.get(2).contains("0001"));
		assertTrue(data.get(2).contains("00000220"));
	}

	@Test
	public void verifyVideoMetadataIntegration_WhenGpsDataStartAfterVideo() throws IOException {

		final String basePath = getTestResourceUrl("/").getPath();

		VboEditor.createVboWithVideoMetadata(basePath, basePath + "/sample-vbo-from-dbn.vbo", VboEditor.VideoType.MP4, "my-session", 2000);

		Map<String, List<String>> vboWithVideoSections = VboEditor.readVboSections(basePath + "/my-session/my-sessionData.vbo");

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
		assertTrue(data.get(1).contains("00002100"));
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
