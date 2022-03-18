package org.sikrip.vboeditor.gui;

import org.sikrip.vboeditor.engine.VboToDbn;
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
public class VboToDbnConverterPanel extends FileConverterPanelSupport {

    private final static Logger LOGGER = LoggerFactory.getLogger(VboToDbnConverterPanel.class);

    public VboToDbnConverterPanel(VboEditorApplication editor) {
        super(editor);
    }

    @Override
    String getConvertButtonText() {
        return "Convert to vbo";
    }

    @Override
    void doConvert() {
        try {
            final String outputFilePath = getOutFileFolderPath() + "/" + new File(getInputFilePath()).getName() + ".dbn";
            VboToDbn.convert(getInputFilePath(), outputFilePath);
            JOptionPane.showMessageDialog(
                null,
                "Dbn file: " + outputFilePath,
                "Conversion completed.",
                JOptionPane.INFORMATION_MESSAGE
            );
        } catch (Exception e) {
            ErrorHandler.showError("Could not convert", "Unable to perform vbo to dbn conversion.", e);
            LOGGER.error("Unable to perform vbo to dbn conversion", e);
        }
    }

    @Override
    FileFilter getFileNameExtensionFilter() {
        return new FileNameExtensionFilter(".vbo files", "vbo");
    }

    @Override
    String getTitle() {
        return "Convert from .vbo to .dbn";
    }
}
