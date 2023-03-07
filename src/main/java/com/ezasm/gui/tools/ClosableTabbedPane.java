package com.ezasm.gui.tools;

import com.ezasm.gui.util.IThemeable;
import com.ezasm.gui.util.Theme;

import javax.swing.*;
import java.awt.*;

public class ClosableTabbedPane extends TabbedPane {

    private final ClosableTabBuilder closeableTabBuilder;

    public ClosableTabbedPane() {
        super();
        closeableTabBuilder = new ClosableTabBuilder();
    }

    @Override
    public void applyTheme(Font font, Theme theme) {
        super.applyTheme(font, theme);
        closeableTabBuilder.setFont(font).setTheme(theme);

        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            Component component = tabbedPane.getTabComponentAt(i);
            if (component instanceof IThemeable themeable) {
                themeable.applyTheme(font, theme);
            }
        }
    }

    @Override
    public void addTab(JComponent component, Icon icon, String title, String tip) {
        super.addTab(component, icon, title, tip);
        closeableTabBuilder.setParent(tabbedPane).setTabName(title);
        tabbedPane.setTabComponentAt(tabbedPane.getTabCount() - 1, closeableTabBuilder.build());
    }
}
