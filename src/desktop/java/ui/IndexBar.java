package ui;

import org.apache.commons.lang.StringUtils;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.Scheduler;
import org.quartz.TriggerBuilder;
import org.quartz.CronScheduleBuilder;
import org.quartz.impl.StdSchedulerFactory;
import utils.Configs;
import utils.HttpClientPool;
import utils.PinYinUtils;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * 顶部固定大盘指数栏。数据源腾讯行情，刷新间隔与股票 Cron 一致。
 */
public class IndexBar extends JPanel {
    public static final String KEY_INDICES = "key_indices";
    public static final String DEFAULT_INDICES = "sh000001;sz399001;sh000300;sz399006;sh000688";

    private final JPanel content;
    private final List<IndexItem> items = new ArrayList<>();
    private Scheduler scheduler;
    private List<String> currentCodes = new ArrayList<>();

    public IndexBar() {
        super(new BorderLayout());
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UIManager.getColor("Component.borderColor")),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        content = new JPanel(new WrapLayout(FlowLayout.LEFT, 16, 4));
        content.setOpaque(false);
        setOpaque(true);
        add(content, BorderLayout.CENTER);
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                content.invalidate();
                revalidate();
            }
        });
    }

    public void refresh() {
        List<String> codes = SettingsDialog.parseCodes(Configs.get().getValue(KEY_INDICES, DEFAULT_INDICES));
        if (codes.isEmpty()) {
            codes = SettingsDialog.parseCodes(DEFAULT_INDICES);
        }
        currentCodes = codes;
        rebuildItems(codes);
        fetchAndUpdate(codes);
        startCron();
    }

    public void stop() {
        stopCron();
    }

    private void rebuildItems(List<String> codes) {
        content.removeAll();
        items.clear();
        for (String code : codes) {
            IndexItem item = new IndexItem(code);
            items.add(item);
            content.add(item);
        }
        content.revalidate();
        content.repaint();
    }

    private void startCron() {
        stopCron();
        if (currentCodes.isEmpty()) {
            return;
        }
        try {
            String cron = Configs.get().getValue("key_cron_expression_stock", "* * * * * ?");
            if (StringUtils.isEmpty(cron)) {
                cron = "* * * * * ?";
            }
            Properties props = new Properties();
            props.put("org.quartz.scheduler.instanceName", "IndexBar");
            props.put("org.quartz.threadPool.threadCount", "1");
            scheduler = new StdSchedulerFactory(props).getScheduler();
            JobDetail detail = JobBuilder.newJob(IndexRefreshJob.class).withIdentity("indexJob").build();
            detail.getJobDataMap().put("bar", this);
            scheduler.scheduleJob(detail, TriggerBuilder.newTrigger()
                    .withSchedule(CronScheduleBuilder.cronSchedule(cron))
                    .build());
            scheduler.start();
        } catch (Exception e) {
            System.err.println("Leeks IndexBar cron error: " + e.getMessage());
        }
    }

    private void stopCron() {
        if (scheduler != null) {
            try {
                scheduler.clear();
                scheduler.shutdown(false);
            } catch (Exception ignored) {
            }
            scheduler = null;
        }
    }

    void fetchAndUpdate(List<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return;
        }
        try {
            String urlPara = String.join(",", codes);
            String result = HttpClientPool.getHttpClient().get("http://qt.gtimg.cn/q=" + urlPara);
            parseAndApply(result);
        } catch (Exception e) {
            System.err.println("Leeks IndexBar fetch error: " + e.getMessage());
        }
    }

    void tick() {
        fetchAndUpdate(currentCodes);
    }

    private void parseAndApply(String result) {
        if (result == null || result.isEmpty()) {
            return;
        }
        String[] lines = result.split("\n");
        for (String line : lines) {
            try {
                if (!line.contains("_") || !line.contains("=")) {
                    continue;
                }
                String code = line.substring(line.indexOf("_") + 1, line.indexOf("="));
                String dataStr = line.substring(line.indexOf("=") + 2, line.length() - 2);
                String[] values = dataStr.split("~");
                if (values.length < 33) {
                    continue;
                }
                String name = values[1];
                String now = values[3];
                String change = values[31];
                String changePercent = values[32];
                for (IndexItem item : items) {
                    if (item.code.equalsIgnoreCase(code)) {
                        item.update(name, now, change, changePercent);
                        break;
                    }
                }
            } catch (Exception ignored) {
            }
        }
        SwingUtilities.invokeLater(() -> {
            content.revalidate();
            content.repaint();
        });
    }

    public static class IndexRefreshJob implements Job {
        @Override
        public void execute(org.quartz.JobExecutionContext context) {
            JobDataMap map = context.getMergedJobDataMap();
            IndexBar bar = (IndexBar) map.get("bar");
            if (bar != null) {
                bar.tick();
            }
        }
    }

    /**
     * 可按容器宽度自动换行的 FlowLayout。
     */
    static class WrapLayout extends FlowLayout {
        WrapLayout(int align, int hgap, int vgap) {
            super(align, hgap, vgap);
        }

        @Override
        public Dimension preferredLayoutSize(Container target) {
            return layoutSize(target, true);
        }

        @Override
        public Dimension minimumLayoutSize(Container target) {
            Dimension minimum = layoutSize(target, false);
            minimum.width -= (getHgap() + 1);
            return minimum;
        }

        private Dimension layoutSize(Container target, boolean preferred) {
            synchronized (target.getTreeLock()) {
                int targetWidth = target.getWidth();
                Container container = target;
                while (container.getSize().width == 0 && container.getParent() != null) {
                    container = container.getParent();
                }
                if (targetWidth == 0) {
                    targetWidth = container.getSize().width;
                }
                if (targetWidth == 0) {
                    targetWidth = Integer.MAX_VALUE;
                }

                int hgap = getHgap();
                int vgap = getVgap();
                Insets insets = target.getInsets();
                int maxWidth = targetWidth - (insets.left + insets.right + hgap * 2);

                int x = 0;
                int y = insets.top + vgap;
                int rowHeight = 0;
                int nmembers = target.getComponentCount();

                for (int i = 0; i < nmembers; i++) {
                    Component m = target.getComponent(i);
                    if (!m.isVisible()) {
                        continue;
                    }
                    Dimension d = preferred ? m.getPreferredSize() : m.getMinimumSize();
                    if (x == 0 || x + d.width <= maxWidth) {
                        if (x > 0) {
                            x += hgap;
                        }
                        x += d.width;
                        rowHeight = Math.max(rowHeight, d.height);
                    } else {
                        x = d.width;
                        y += vgap + rowHeight;
                        rowHeight = d.height;
                    }
                }
                y += rowHeight + vgap + insets.bottom;
                return new Dimension(targetWidth, y);
            }
        }
    }

    private static boolean isColorful() {
        String colorStr = Configs.get().getValue("key_colorful");
        return colorStr == null || Boolean.parseBoolean(colorStr);
    }

    static class IndexItem extends JPanel {
        final String code;
        private final JLabel nameLabel;
        private final JLabel priceLabel;
        private final JLabel changeLabel;
        private String lastName;
        private String lastNow;
        private String lastChange;
        private String lastChangePercent;

        IndexItem(String code) {
            super(new FlowLayout(FlowLayout.LEFT, 6, 0));
            this.code = code;
            setOpaque(false);
            nameLabel = new JLabel(code);
            nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD));
            priceLabel = new JLabel("--");
            changeLabel = new JLabel("--");
            add(nameLabel);
            add(priceLabel);
            add(changeLabel);
        }

        void update(String name, String now, String change, String changePercent) {
            this.lastName = name;
            this.lastNow = now;
            this.lastChange = change;
            this.lastChangePercent = changePercent;
            SwingUtilities.invokeLater(this::render);
        }

        void reapplyMode() {
            SwingUtilities.invokeLater(this::render);
        }

        private void render() {
            boolean colorful = isColorful();
            String displayName = lastName != null && !lastName.isEmpty() ? lastName : code;
            if (!colorful && displayName != null && !displayName.equals(code)) {
                displayName = PinYinUtils.toPinYin(displayName);
            }
            nameLabel.setText(displayName);
            priceLabel.setText(lastNow != null ? lastNow : "--");
            String ch = lastChange != null ? lastChange : "0";
            String pct = lastChangePercent != null ? lastChangePercent : "0";
            if (!pct.startsWith("-") && !pct.startsWith("+")) {
                try {
                    if (new BigDecimal(pct).compareTo(BigDecimal.ZERO) > 0) {
                        pct = "+" + pct;
                    }
                } catch (Exception ignored) {
                }
            }
            if (!ch.startsWith("-") && !ch.startsWith("+")) {
                try {
                    if (new BigDecimal(ch).compareTo(BigDecimal.ZERO) > 0) {
                        ch = "+" + ch;
                    }
                } catch (Exception ignored) {
                }
            }
            if (!colorful) {
                // 隐蔽模式：去掉显眼的 + 前缀
                if (ch.startsWith("+")) {
                    ch = ch.substring(1);
                }
                if (pct.startsWith("+")) {
                    pct = pct.substring(1);
                }
            }
            changeLabel.setText(ch + "  " + pct + "%");
            Color color;
            Color fg = UIManager.getColor("Label.foreground");
            if (fg == null) {
                fg = Color.GRAY;
            }
            if (!colorful) {
                color = fg;
            } else {
                color = fg;
                try {
                    BigDecimal p = new BigDecimal(
                            (lastChangePercent != null ? lastChangePercent : "0").replace("%", "").replace("+", ""));
                    if (p.compareTo(BigDecimal.ZERO) > 0) {
                        color = Color.RED;
                    } else if (p.compareTo(BigDecimal.ZERO) < 0) {
                        color = Color.GREEN;
                    }
                } catch (Exception ignored) {
                }
            }
            nameLabel.setForeground(fg);
            priceLabel.setForeground(color);
            changeLabel.setForeground(color);
        }
    }
}
