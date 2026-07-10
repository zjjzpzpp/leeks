package ui;

import handler.StockRefreshHandler;
import handler.TencentStockHandler;
import quartz.HandlerJob;
import quartz.QuartzManager;
import utils.Configs;
import utils.HoldingConfig;
import utils.HoldingEditDialog;
import utils.TableRowDragSupport;
import utils.WindowUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.List;

public class StockPanel extends JPanel {
    private static final String CONFIG_KEY = "key_stocks";
    private static final String VISIBLE_KEY = "stock_visible_columns";
    private final JTable table;
    private final JLabel refreshTimeLabel;
    private final StockRefreshHandler handler;
    private final JButton btnRefresh;
    private final JButton btnStop;
    private final JButton btnColumns;
    private final JButton btnAdd;
    private final JButton btnEdit;
    private final JButton btnDelete;

    public StockPanel() {
        super(new BorderLayout());
        refreshTimeLabel = new JLabel();
        refreshTimeLabel.setBorder(new EmptyBorder(0, 5, 0, 5));

        table = new JTable();
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        handler = new TencentStockHandler(table, refreshTimeLabel);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        table.setRowSorter(null);
        TableRowDragSupport.enable(table, handler.codeColumnIndex, codes ->
                TableRowDragSupport.persistOrder(CONFIG_KEY, codes));

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedRow() >= 0) {
                    editSelected();
                }
            }
        });

        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        btnRefresh = new JButton("刷新");
        btnRefresh.addActionListener(e -> refresh());
        btnStop = new JButton("停止");
        btnStop.addActionListener(e -> stop());
        btnAdd = new JButton("新增");
        btnAdd.addActionListener(e -> addRow());
        btnEdit = new JButton("编辑");
        btnEdit.addActionListener(e -> editSelected());
        btnDelete = new JButton("删除");
        btnDelete.addActionListener(e -> deleteSelected());
        btnColumns = new JButton("列设置");
        btnColumns.addActionListener(e -> showColumnDialog());
        toolbar.add(btnRefresh);
        toolbar.add(btnStop);
        toolbar.addSeparator();
        toolbar.add(btnAdd);
        toolbar.add(btnEdit);
        toolbar.add(btnDelete);
        toolbar.addSeparator();
        toolbar.add(btnColumns);
        toolbar.add(Box.createHorizontalGlue());
        toolbar.add(refreshTimeLabel);

        add(toolbar, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    private void rebuildTableColumns() {
        table.setModel(new javax.swing.table.DefaultTableModel());
        table.setModel(handler);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        handler.reapplyColumnColors();
        applyColumnVisibility();
    }

    private void showColumnDialog() {
        String[] allCols = getDefaultColumns();
        String visibleStr = Configs.get().getValue(VISIBLE_KEY);
        ColumnSelectionDialog dlg = new ColumnSelectionDialog(
                (Frame) SwingUtilities.getWindowAncestor(this), "股票列设置", allCols, visibleStr);
        dlg.setVisible(true);
        if (dlg.isApplied()) {
            Configs.get().setValue(VISIBLE_KEY, dlg.getSelectedColumns());
            rebuildTableColumns();
        }
    }

    private String[] getDefaultColumns() {
        return WindowUtils.STOCK_TABLE_HEADER_VALUE.split(",");
    }

    private void addRow() {
        Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
        HoldingEditDialog dlg = new HoldingEditDialog(owner, "新增股票", "股票编码", "持仓",
                "", "", "", true);
        dlg.setVisible(true);
        if (!dlg.isApplied()) {
            return;
        }
        HoldingConfig.Entry entry = dlg.getEntry();
        List<HoldingConfig.Entry> list = HoldingConfig.load(CONFIG_KEY);
        if (HoldingConfig.findByCode(list, entry.code) != null) {
            JOptionPane.showMessageDialog(this, "编码已存在：" + entry.code, "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        list.add(entry);
        HoldingConfig.save(CONFIG_KEY, HoldingConfig.mergeByCode(list));
        refresh();
    }

    private void editSelected() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(this, "请先选中一行", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int modelRow = table.convertRowIndexToModel(viewRow);
        Object codeObj = handler.getValueAt(modelRow, handler.codeColumnIndex);
        if (codeObj == null) {
            return;
        }
        String code = codeObj.toString();
        List<HoldingConfig.Entry> list = HoldingConfig.load(CONFIG_KEY);
        HoldingConfig.Entry existing = HoldingConfig.findByCode(list, code);
        String cost = existing != null ? existing.cost : "";
        String bonds = existing != null ? existing.bonds : "";
        Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
        HoldingEditDialog dlg = new HoldingEditDialog(owner, "编辑股票", "股票编码", "持仓",
                code, cost, bonds, false);
        dlg.setVisible(true);
        if (!dlg.isApplied()) {
            return;
        }
        HoldingConfig.Entry updated = dlg.getEntry();
        updated.code = code;
        if (existing != null) {
            existing.cost = updated.cost;
            existing.bonds = updated.bonds;
        } else {
            list.add(updated);
        }
        HoldingConfig.save(CONFIG_KEY, HoldingConfig.mergeByCode(list));
        refresh();
    }

    private void deleteSelected() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(this, "请先选中一行", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int modelRow = table.convertRowIndexToModel(viewRow);
        Object codeObj = handler.getValueAt(modelRow, handler.codeColumnIndex);
        if (codeObj == null) {
            return;
        }
        String code = codeObj.toString();
        int ok = JOptionPane.showConfirmDialog(this, "确定删除股票 " + code + " ？", "确认删除",
                JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) {
            return;
        }
        List<HoldingConfig.Entry> list = HoldingConfig.load(CONFIG_KEY);
        list.removeIf(e -> code.equalsIgnoreCase(e.code));
        HoldingConfig.save(CONFIG_KEY, list);
        refresh();
    }

    public void refresh() {
        handler.clearRow();
        String colorStr = Configs.get().getValue("key_colorful");
        boolean colorful = colorStr == null || Boolean.parseBoolean(colorStr);
        handler.refreshColorful(colorful);
        handler.setStriped(Configs.get().getBoolean("key_table_striped"));
        table.setRowSorter(null);
        List<String> codes = HoldingConfig.toCodeLines(HoldingConfig.load(CONFIG_KEY));
        handler.setupTable(codes);
        applyColumnVisibility();
        if (!codes.isEmpty()) {
            handler.handle(codes);
            QuartzManager qm = QuartzManager.getInstance("Stock");
            HashMap<String, Object> dataMap = new HashMap<>();
            dataMap.put(HandlerJob.KEY_HANDLER, handler);
            dataMap.put(HandlerJob.KEY_CODES, codes);
            String cron = Configs.get().getValue("key_cron_expression_stock", "* * * * * ?");
            qm.runJob(HandlerJob.class, cron, dataMap);
        }
        log("refresh done: codes=" + codes.size() + " tableRows=" + table.getRowCount());
    }

    private void applyColumnVisibility() {
        String visibleStr = Configs.get().getValue(VISIBLE_KEY);
        if (visibleStr == null || visibleStr.isEmpty()) return;
        java.util.Set<String> visible = new java.util.LinkedHashSet<>();
        for (String s : visibleStr.split(",")) visible.add(s.trim());
        String[] allCols = getDefaultColumns();
        for (int i = table.getColumnCount() - 1; i >= 0; i--) {
            if (i < allCols.length && !visible.contains(allCols[i])) {
                table.removeColumn(table.getColumnModel().getColumn(i));
            }
        }
    }

    private void log(String msg) {
        System.err.println("Leeks StockPanel: " + msg);
    }

    public void stop() {
        QuartzManager.getInstance("Stock").stopJob();
        handler.stopHandle();
    }
}
