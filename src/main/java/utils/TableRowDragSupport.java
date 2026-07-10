package utils;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.awt.Cursor;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DragSource;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 表格行上下拖动排序；松手后回调当前 model 顺序下的编码列表以便写配置。
 */
public final class TableRowDragSupport {
    private TableRowDragSupport() {
    }

    public static void enable(JTable table, int codeColumnIndex, Consumer<List<String>> onOrderChanged) {
        table.setDragEnabled(true);
        table.setDropMode(DropMode.INSERT_ROWS);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowSorter(null);
        table.setTransferHandler(new TransferHandler() {
            private int fromIndex = -1;

            @Override
            public int getSourceActions(JComponent c) {
                return MOVE;
            }

            @Override
            protected Transferable createTransferable(JComponent c) {
                fromIndex = table.getSelectedRow();
                if (fromIndex < 0) {
                    return null;
                }
                return new StringSelection(String.valueOf(fromIndex));
            }

            @Override
            public boolean canImport(TransferSupport support) {
                if (!support.isDrop() || !support.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                    return false;
                }
                JTable.DropLocation dl = (JTable.DropLocation) support.getDropLocation();
                if (dl == null || dl.getRow() < 0) {
                    return false;
                }
                support.setShowDropLocation(true);
                table.setCursor(fromIndex >= 0 ? DragSource.DefaultMoveDrop : DragSource.DefaultMoveNoDrop);
                return true;
            }

            @Override
            public boolean importData(TransferSupport support) {
                if (!canImport(support) || fromIndex < 0) {
                    return false;
                }
                JTable.DropLocation dl = (JTable.DropLocation) support.getDropLocation();
                int toIndex = dl.getRow();
                TableModel model = table.getModel();
                if (!(model instanceof DefaultTableModel)) {
                    return false;
                }
                DefaultTableModel dtm = (DefaultTableModel) model;
                int rowCount = dtm.getRowCount();
                if (toIndex < 0) {
                    toIndex = rowCount;
                }
                if (toIndex > rowCount) {
                    toIndex = rowCount;
                }
                if (fromIndex == toIndex || fromIndex + 1 == toIndex) {
                    table.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                    return false;
                }
                try {
                    @SuppressWarnings("unchecked")
                    java.util.Vector<Object> row = (java.util.Vector<Object>) dtm.getDataVector().get(fromIndex);
                    java.util.Vector<Object> rowCopy = new java.util.Vector<>(row);
                    dtm.removeRow(fromIndex);
                    if (toIndex > fromIndex) {
                        toIndex--;
                    }
                    dtm.insertRow(toIndex, rowCopy);
                    table.setRowSelectionInterval(toIndex, toIndex);
                    if (onOrderChanged != null) {
                        List<String> codes = new ArrayList<>();
                        for (int i = 0; i < dtm.getRowCount(); i++) {
                            Object v = dtm.getValueAt(i, codeColumnIndex);
                            if (v != null) {
                                codes.add(v.toString());
                            }
                        }
                        onOrderChanged.accept(codes);
                    }
                    return true;
                } finally {
                    table.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                    fromIndex = -1;
                }
            }

            @Override
            protected void exportDone(JComponent source, Transferable data, int action) {
                table.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                fromIndex = -1;
            }
        });
    }

    public static void persistOrder(String configKey, List<String> orderedCodes) {
        if (orderedCodes == null || orderedCodes.isEmpty()) {
            return;
        }
        List<HoldingConfig.Entry> list = HoldingConfig.load(configKey);
        List<HoldingConfig.Entry> reordered = new ArrayList<>();
        for (String code : orderedCodes) {
            HoldingConfig.Entry e = HoldingConfig.findByCode(list, code);
            if (e != null) {
                reordered.add(e);
            } else {
                reordered.add(new HoldingConfig.Entry(code, "", ""));
            }
        }
        for (HoldingConfig.Entry e : list) {
            if (HoldingConfig.findByCode(reordered, e.code) == null) {
                reordered.add(e);
            }
        }
        HoldingConfig.save(configKey, reordered);
    }
}
