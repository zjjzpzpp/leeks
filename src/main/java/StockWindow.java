import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionToolbarPosition;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.table.JBTable;
import handler.SinaStockHandler;
import handler.StockRefreshHandler;
import handler.TencentStockHandler;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import quartz.HandlerJob;
import quartz.QuartzManager;
import utils.Configs;
import utils.ConfigService;
import utils.HoldingConfig;
import utils.HoldingEditDialog;
import utils.LogUtil;
import utils.PopupsUiUtil;
import utils.TableRowDragSupport;
import utils.WindowUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.net.MalformedURLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class StockWindow {
    public static final String NAME = "Stock";
    private JPanel mPanel;

    static StockRefreshHandler handler;

    static JBTable table;
    static JLabel refreshTimeLabel;

    public JPanel getmPanel() {
        return mPanel;
    }

    static {
        refreshTimeLabel = new JLabel();
        refreshTimeLabel.setToolTipText("最后刷新时间");
        refreshTimeLabel.setBorder(new EmptyBorder(0, 0, 0, 5));
        table = new JBTable();
        //记录列名的变化
        table.getTableHeader().addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                StringBuilder tableHeadChange = new StringBuilder();
                for (int i = 0; i < table.getColumnCount(); i++) {
                    tableHeadChange.append(table.getColumnName(i)).append(",");
                }
                ConfigService instance = Configs.get();
                //将列名的修改放入环境中 key:stock_table_header_key
                instance.setValue(WindowUtils.STOCK_TABLE_HEADER_KEY, tableHeadChange
                        .substring(0, tableHeadChange.length() > 0 ? tableHeadChange.length() - 1 : 0));

                //LogUtil.info(instance.getValue(WindowUtils.STOCK_TABLE_HEADER_KEY));
            }

        });
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (table.getSelectedRow() < 0)
                    return;
                String code = String.valueOf(table.getModel().getValueAt(table.convertRowIndexToModel(table.getSelectedRow()), handler.codeColumnIndex));//FIX 移动列导致的BUG
                if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() > 1) {
                    // 双击编辑持仓
                    editStockRow();
                } else if (SwingUtilities.isRightMouseButton(e)) {
                    //鼠标右键
                    JBPopupFactory.getInstance().createListPopup(new BaseListPopupStep<PopupsUiUtil.StockShowType>("",
                            PopupsUiUtil.StockShowType.values()) {
                        @Override
                        public @NotNull String getTextFor(PopupsUiUtil.StockShowType value) {
                            return value.getDesc();
                        }

                        @Override
                        public @Nullable PopupStep onChosen(PopupsUiUtil.StockShowType selectedValue, boolean finalChoice) {
                            try {
                                PopupsUiUtil.showImageByStockCode(code, selectedValue, new Point(e.getXOnScreen(), e.getYOnScreen()));
                            } catch (MalformedURLException ex) {
                                ex.printStackTrace();
                                LogUtil.info(ex.getMessage());
                            }
                            return super.onChosen(selectedValue, finalChoice);
                        }
                    }).show(RelativePoint.fromScreen(new Point(e.getXOnScreen(), e.getYOnScreen())));
                }
            }
        });
        table.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                table.clearSelection();
            }
        });
    }

    public StockWindow() {

        mPanel = new JPanel(new BorderLayout(0, 0));
        //切换接口
        handler = factoryHandler();
        table.setRowSorter(null);
        TableRowDragSupport.enable(table, handler.codeColumnIndex, codes ->
                TableRowDragSupport.persistOrder("key_stocks", codes));

        AnActionButton refreshAction = new AnActionButton("停止刷新当前表格数据", AllIcons.Actions.Pause) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                stop();
                this.setEnabled(false);
            }
        };
        ToolbarDecorator toolbarDecorator = ToolbarDecorator.createDecorator(table)
                .addExtraAction(new AnActionButton("持续刷新当前表格数据", AllIcons.Actions.Refresh) {
                    @Override
                    public void actionPerformed(@NotNull AnActionEvent e) {
                        refresh();
                        refreshAction.setEnabled(true);
                    }
                })
                .addExtraAction(refreshAction)
                .addExtraAction(new AnActionButton("新增股票", AllIcons.General.Add) {
                    @Override
                    public void actionPerformed(@NotNull AnActionEvent e) {
                        addStockRow();
                    }
                })
                .addExtraAction(new AnActionButton("编辑持仓", AllIcons.Actions.Edit) {
                    @Override
                    public void actionPerformed(@NotNull AnActionEvent e) {
                        editStockRow();
                    }
                })
                .addExtraAction(new AnActionButton("删除股票", AllIcons.General.Remove) {
                    @Override
                    public void actionPerformed(@NotNull AnActionEvent e) {
                        deleteStockRow();
                    }
                })
                .setToolbarPosition(ActionToolbarPosition.TOP);
        JPanel toolPanel = toolbarDecorator.createPanel();
        toolbarDecorator.getActionsPanel().add(refreshTimeLabel, BorderLayout.EAST);
        toolPanel.setBorder(new EmptyBorder(0,0,0,0));
        mPanel.add(toolPanel, BorderLayout.CENTER);
        // 非主要tab，需要创建，创建时立即应用数据
        apply();
    }

    private static StockRefreshHandler factoryHandler(){
        boolean useSinaApi = Configs.get().getBoolean("key_stocks_sina");
        if (useSinaApi){
            if (handler instanceof SinaStockHandler){
                return handler;
            }
            return new SinaStockHandler(table, refreshTimeLabel);
        }
        if (handler instanceof TencentStockHandler){
            return handler;
        }
        return  new TencentStockHandler(table, refreshTimeLabel);
    }

    public static void apply() {
        if (handler != null) {
            handler = factoryHandler();
            ConfigService instance = Configs.get();
            handler.setStriped(instance.getBoolean("key_table_striped"));
            handler.clearRow();
            handler.setupTable(loadStocks());
            refresh();
        }
    }
    public static void refresh() {
        if (handler != null) {
            ConfigService instance = Configs.get();
            handler.refreshColorful(instance.getBoolean("key_colorful"));
            table.setRowSorter(null);
            List<String> codes = loadStocks();
            if (CollectionUtils.isEmpty(codes)) {
                stop(); //如果没有数据则不需要启动时钟任务浪费资源
            } else {
                handler.handle(codes);
                QuartzManager quartzManager = QuartzManager.getInstance(NAME);
                HashMap<String, Object> dataMap = new HashMap<>();
                dataMap.put(HandlerJob.KEY_HANDLER, handler);
                dataMap.put(HandlerJob.KEY_CODES, codes);
                String cronExpression = instance.getValue("key_cron_expression_stock");
                if (StringUtils.isEmpty(cronExpression)) {
                    cronExpression = "*/10 * * * * ?";
                }
                quartzManager.runJob(HandlerJob.class, cronExpression, dataMap);
            }
        }
    }

    public static void stop() {
        QuartzManager.getInstance(NAME).stopJob();
        if (handler != null) {
            handler.stopHandle();
        }
    }

    private static List<String> loadStocks(){
        return HoldingConfig.toCodeLines(HoldingConfig.load("key_stocks"));
    }

    private static Frame ownerFrame() {
        Window w = SwingUtilities.getWindowAncestor(table);
        return w instanceof Frame ? (Frame) w : null;
    }

    private static void addStockRow() {
        HoldingEditDialog dlg = new HoldingEditDialog(ownerFrame(), "新增股票", "股票编码", "持仓",
                "", "", "", true);
        dlg.setVisible(true);
        if (!dlg.isApplied()) {
            return;
        }
        HoldingConfig.Entry entry = dlg.getEntry();
        List<HoldingConfig.Entry> list = HoldingConfig.load("key_stocks");
        if (HoldingConfig.findByCode(list, entry.code) != null) {
            JOptionPane.showMessageDialog(table, "编码已存在：" + entry.code, "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        list.add(entry);
        HoldingConfig.save("key_stocks", HoldingConfig.mergeByCode(list));
        apply();
    }

    private static void editStockRow() {
        if (handler == null || table.getSelectedRow() < 0) {
            JOptionPane.showMessageDialog(table, "请先选中一行", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int modelRow = table.convertRowIndexToModel(table.getSelectedRow());
        Object codeObj = handler.getValueAt(modelRow, handler.codeColumnIndex);
        if (codeObj == null) {
            return;
        }
        String code = codeObj.toString();
        List<HoldingConfig.Entry> list = HoldingConfig.load("key_stocks");
        HoldingConfig.Entry existing = HoldingConfig.findByCode(list, code);
        String cost = existing != null ? existing.cost : "";
        String bonds = existing != null ? existing.bonds : "";
        HoldingEditDialog dlg = new HoldingEditDialog(ownerFrame(), "编辑股票", "股票编码", "持仓",
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
        HoldingConfig.save("key_stocks", HoldingConfig.mergeByCode(list));
        apply();
    }

    private static void deleteStockRow() {
        if (handler == null || table.getSelectedRow() < 0) {
            JOptionPane.showMessageDialog(table, "请先选中一行", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int modelRow = table.convertRowIndexToModel(table.getSelectedRow());
        Object codeObj = handler.getValueAt(modelRow, handler.codeColumnIndex);
        if (codeObj == null) {
            return;
        }
        String code = codeObj.toString();
        int ok = JOptionPane.showConfirmDialog(table, "确定删除股票 " + code + " ？", "确认删除",
                JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) {
            return;
        }
        List<HoldingConfig.Entry> list = HoldingConfig.load("key_stocks");
        list.removeIf(e -> code.equalsIgnoreCase(e.code));
        HoldingConfig.save("key_stocks", list);
        apply();
    }

}
