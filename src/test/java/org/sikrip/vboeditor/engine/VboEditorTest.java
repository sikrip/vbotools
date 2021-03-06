package org.sikrip.vboeditor.engine;

import org.junit.Test;
import org.sikrip.vboeditor.engine.VboEditor;
import org.sikrip.vboeditor.model.TraveledRouteCoordinate;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.List;
import java.util.Map;

import static junit.framework.Assert.assertTrue;
import static junit.framework.TestCase.assertEquals;
import static org.sikrip.vboeditor.TestHelper.getTestResourceUrl;
import static org.sikrip.vboeditor.engine.VboEditor.getTraveledRoute;
import static org.sikrip.vboeditor.engine.VboIOUtils.readVboSections;

/**
 * Tests for {@link VboEditor}.
 */
public class VboEditorTest {

    @Test
    public void verifyTraveledRouteExtraction() throws IOException, ParseException {
        List<TraveledRouteCoordinate> route = getTraveledRoute(getTestResourceUrl("/sample-vbo-for-route-test.vbo").getPath());

        assertEquals(21, route.size());
        assertEquals(100, route.get(0).getGpsDataInterval());
        assertEquals(48408300, route.get(0).getTime());
        assertEquals(02279.25223, route.get(0).getLatitude());
        assertEquals(-01401.70107, route.get(0).getLongitude());
        assertEquals(85.160, route.get(0).getSpeed());
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
    public void verifyHarrysLapTimerVbo_ShouldHaveCustomSections() throws IOException {
        final String basePath = getTestResourceUrl("/").getPath();

        VboEditor.createVboWithVideoMetadata(basePath, basePath + "/sampleHarrysLapTimer.vbo", VboEditor.VideoType.MP4, "my-session", 0);

        Map<String, List<String>> vboWithVideoSections = readVboSections(basePath + "/my-session/my-sessionData.vbo");

        assertTrue(vboWithVideoSections.containsKey("[session data]"));
        assertTrue(vboWithVideoSections.containsKey("[laptiming]"));
    }

    @Test
    public void verifyVideoMetadataIntegration_WhenGpsDataStartBeforeVideo() throws IOException {

        final String basePath = getTestResourceUrl("/").getPath();

        VboEditor.createVboWithVideoMetadata(basePath, basePath + "/sample-vbo-from-dbn.vbo", VboEditor.VideoType.MP4, "my-session", -2020);

        Map<String, List<String>> vboWithVideoSections = readVboSections(basePath + "/my-session/my-sessionData.vbo");

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

        Map<String, List<String>> vboWithVideoSections = readVboSections(basePath + "/my-session/my-sessionData.vbo");

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

        Map<String, List<String>> vboWithVideoSections = readVboSections(basePath + "/my-session/my-sessionData.vbo");

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
}
