package com.ezasm.gui.menubar;

import com.ezasm.gui.Window;
import com.ezasm.util.FileIO;

import javax.swing.*;
import java.io.File;
import java.io.IOException;

import static com.ezasm.gui.util.DialogFactory.promptOverwriteDialog;
import static com.ezasm.gui.util.DialogFactory.promptWarningDialog;
import static com.ezasm.util.FileIO.*;

/**
 * Action functions for the menubar actions like Save, Save As, Open, New, etc.
 */
public class MenuActions {

    /**
     * Runs the action event for save as.
     */
    static void saveAs() {
        JFileChooser fileChooser = createFileChooser("Save", TEXT_FILE_MASK | EZ_FILE_MASK);
        fileChooser.setSelectedFile(new File("code.ez"));
        int fileChooserOption = fileChooser.showSaveDialog(null);
        if (fileChooserOption == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            boolean overwrite = promptOverwriteDialog(file);
            if (overwrite) {
                try {
                    FileIO.writeFile(file, Window.getInstance().getEditor().getText());
                    Window.getInstance().getEditor().setOpenFilePath(file.getPath());
                } catch (IOException e) {
                    promptWarningDialog("Error Saving File",
                            String.format("There was an error saving to '%s'", file.getName()));
                }
            }
        }
    }

    /**
     * Runs the action event for save.
     */
    static void save() {
        File fileToUpdate = new File(Window.getInstance().getEditor().getOpenFilePath());
        if (!fileToUpdate.exists()) {
            saveAs();
        } else {
            try {
                FileIO.writeFile(fileToUpdate, Window.getInstance().getEditor().getText());
            } catch (IOException e) {
                promptWarningDialog("Error Saving File",
                        String.format("There was an error saving to '%s'", fileToUpdate.getName()));
            }
        }
    }

    /**
     * Runs the action event for load.
     */
    static void load() {
        JFileChooser fileChooser = createFileChooser("Open File", TEXT_FILE_MASK | EZ_FILE_MASK);
        int fileChooserOption = fileChooser.showOpenDialog(null);
        if (fileChooserOption == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (file != null && file.exists() && file.canRead()) {
                try {
                    String content = FileIO.readFile(file);
                    Window.getInstance().getEditor().setText(content);
                    Window.getInstance().getEditor().setOpenFilePath(file.getPath());
                } catch (IOException ex) {
                    promptWarningDialog("Error Loading File",
                            String.format("There was an error loading '%s'", file.getName()));
                }
            }
        }
    }

    /**
     * Runs the action event for selecting an input file.
     */
    static void selectInputFile() {
        JFileChooser fileChooser = createFileChooser("Choose an Input File", TEXT_FILE_MASK);
        int fileChooserOption = fileChooser.showOpenDialog(null);
        if (fileChooserOption == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (file != null) {
                if (file.exists() && file.canRead()) {
                    Window.getInstance().setInputStream(file);
                } else {
                    promptWarningDialog("Error Reading File",
                            String.format("There was an error reading from '%s'\nOperation cancelled", file.getName()));
                }
            }
        }
    }

    /**
     * Runs the action event for selecting an input file.
     */
    static void selectOutputFile() {
        JFileChooser fileChooser = createFileChooser("Choose an Output File", TEXT_FILE_MASK);
        fileChooser.setSelectedFile(new File("output.txt"));
        int fileChooserOption = fileChooser.showSaveDialog(null);
        if (fileChooserOption == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (file != null) {
                boolean overwrite = promptOverwriteDialog(file);
                if (overwrite) {
                    Window.getInstance().setOutputStream(file);
                }
            }
        }
    }

}
