package org.sikrip.vboeditor.gui;

import org.sikrip.vboeditor.engine.DbnToVbo;
import org.sikrip.vboeditor.helper.ErrorHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;

/**
 * GUI for the file conversion operations.
 */
public class DbnToVboConverterPanel extends FileConverterPanelSupport {

    private final static Logger LOGGER = LoggerFactory.getLogger(DbnToVboConverterPanel.class);

    public DbnToVboConverterPanel(VboEditorApplication editor) {
        super(editor);
    }

    @Override
    String getConvertButtonText() {
        return "Convert to dbn";
    }

    @Override
    void doConvert() {
        try {
            final String outputFilePath = getOutFileFolderPath() + "/" + new File(getInputFilePath()).getName() + ".vbo";
            DbnToVbo.convert(getInputFilePath(), outputFilePath);
            JOptionPane.showMessageDialog(
                null,
                "Vbo file: " + outputFilePath,
                "Conversion completed.",
                JOptionPane.INFORMATION_MESSAGE
            );
        } catch (Exception e) {
            ErrorHandler.showError("Could not convert", "Unable to perform dbn to vbo conversion.", e);
            LOGGER.error("Unable to perform dbn to vbo conversion", e);
        }
    }

    @Override
    FileFilter getFileNameExtensionFilter() {
        return new FileNameExtensionFilter(".dbn files", "dbn");
    }

    @Override
    String getTitle() {
        return "Convert from .dbn to .vbo";
    }
}
