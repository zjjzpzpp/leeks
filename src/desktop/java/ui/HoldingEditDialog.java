package ui;

import javax.swing.*;
import java.awt.*;

/**
 * 新增/编辑持仓：编码、成本价、份额(基金)或持仓(股票)
 */
public class HoldingEditDialog extends JDialog {
    private final JTextField codeField = new JTextField(16);
    private final JTextField costField = new JTextField(16);
    private final JTextField bondsField = new JTextField(16);
    private boolean applied;
    private final boolean codeEditable;

    /**
     * @param codeLabel   编码标签，如「基金编码」「股票编码」
     * @param bondsLabel  份额/持仓标签
     * @param codeEditable 编辑已有行时编码是否可改
     */
    public HoldingEditDialog(Frame owner, String title, String codeLabel, String bondsLabel,
                             String code, String cost, String bonds, boolean codeEditable) {
        super(owner, title, true);
        this.codeEditable = codeEditable;
        setSize(380, 220);
        setLocationRelativeTo(owner);

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 8, 6, 8);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0;
        c.gridx = 0;
        c.gridy = 0;
        form.add(new JLabel(codeLabel + "："), c);
        c.gridx = 1;
        c.weightx = 1;
        codeField.setText(code == null ? "" : code);
        codeField.setEditable(codeEditable);
        form.add(codeField, c);

        c.gridx = 0;
        c.gridy = 1;
        c.weightx = 0;
        form.add(new JLabel("成本价："), c);
        c.gridx = 1;
        c.weightx = 1;
        costField.setText(emptyIfDash(cost));
        form.add(costField, c);

        c.gridx = 0;
        c.gridy = 2;
        c.weightx = 0;
        form.add(new JLabel(bondsLabel + "："), c);
        c.gridx = 1;
        c.weightx = 1;
        bondsField.setText(emptyIfDash(bonds));
        form.add(bondsField, c);

        JLabel tip = new JLabel("<html><font color=gray>成本价、份额/持仓可留空，仅关注行情</font></html>");
        c.gridx = 0;
        c.gridy = 3;
        c.gridwidth = 2;
        form.add(tip, c);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton ok = new JButton("确定");
        JButton cancel = new JButton("取消");
        ok.addActionListener(e -> {
            if (codeField.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "编码不能为空", "提示", JOptionPane.WARNING_MESSAGE);
                return;
            }
            applied = true;
            dispose();
        });
        cancel.addActionListener(e -> dispose());
        btnRow.add(ok);
        btnRow.add(cancel);

        add(form, BorderLayout.CENTER);
        add(btnRow, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(ok);
    }

    private static String emptyIfDash(String s) {
        if (s == null || "--".equals(s.trim())) {
            return "";
        }
        return s.trim();
    }

    public boolean isApplied() {
        return applied;
    }

    public HoldingConfig.Entry getEntry() {
        return new HoldingConfig.Entry(
                codeField.getText().trim(),
                costField.getText().trim(),
                bondsField.getText().trim());
    }
}
