package utils;

public interface ConfigService {
    String getValue(String key);
    String getValue(String key, String defaultValue);
    void setValue(String key, String value);
    boolean getBoolean(String key);
    void setBoolean(String key, boolean value);
}
