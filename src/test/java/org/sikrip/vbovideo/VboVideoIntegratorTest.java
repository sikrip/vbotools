package org.sikrip.vbovideo;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import org.junit.Test;

public class VboVideoIntegratorTest {

	@Test
	public void verifyVboHeadersReading() throws IOException {
		Map<String, List<String>> vboSections = VboVideoIntegrator.readVboSections(getTestResourceUrl("/sample.vbo").getPath());

		System.out.println(vboSections);
	}

	public static URL getTestResourceUrl(String filename) {
		URL resource = VboVideoIntegratorTest.class.getResource(filename);
		if (resource == null) {
			throw new RuntimeException("Cannot find resource:" + filename);
		}
		return resource;
	}
}
