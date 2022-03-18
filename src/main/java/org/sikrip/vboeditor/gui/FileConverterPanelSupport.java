package org.sikrip.vboeditor.gui;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

/**
 * GUI for the file conversion operations.
 */
public abstract class FileConverterPanelSupport extends JPanel {

    private final VboEditorApplication editor;

    private final JTextField inputFilePath = new JTextField();
    private final JButton inputFileChooseBtn = new JButton("...");
    private final JTextField outFileFolderPath = new JTextField();
    private final JButton convertBtn = new JButton(getConvertButtonText());

    public FileConverterPanelSupport(VboEditorApplication editor) {
        this.editor = editor;
        createGUI();
        addActionListeners();
    }

    public String getInputFilePath() {
        return inputFilePath.getText();
    }

    public String getOutFileFolderPath() {
        return outFileFolderPath.getText();
    }

    private void createGUI() {
        setBorder(BorderFactory.createTitledBorder(getTitle()));
        JPanel innerPanel = new JPanel();
        innerPanel.add(new JLabel("Choose input file"));
        innerPanel.add(inputFilePath);
        innerPanel.add(inputFileChooseBtn);
        add(innerPanel);

        innerPanel = new JPanel();
        outFileFolderPath.setEditable(false);
        outFileFolderPath.setEnabled(false);
        innerPanel.add(new JLabel("Output folder"));
        innerPanel.add(outFileFolderPath);
        add(innerPanel);

        innerPanel = new JPanel();
        innerPanel.add(convertBtn);
        add(innerPanel);

        // adjust sizes
        inputFilePath.setPreferredSize(new Dimension(300, 30));
        outFileFolderPath.setPreferredSize(new Dimension(300, 30));
    }

    private void addActionListeners() {
        inputFileChooseBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                chooseVboFile();
            }
        });
        convertBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                convert();
            }
        });
    }

    private void chooseVboFile() {
        final JFileChooser fileChooser = new JFileChooser();
        if (VboEditorApplication.getBrowsePath() != null) {
            fileChooser.setCurrentDirectory(new File(VboEditorApplication.getBrowsePath()));
        }
        fileChooser.setFileFilter(getFileNameExtensionFilter());
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            final File selectedFile = fileChooser.getSelectedFile();
            inputFilePath.setText(selectedFile.getAbsolutePath());
            outFileFolderPath.setText(selectedFile.getParent());
        }
    }

    private void convert() {
        final Component messageDialogParent = this;
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                doConvert();
                return null;
            }
            @Override
            protected void done() {
                editor.hideWaitDlg();
            }
        };
        worker.execute();
        editor.showWaitDlg();
    }

    abstract String getConvertButtonText();
    abstract FileFilter getFileNameExtensionFilter();
    abstract String getTitle();
    abstract void doConvert();
}
