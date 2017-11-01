package org.sikrip.vboeditor.engine;

import org.junit.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static junit.framework.TestCase.assertEquals;
import static org.sikrip.vboeditor.TestHelper.getTestResourceUrl;
import static org.sikrip.vboeditor.engine.VboIOUtils.readVboSections;
import static org.sikrip.vboeditor.engine.VboUtils.DATA_SECTION;
import static org.sikrip.vboeditor.engine.VboUtils.getAccelerationData;
import static org.sikrip.vboeditor.engine.VboUtils.getDataSeparator;
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

    @Test
    public void verifyAccelerationData() throws IOException {
        final Map<String, List<String>> vboFileSections = readVboSections(
                getTestResourceUrl("/sample-vbo-from-dbn.vbo").getPath()
        );

        final List<String> data = vboFileSections.get(DATA_SECTION);
        final List<Double> accelerationData = getAccelerationData(
                data,
                getDataSeparator(vboFileSections), 1, 4
        );

        final Double min = Collections.min(accelerationData);
        final Double max = Collections.max(accelerationData);

        assertEquals(-0.04230000000000004, min);
        assertEquals(0.03650000000000006, max);
        assertEquals(0.0, accelerationData.get(0));
        assertEquals(data.size(), accelerationData.size());
    }
}
