package com.ezasm.gui;

import com.ezasm.simulation.Registers;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;

/**
 * The GUI display table of the registers. Has a scroll pane embedded.
 */
public class RegisterTable extends JPanel implements IThemeable {

    private final JTable table;
    private final Registers registers;
    private final JScrollPane scrollPane;
    private static final Dimension MIN_SIZE = new Dimension(150, 2000);
    private static final Dimension MAX_SIZE = new Dimension(200, 2000);

    /**
     * Given the registers, construct a table which displays the names and values of each one.
     *
     * @param registers the registers to read from.
     */
    public RegisterTable(Registers registers) {
        super();
        this.registers = registers;
        table = new JTable();
        table.setModel(new RegistersTableModel(registers));

        scrollPane = new JScrollPane(table);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setPreferredSize(table.getPreferredSize());

        setPreferredSize(new Dimension(MAX_SIZE.width, getHeight()));
        setMaximumSize(MAX_SIZE);
        setLayout(new BorderLayout());
        add(scrollPane);
    }

    /**
     * Applies the proper theming to the editor area
     */
    public void applyTheme(Font font, Theme theme) {
        theme.applyThemeScrollbar(scrollPane.getVerticalScrollBar());
        Theme.applyFontAndTheme(this, font, theme);
        Theme.applyFontAndTheme(table, font, theme);
        table.setRowHeight(font.getSize() + 3);
        table.getTableHeader().setOpaque(false);
        table.getTableHeader().setBackground(theme.currentLine());
        table.getTableHeader().setForeground(theme.foreground());
    }

    /**
     * Forcibly refreshes the display of the table
     */
    public void update() {
        SwingUtilities.invokeLater(table::updateUI);
    }

    /**
     * Helper model class to inform the TableModel of how to construct and read from itself.
     */
    private class RegistersTableModel extends AbstractTableModel {

        private static final String[] columns = { "Register", "Value" };

        private final Registers registers;

        public RegistersTableModel(Registers registers) {
            super();
            this.registers = registers;
        }

        public int getRowCount() {
            return registers.getRegisters().length;
        }

        public int getColumnCount() {
            return columns.length;
        }

        public Object getValueAt(int row, int col) {
            if (col == 0) {
                // labels
                return "$" + Registers.getRegisterName(row);
            } else if (col == 1) {
                // values
                return registers.getRegister(row).getLong();
            } else {
                // Error
                throw new RuntimeException();
            }
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            fireTableCellUpdated(rowIndex, columnIndex);
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            return false;
        }

        @Override
        public String getColumnName(int column) {
            return columns[column];
        }
    }

}
