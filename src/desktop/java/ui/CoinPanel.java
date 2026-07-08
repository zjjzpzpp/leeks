package ui;

import handler.YahooCoinHandler;
import handler.CoinRefreshHandler;
import quartz.HandlerJob;
import quartz.QuartzManager;
import utils.Configs;
import utils.WindowUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.HashMap;
import java.util.List;

public class CoinPanel extends JPanel {
    private final JTable table;
    private final JLabel refreshTimeLabel;
    private final CoinRefreshHandler handler;
    private final JButton btnRefresh;
    private final JButton btnStop;

    public CoinPanel() {
        super(new BorderLayout());
        refreshTimeLabel = new JLabel();
        refreshTimeLabel.setBorder(new EmptyBorder(0, 5, 0, 5));

        table = new JTable();
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.getTableHeader().addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseDragged(java.awt.event.MouseEvent e) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < table.getColumnCount(); i++) {
                    sb.append(table.getColumnName(i)).append(",");
                }
                Configs.get().setValue(WindowUtils.COIN_TABLE_HEADER_KEY,
                        sb.substring(0, sb.length() > 0 ? sb.length() - 1 : 0));
            }
        });

        handler = new YahooCoinHandler(table, refreshTimeLabel);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        btnRefresh = new JButton("刷新");
        btnRefresh.addActionListener(e -> refresh());
        btnStop = new JButton("停止");
        btnStop.addActionListener(e -> stop());
        toolbar.add(btnRefresh);
        toolbar.add(btnStop);
        toolbar.add(Box.createHorizontalGlue());
        toolbar.add(refreshTimeLabel);

        add(toolbar, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    public void refresh() {
        handler.clearRow();
        String colorStr = Configs.get().getValue("key_colorful");
        boolean colorful = colorStr == null || Boolean.parseBoolean(colorStr);
        handler.refreshColorful(colorful);
        handler.setStriped(Configs.get().getBoolean("key_table_striped"));
        List<String> codes = getCoinCodes();
        handler.setupTable(codes);
        if (!codes.isEmpty()) {
            handler.handle(codes);
            QuartzManager qm = QuartzManager.getInstance("Coin");
            HashMap<String, Object> dataMap = new HashMap<>();
            dataMap.put(HandlerJob.KEY_HANDLER, handler);
            dataMap.put(HandlerJob.KEY_CODES, codes);
            String cron = Configs.get().getValue("key_cron_expression_coin", "*/10 * * * * ?");
            qm.runJob(HandlerJob.class, cron, dataMap);
        }
    }

    public void stop() {
        QuartzManager.getInstance("Coin").stopJob();
        handler.stopHandle();
    }

    private List<String> getCoinCodes() {
        return SettingsDialog.parseCodes(Configs.get().getValue("key_coins"));
    }
}
