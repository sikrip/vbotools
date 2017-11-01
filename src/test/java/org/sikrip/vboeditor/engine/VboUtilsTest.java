package org.sikrip.vboeditor.engine;

import org.junit.Test;

import java.io.IOException;

import static junit.framework.TestCase.assertEquals;
import static org.sikrip.vboeditor.TestHelper.getTestResourceUrl;
import static org.sikrip.vboeditor.engine.VboIOUtils.readVboSections;
import static org.sikrip.vboeditor.engine.VboUtils.getGpsDataInterval;

/**
 * Tests for {@link VboUtils}.
 */
public class VboUtilsTest {

    @Test
    public void verifyDataIntervalIdentification() throws IOException {
        assertEquals(100, getGpsDataInterval(readVboSections(getTestResourceUrl("/sample-vbo-from-dbn.vbo").getPath()), " "));
        assertEquals(200, getGpsDataInterval(readVboSections(getTestResourceUrl("/sample.vbo").getPath()), " "));
    }
}
