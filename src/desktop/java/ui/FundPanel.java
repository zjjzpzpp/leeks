package ui;

import handler.TianTianFundHandler;
import handler.FundRefreshHandler;
import quartz.HandlerJob;
import quartz.QuartzManager;
import utils.Configs;
import utils.WindowUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.HashMap;
import java.util.List;

public class FundPanel extends JPanel {
    private static final String VISIBLE_KEY = "fund_visible_columns";
    private final JTable table;
    private final JLabel refreshTimeLabel;
    private final TianTianFundHandler handler;
    private final JButton btnRefresh;
    private final JButton btnStop;
    private final JButton btnColumns;

    public FundPanel() {
        super(new BorderLayout());
        refreshTimeLabel = new JLabel();
        refreshTimeLabel.setBorder(new EmptyBorder(0, 5, 0, 5));

        table = new JTable();
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        handler = new TianTianFundHandler(table, refreshTimeLabel);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        btnRefresh = new JButton("刷新");
        btnRefresh.addActionListener(e -> refresh());
        btnStop = new JButton("停止");
        btnStop.addActionListener(e -> stop());
        btnColumns = new JButton("列设置");
        btnColumns.addActionListener(e -> showColumnDialog());
        toolbar.add(btnRefresh);
        toolbar.add(btnStop);
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
                (Frame) SwingUtilities.getWindowAncestor(this), "基金列设置", allCols, visibleStr);
        dlg.setVisible(true);
        if (dlg.isApplied()) {
            Configs.get().setValue(VISIBLE_KEY, dlg.getSelectedColumns());
            rebuildTableColumns();
        }
    }

    private String[] getDefaultColumns() {
        return WindowUtils.FUND_TABLE_HEADER_VALUE.split(",");
    }

    public void refresh() {
        handler.clearRow();
        String colorStr = Configs.get().getValue("key_colorful");
        boolean colorful = colorStr == null || Boolean.parseBoolean(colorStr);
        handler.refreshColorful(colorful);
        handler.setStriped(Configs.get().getBoolean("key_table_striped"));
        List<String> codes = getFundCodes();
        handler.setupTable(codes);
        applyColumnVisibility();
        if (!codes.isEmpty()) {
            handler.handle(codes);
            QuartzManager qm = QuartzManager.getInstance("Fund");
            HashMap<String, Object> dataMap = new HashMap<>();
            dataMap.put(HandlerJob.KEY_HANDLER, handler);
            dataMap.put(HandlerJob.KEY_CODES, codes);
            String cron = Configs.get().getValue("key_cron_expression_fund", "*/10 * * * * ?");
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
            if (!visible.contains(allCols[i])) {
                table.removeColumn(table.getColumnModel().getColumn(i));
            }
        }
    }

    private void log(String msg) {
        System.err.println("Leeks FundPanel: " + msg);
    }

    public void stop() {
        QuartzManager.getInstance("Fund").stopJob();
        handler.stopHandle();
    }

    private List<String> getFundCodes() {
        return SettingsDialog.parseCodes(Configs.get().getValue("key_funds"));
    }
}
