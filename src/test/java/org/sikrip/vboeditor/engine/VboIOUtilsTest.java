package org.sikrip.vboeditor.engine;

import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static junit.framework.TestCase.assertEquals;

import static org.sikrip.vboeditor.TestHelper.getTestResourceUrl;
import static org.sikrip.vboeditor.engine.VboIOUtils.readVboSections;

/**
 * Tests for {@link VboIOUtils}.
 */
public class VboIOUtilsTest {

    @Test
    public void verifyVboHeadersReading() throws IOException {
        Map<String, List<String>> vboSections = readVboSections(getTestResourceUrl("/sample.vbo").getPath());

        assertEquals(6, vboSections.size());

        assertEquals(7, vboSections.get("[header]").size());
        assertEquals(1, vboSections.get("[column names]").size());
        assertEquals(4420, vboSections.get("[data]").size());
    }
}
