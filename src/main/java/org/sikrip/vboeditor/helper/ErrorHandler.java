package org.sikrip.vboeditor.helper;

import javax.swing.*;
import static org.sikrip.vboeditor.helper.Constants.*;



/**
 * Shows error dialogs and sends reports.
 */
public final class ErrorHandler {

    private ErrorHandler(){
        // do not allow instantiation of this
    }

    public static void showError(String title, String message, Exception e) {
        JOptionPane.showMessageDialog(
                null,
                message,
                String.format("%s - %s", APP_TITLE, title),
                JOptionPane.ERROR_MESSAGE
        );
        // TODO send error report using e
    }
}
