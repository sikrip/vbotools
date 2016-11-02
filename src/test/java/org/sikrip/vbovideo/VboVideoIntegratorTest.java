package org.sikrip.vbovideo;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import org.junit.Test;

public class VboVideoIntegratorTest {

	@Test
	public void verifyIntegration() throws IOException {
		VboVideoIntegrator
				.integrate(getTestResourceUrl("/sample.vbo").getPath(), "path/to/my/video", VboVideoIntegrator.VideoType.MP4, "session-name", 200, 2000);
		// TODO verify
	}

	@Test
	public void verifyVboHeadersReading() throws IOException {
		Map<String, List<String>> vboSections = VboVideoIntegrator.readVboSections(getTestResourceUrl("/sample.vbo").getPath());

		// TODO verify
	}

	public static URL getTestResourceUrl(String filename) {
		URL resource = VboVideoIntegratorTest.class.getResource(filename);
		if (resource == null) {
			throw new RuntimeException("Cannot find resource:" + filename);
		}
		return resource;
	}
}
