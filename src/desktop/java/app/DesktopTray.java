package app;

import ui.MainWindow;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class DesktopTray {
    public static void setup(MainWindow window) {
        if (!SystemTray.isSupported()) {
            return;
        }

        try {
            Image image = Toolkit.getDefaultToolkit().createImage(DesktopTray.class.getResource("/icon.png"));
            PopupMenu popup = new PopupMenu();
            MenuItem showItem = new MenuItem("显示主窗口");
            showItem.addActionListener(e -> {
                window.setVisible(true);
                window.setExtendedState(JFrame.NORMAL);
            });
            MenuItem settingsItem = new MenuItem("设置");
            settingsItem.addActionListener(e -> window.openSettings());
            MenuItem exitItem = new MenuItem("退出");
            exitItem.addActionListener(e -> System.exit(0));

            popup.add(showItem);
            popup.add(settingsItem);
            popup.addSeparator();
            popup.add(exitItem);

            TrayIcon trayIcon = new TrayIcon(image, "Leeks", popup);
            trayIcon.setImageAutoSize(true);
            trayIcon.addActionListener(e -> {
                window.setVisible(true);
                window.setExtendedState(JFrame.NORMAL);
            });

            SystemTray.getSystemTray().add(trayIcon);
        } catch (Exception ignored) {
        }
    }
}
