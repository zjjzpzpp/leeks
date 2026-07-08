package utils;

public class Configs {
    private static ConfigService instance;

    public static void set(ConfigService service) {
        instance = service;
    }

    public static ConfigService get() {
        if (instance == null) {
            throw new IllegalStateException("Configs not initialized");
        }
        return instance;
    }
}
