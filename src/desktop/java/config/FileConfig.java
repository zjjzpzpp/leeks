package config;

import utils.ConfigService;
import java.io.*;
import java.util.Properties;

public class FileConfig implements ConfigService {
    private final Properties props = new Properties();
    private final File file;

    public FileConfig(File file) {
        this.file = file;
        System.err.println("Leeks config file: " + file.getAbsolutePath());
        load();
    }

    private void load() {
        if (!file.exists()) {
            System.err.println("Leeks config not found, will create on save");
            return;
        }
        try (InputStream in = new FileInputStream(file)) {
            props.load(in);
        } catch (Exception e) {
            System.err.println("Leeks config load error: " + e.getMessage());
        }
    }

    private void save() {
        try {
            file.getParentFile().mkdirs();
            try (OutputStream out = new FileOutputStream(file)) {
                props.store(out, "leeks config");
            }
        } catch (Exception e) {
            System.err.println("Leeks config save error: " + e.getMessage());
        }
    }

    @Override
    public String getValue(String key) {
        return props.getProperty(key);
    }

    @Override
    public String getValue(String key, String defaultValue) {
        return props.getProperty(key, defaultValue);
    }

    @Override
    public void setValue(String key, String value) {
        if (value == null) {
            props.remove(key);
        } else {
            props.setProperty(key, value);
        }
        save();
    }

    @Override
    public boolean getBoolean(String key) {
        return Boolean.parseBoolean(props.getProperty(key, "false"));
    }

    @Override
    public void setBoolean(String key, boolean value) {
        props.setProperty(key, Boolean.toString(value));
        save();
    }
}
