package ui;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.FlatLaf;
import utils.Configs;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

public class MainWindow extends JFrame {
    private IndexBar indexBar;
    private FundPanel fundPanel;
    private StockPanel stockPanel;
    private JCheckBox cbTopmost;
    private JCheckBox cbDark;

    public MainWindow() {
        setTitle("Leeks");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        int w = parseInt(Configs.get().getValue("window_width"), 900);
        int h = parseInt(Configs.get().getValue("window_height"), 500);
        setSize(w, h);
        setLocationRelativeTo(null);

        indexBar = new IndexBar();
        add(indexBar, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        fundPanel = new FundPanel();
        stockPanel = new StockPanel();
        tabs.addTab("Fund", fundPanel);
        tabs.addTab("Stock", stockPanel);
        add(tabs, BorderLayout.CENTER);

        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        cbDark = new JCheckBox("暗黑");
        cbDark.setSelected(Configs.get().getBoolean("key_dark_theme"));
        cbDark.addActionListener(e -> toggleTheme(cbDark.isSelected()));
        cbTopmost = new JCheckBox("置顶");
        cbTopmost.addActionListener(e -> setAlwaysOnTop(cbTopmost.isSelected()));
        JButton btnSettings = new JButton("设置");
        btnSettings.addActionListener(e -> openSettings());
        bottomBar.add(cbDark);
        bottomBar.add(cbTopmost);
        bottomBar.add(btnSettings);
        add(bottomBar, BorderLayout.SOUTH);

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (getWidth() > 200 && getHeight() > 200) {
                    Configs.get().setValue("window_width", String.valueOf(getWidth()));
                    Configs.get().setValue("window_height", String.valueOf(getHeight()));
                }
            }
        });

        loadData();
    }

    private static int parseInt(String s, int def) {
        if (s == null || s.isEmpty()) return def;
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return def; }
    }

    private void toggleTheme(boolean dark) {
        Configs.get().setBoolean("key_dark_theme", dark);
        try {
            if (dark) {
                FlatDarkLaf.setup();
            } else {
                FlatLightLaf.setup();
            }
            FlatLaf.updateUI();
        } catch (Exception ignored) {
        }
    }

    public void openSettings() {
        SettingsDialog dialog = new SettingsDialog(this);
        dialog.setVisible(true);
        if (dialog.isApplied()) {
            indexBar.refresh();
            fundPanel.refresh();
            stockPanel.refresh();
        }
    }

    private void loadData() {
        indexBar.refresh();
        fundPanel.refresh();
        stockPanel.refresh();
    }
}
