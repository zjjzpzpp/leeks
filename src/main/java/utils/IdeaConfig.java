package utils;

import com.intellij.ide.util.PropertiesComponent;

public class IdeaConfig implements ConfigService {
    private final PropertiesComponent pc = PropertiesComponent.getInstance();

    @Override
    public String getValue(String key) {
        return pc.getValue(key);
    }

    @Override
    public String getValue(String key, String defaultValue) {
        return pc.getValue(key, defaultValue);
    }

    @Override
    public void setValue(String key, String value) {
        pc.setValue(key, value);
    }

    @Override
    public boolean getBoolean(String key) {
        return pc.getBoolean(key);
    }

    @Override
    public void setBoolean(String key, boolean value) {
        pc.setValue(key, Boolean.toString(value));
    }
}
