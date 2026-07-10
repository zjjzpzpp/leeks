package ui;

import utils.Configs;
import utils.ConfigService;
import utils.HttpClientPool;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class SettingsDialog extends JDialog {
    private JTextArea textAreaFund;
    private JTextArea textAreaStock;
    private JTextArea textAreaIndex;
    private JCheckBox checkboxColorful;
    private JCheckBox checkBoxStriped;
    private JCheckBox checkboxSina;
    private JCheckBox checkboxLog;
    private JTextField cronFund;
    private JTextField cronStock;
    private JTextField inputProxy;
    private boolean applied;

    private static final String CRON_TOOLTIP = "Cron表达式格式：秒 分 时 日 月 星期\n"
            + "例：*/10 * * * * ? 表示每10秒\n"
            + "    * * * * * ?   表示每秒\n"
            + "    0 * * * * ?   表示每分钟";

    private static final String FUND_PLACEHOLDER = "";
    private static final String STOCK_PLACEHOLDER = "";

    public SettingsDialog(Frame owner) {
        super(owner, "设置", true);
        setSize(800, 520);
        setLocationRelativeTo(owner);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Fund", createFundPanel());
        tabs.addTab("Stock", createStockPanel());
        tabs.addTab("Index", createIndexPanel());
        tabs.addTab("通用", createGeneralPanel());

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnSave = new JButton("保存");
        btnSave.addActionListener(e -> {
            save();
            applied = true;
            dispose();
        });
        JButton btnCancel = new JButton("取消");
        btnCancel.addActionListener(e -> dispose());
        btnPanel.add(btnSave);
        btnPanel.add(btnCancel);

        add(tabs, BorderLayout.CENTER);
        add(btnPanel, BorderLayout.SOUTH);

        load();
    }

    public boolean isApplied() {
        return applied;
    }

    private void load() {
        ConfigService cfg = Configs.get();
        textAreaFund.setText(cfg.getValue("key_funds", ""));
        textAreaStock.setText(cfg.getValue("key_stocks", ""));
        String indices = cfg.getValue(IndexBar.KEY_INDICES);
        textAreaIndex.setText(isEmpty(indices) ? IndexBar.DEFAULT_INDICES : indices);
        String colorfulStr = cfg.getValue("key_colorful");
        if (colorfulStr == null) {
            checkboxColorful.setSelected(false);
        } else {
            checkboxColorful.setSelected(!Boolean.parseBoolean(colorfulStr));
        }
        checkBoxStriped.setSelected(cfg.getBoolean("key_table_striped"));
        checkboxSina.setSelected(cfg.getBoolean("key_stocks_sina"));
        checkboxLog.setSelected(cfg.getBoolean("key_close_log"));
        String cronF = cfg.getValue("key_cron_expression_fund", "*/10 * * * * ?");
        cronFund.setText(isEmpty(cronF) ? "*/10 * * * * ?" : cronF);
        String cronS = cfg.getValue("key_cron_expression_stock", "* * * * * ?");
        cronStock.setText(isEmpty(cronS) ? "* * * * * ?" : cronS);
        inputProxy.setText(cfg.getValue("key_proxy", ""));
    }

    private void save() {
        ConfigService cfg = Configs.get();
        cfg.setValue("key_funds", textAreaFund.getText());
        cfg.setValue("key_stocks", textAreaStock.getText());
        String idx = textAreaIndex.getText().trim();
        cfg.setValue(IndexBar.KEY_INDICES, isEmpty(idx) ? IndexBar.DEFAULT_INDICES : idx);
        cfg.setBoolean("key_colorful", !checkboxColorful.isSelected());
        cfg.setBoolean("key_table_striped", checkBoxStriped.isSelected());
        cfg.setBoolean("key_stocks_sina", checkboxSina.isSelected());
        cfg.setBoolean("key_close_log", checkboxLog.isSelected());
        cfg.setValue("key_cron_expression_fund", cronFund.getText());
        cfg.setValue("key_cron_expression_stock", cronStock.getText());
        cfg.setValue("key_proxy", inputProxy.getText().trim());
        HttpClientPool.getHttpClient().buildHttpClient(inputProxy.getText().trim());
    }

    private JPanel createFundPanel() {
        JPanel p = new JPanel(new BorderLayout());
        JPanel top = new JPanel(new BorderLayout());
        top.add(new JLabel("基金编码（分号分隔）："), BorderLayout.NORTH);
        JLabel hint = new JLabel("<html>格式：<b>基金编码,持仓成本价,持有份额</b>，多条用分号分隔。<br>"
                + "例：001632,3.2050,951.64;270042,1.5000,1000;005827<br>"
                + "不填成本价和份额则只显示行情：005827;001632</html>");
        hint.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
        top.add(hint, BorderLayout.SOUTH);
        p.add(top, BorderLayout.NORTH);
        textAreaFund = new JTextArea(FUND_PLACEHOLDER, 10, 40);
        textAreaFund.setLineWrap(true);
        p.add(new JScrollPane(textAreaFund), BorderLayout.CENTER);
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT));
        row.add(new JLabel("刷新间隔："));
        cronFund = new JTextField("*/10 * * * * ?", 15);
        cronFund.setToolTipText(CRON_TOOLTIP);
        row.add(cronFund);
        row.add(new JLabel("（默认每10秒）"));
        p.add(row, BorderLayout.SOUTH);
        return p;
    }

    private JPanel createStockPanel() {
        JPanel p = new JPanel(new BorderLayout());
        JPanel top = new JPanel(new BorderLayout());
        top.add(new JLabel("股票编码（分号分隔）："), BorderLayout.NORTH);
        JLabel hint = new JLabel("<html>格式：<b>股票编码,成本价,持仓数</b>，多条用分号分隔。<br>"
                + "前缀：A股=sh/sz，港股=hk，美股=us<br>"
                + "例：sh600519,1800,100;hk00700,410,200;usAAPL.OQ,180,50</html>");
        hint.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
        top.add(hint, BorderLayout.SOUTH);
        p.add(top, BorderLayout.NORTH);
        textAreaStock = new JTextArea(STOCK_PLACEHOLDER, 10, 40);
        textAreaStock.setLineWrap(true);
        p.add(new JScrollPane(textAreaStock), BorderLayout.CENTER);
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT));
        row.add(new JLabel("刷新间隔："));
        cronStock = new JTextField("* * * * * ?", 15);
        cronStock.setToolTipText(CRON_TOOLTIP);
        row.add(cronStock);
        row.add(new JLabel("（默认每秒；顶部大盘指数与此相同）"));
        p.add(row, BorderLayout.SOUTH);
        return p;
    }

    private JPanel createIndexPanel() {
        JPanel p = new JPanel(new BorderLayout());
        JPanel top = new JPanel(new BorderLayout());
        top.add(new JLabel("大盘指数编码（分号分隔）："), BorderLayout.NORTH);
        JLabel hint = new JLabel("<html>固定显示在窗口顶部，不随表格滚动。<br>"
                + "格式与股票相同，前缀小写。例：sh000001;sz399001;sh000300<br>"
                + "默认：上证、深成、沪深300、创业板指(sz399006)、科创50(sh000688)。<br>"
                + "刷新间隔与 <b>Stock</b> 页的 Cron 一致。</html>");
        hint.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
        top.add(hint, BorderLayout.SOUTH);
        p.add(top, BorderLayout.NORTH);
        textAreaIndex = new JTextArea(IndexBar.DEFAULT_INDICES, 8, 40);
        textAreaIndex.setLineWrap(true);
        p.add(new JScrollPane(textAreaIndex), BorderLayout.CENTER);
        return p;
    }

    private JPanel createGeneralPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        checkboxColorful = new JCheckBox("隐蔽模式（全拼音，无色文本）");
        checkBoxStriped = new JCheckBox("表格条纹");
        checkboxSina = new JCheckBox("新浪股票接口（无港股美股）");
        checkboxLog = new JCheckBox("关闭日志");

        JPanel proxyRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        proxyRow.add(new JLabel("代理:"));
        inputProxy = new JTextField(20);
        inputProxy.setToolTipText("格式：127.0.0.1:1080");
        proxyRow.add(inputProxy);
        JButton btnTest = new JButton("测试");
        btnTest.addActionListener(e -> {
            String proxy = inputProxy.getText().trim();
            proxyTest(proxy);
        });
        proxyRow.add(btnTest);

        p.add(checkboxColorful);
        p.add(checkBoxStriped);
        p.add(checkboxSina);
        p.add(checkboxLog);
        p.add(proxyRow);
        p.add(Box.createVerticalGlue());
        return p;
    }

    private void proxyTest(String proxy) {
        HttpClientPool.getHttpClient().buildHttpClient(proxy);
        try {
            HttpClientPool.getHttpClient().get("https://www.baidu.com");
            JOptionPane.showMessageDialog(this, "代理测试成功！", "提示", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "代理测试失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static boolean isEmpty(String s) {
        return s == null || s.isEmpty();
    }

    static List<String> parseCodes(String value) {
        if (value == null || value.isEmpty()) {
            return new ArrayList<>();
        }
        Set<String> set = new LinkedHashSet<>();
        String[] codes;
        if (value.contains(";")) {
            codes = value.split("[;]");
        } else {
            codes = value.split("[,，]");
        }
        for (String code : codes) {
            if (!code.trim().isEmpty()) {
                set.add(code.trim());
            }
        }
        return new ArrayList<>(set);
    }
}
