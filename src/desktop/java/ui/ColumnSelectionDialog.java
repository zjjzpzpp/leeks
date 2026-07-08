package ui;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ColumnSelectionDialog extends JDialog {
    private final List<JCheckBox> checkboxes = new ArrayList<>();
    private boolean applied;

    public ColumnSelectionDialog(Frame owner, String title, String[] availableColumns, String selectedStr) {
        super(owner, title, true);
        setSize(300, 400);
        setLocationRelativeTo(owner);

        Set<String> selected = new LinkedHashSet<>();
        if (selectedStr != null && !selectedStr.isEmpty()) {
            for (String s : selectedStr.split(",")) {
                selected.add(s.trim());
            }
        }

        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        JScrollPane scroll = new JScrollPane(listPanel);
        scroll.setBorder(BorderFactory.createEmptyBorder());

        for (String col : availableColumns) {
            JCheckBox cb = new JCheckBox(col);
            cb.setSelected(selected.isEmpty() || selected.contains(col));
            checkboxes.add(cb);
            listPanel.add(cb);
        }

        add(scroll, BorderLayout.CENTER);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnAll = new JButton("全选");
        btnAll.addActionListener(e -> {
            for (JCheckBox cb : checkboxes) cb.setSelected(true);
        });
        JButton btnNone = new JButton("全不选");
        btnNone.addActionListener(e -> {
            for (JCheckBox cb : checkboxes) cb.setSelected(false);
        });
        JButton btnOk = new JButton("确定");
        btnOk.addActionListener(e -> {
            applied = true;
            dispose();
        });
        JButton btnCancel = new JButton("取消");
        btnCancel.addActionListener(e -> dispose());
        btnRow.add(btnAll);
        btnRow.add(btnNone);
        btnRow.add(btnOk);
        btnRow.add(btnCancel);
        add(btnRow, BorderLayout.SOUTH);
    }

    public boolean isApplied() {
        return applied;
    }

    public String getSelectedColumns() {
        StringBuilder sb = new StringBuilder();
        for (JCheckBox cb : checkboxes) {
            if (cb.isSelected()) {
                if (sb.length() > 0) sb.append(",");
                sb.append(cb.getText());
            }
        }
        return sb.toString();
    }
}
