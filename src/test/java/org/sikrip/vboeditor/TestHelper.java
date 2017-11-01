package org.sikrip.vboeditor;

import org.sikrip.vboeditor.engine.VboEditorTest;

import java.net.URL;

/**
 * Helper functions for testing.
 */
public final class TestHelper {

    private TestHelper(){/*hidden*/}

    public static URL getTestResourceUrl(String filename) {
        URL resource = VboEditorTest.class.getResource(filename);
        if (resource == null) {
            throw new RuntimeException("Cannot find resource:" + filename);
        }
        return resource;
    }
}
