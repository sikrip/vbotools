package org.sikrip.vboeditor.gui;

import javax.swing.*;

/**
 * GUI for the file conversion operations.
 */
public class FileConverterPanel extends JPanel {

    private final VboToDbnConverterPanel vboToDbnConverterPanel;
    private final DbnToVboConverterPanel dbnToVboConverterPanel;

    public FileConverterPanel(VboEditorApplication editor) {
        vboToDbnConverterPanel = new VboToDbnConverterPanel(editor);
        dbnToVboConverterPanel = new DbnToVboConverterPanel(editor);
        createGui();
    }

    private void createGui() {
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        add(vboToDbnConverterPanel);
        add(dbnToVboConverterPanel);
    }
}
