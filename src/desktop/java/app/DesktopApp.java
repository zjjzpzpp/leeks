package app;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import config.FileConfig;
import ui.MainWindow;
import utils.Configs;
import utils.HttpClientPool;

import javax.swing.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;

public class DesktopApp {
    public static void main(String[] args) {
        File jarDir = new File(DesktopApp.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getParentFile();
        File configFile = new File(jarDir.getParentFile(), "leeks.properties");
        File logFile = new File(jarDir.getParentFile(), "leeks.log");
        try {
            System.setErr(new PrintStream(new FileOutputStream(logFile)));
        } catch (Exception ignored) {
        }

        Configs.set(new FileConfig(configFile));
        applyTheme();

        HttpClientPool.getHttpClient();

        System.err.println("=== Leeks desktop start ===");

        SwingUtilities.invokeLater(() -> {
            MainWindow window = new MainWindow();
            window.setVisible(true);
        });
    }

    public static void applyTheme() {
        try {
            if (Configs.get().getBoolean("key_dark_theme")) {
                FlatDarkLaf.setup();
            } else {
                FlatLightLaf.setup();
            }
        } catch (Exception e) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }
        }
    }
}
