package com.ezasm.gui;

import javax.swing.*;

import org.fife.ui.rsyntaxtextarea.*;
import org.fife.ui.rtextarea.RTextScrollPane;

import java.awt.*;
import java.io.IOException;

import static com.ezasm.gui.Theme.applyFontAndTheme;

/**
 * The editor pane within the GUI. Allows the user to type code or edit loaded code.
 */
public class EditorPane extends JPanel implements IThemeable {

    private final RSyntaxTextArea textArea;
    private final RTextScrollPane scrollPane;
    private static final String EZASM_TOKEN_MAKER_NAME = "text/ezasm";
    private static final Dimension MIN_SIZE = new Dimension(600, 400);
    private static final Dimension MAX_SIZE = new Dimension(600, 2000);

    /**
     * Creates a text edit field using RSyntaxTextArea features.
     */
    public EditorPane() {
        super();

        ((AbstractTokenMakerFactory) TokenMakerFactory.getDefaultInstance()).putMapping(EZASM_TOKEN_MAKER_NAME,
                EzASMTokenMaker.class.getName());
        textArea = new RSyntaxTextArea();
        textArea.setSyntaxEditingStyle(EZASM_TOKEN_MAKER_NAME);
        textArea.setTabSize(2);
        textArea.setCodeFoldingEnabled(false);

        scrollPane = new RTextScrollPane(textArea);
        scrollPane.setLineNumbersEnabled(true);
        scrollPane.setMinimumSize(textArea.getSize());
        scrollPane.setPreferredSize(textArea.getPreferredSize());

        setMaximumSize(MAX_SIZE);
        setLayout(new BorderLayout());
        add(scrollPane);
    }

    /**
     * Themes the syntax text area according to the given font and theme.
     */
    private void themeSyntaxTextArea(Font font, Theme newTheme) {
        try {
            // Load a good default theme for things we are not setting
            org.fife.ui.rsyntaxtextarea.Theme theme = org.fife.ui.rsyntaxtextarea.Theme
                    .load(getClass().getResourceAsStream("/org/fife/ui/rsyntaxtextarea/themes/idea.xml"));

            // Background and caret theme
            theme.bgColor = newTheme.getBackground();
            theme.marginLineColor = newTheme.getBackground();
            theme.caretColor = newTheme.getForeground();

            // Line number background theme
            theme.gutterBackgroundColor = newTheme.getBackground().darker();
            theme.gutterBorderColor = theme.gutterBackgroundColor;

            // Line number theme
            theme.lineNumberFont = font.getFontName();
            theme.lineNumberFontSize = font.getSize();
            theme.lineNumberColor = newTheme.modifyAwayFromBackground(newTheme.getBackground(), 3);
            theme.currentLineNumberColor = newTheme.modifyTowardsBackground(newTheme.getForeground(), 1);
            theme.currentLineHighlight = newTheme.getCurrentLine();

            // Token-specific theme
            theme.scheme.getStyle(Token.NULL).foreground = newTheme.getForeground();
            theme.scheme.getStyle(Token.IDENTIFIER).foreground = newTheme.getForeground();
            theme.scheme.getStyle(Token.SEPARATOR).foreground = newTheme.getForeground();
            theme.scheme.getStyle(Token.WHITESPACE).foreground = newTheme
                    .modifyAwayFromBackground(newTheme.getCurrentLine());
            theme.scheme.getStyle(Token.COMMENT_EOL).foreground = newTheme.getComment();
            theme.scheme.getStyle(Token.LITERAL_CHAR).foreground = newTheme.getGreen();
            theme.scheme.getStyle(Token.LITERAL_STRING_DOUBLE_QUOTE).foreground = newTheme.getGreen();
            theme.scheme.getStyle(Token.LITERAL_NUMBER_DECIMAL_INT).foreground = newTheme.getPurple();
            theme.scheme.getStyle(Token.RESERVED_WORD).foreground = newTheme.getCyan();
            theme.scheme.getStyle(Token.VARIABLE).foreground = newTheme.getYellow();
            theme.scheme.getStyle(Token.ERROR_IDENTIFIER).foreground = newTheme.getRed();
            theme.scheme.getStyle(Token.ERROR_NUMBER_FORMAT).foreground = newTheme.getRed();

            theme.apply(textArea);
            setFont(textArea, font);
            textArea.revalidate();
        } catch (IOException e) { // Never happens
            e.printStackTrace();
        }
    }

    /**
     * Set the font for all token types.
     *
     * @param textArea The text area to modify.
     * @param font     The font to use.
     */
    private static void setFont(RSyntaxTextArea textArea, Font font) {
        if (font != null) {
            SyntaxScheme ss = textArea.getSyntaxScheme();
            ss = (SyntaxScheme) ss.clone();
            for (int i = 0; i < ss.getStyleCount(); i++) {
                if (ss.getStyle(i) != null) {
                    ss.getStyle(i).font = font;
                }
            }
            textArea.setSyntaxScheme(ss);
            textArea.setFont(font);
        }
    }

    /**
     * Applies the proper theming to the editor area.
     */
    public void applyTheme(Font font, Theme theme) {
        themeSyntaxTextArea(font, theme);
        setFont(textArea, font);

        applyFontAndTheme(scrollPane, font, theme);
        theme.applyThemeScrollbar(scrollPane.getHorizontalScrollBar());
        theme.applyThemeScrollbar(scrollPane.getVerticalScrollBar());
    }

    /**
     * Sets the editable state of the text field based on the boolean given.
     *
     * @param value true to be editable, false to not be editable.
     */
    public void setEditable(boolean value) {
        textArea.setEnabled(value);
    }

    /**
     * Gets the truth value of whether the editor can be typed in.
     *
     * @return true if the editor can be typed in currently, false otherwise.
     */
    public boolean getEditable() {
        return textArea.isEnabled();
    }

    /**
     * Gets the text content of the text editor.
     *
     * @return the text content of the text editor.
     */
    public String getText() {
        return textArea.getText();
    }

    /**
     * Sets the text of the editor to the given content.
     *
     * @param content the text to set the text within the editor to.
     */
    public void setText(String content) {
        textArea.setText(content);
    }
}
