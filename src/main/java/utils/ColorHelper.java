package utils;

import java.awt.Color;

/**
 * 彩色方案：IDEA 内用 JBColor（自动适配深色主题），桌面版用标准 Color。
 */
public final class ColorHelper {
    private static final boolean isIdea;

    static {
        boolean idea = false;
        try {
            Class.forName("com.intellij.ui.JBColor");
            idea = true;
        } catch (ClassNotFoundException ignored) {
        }
        isIdea = idea;
    }

    public static Color RED() {
        return isIdea ? colorByName("RED") : new Color(200, 0, 0);
    }

    public static Color GREEN() {
        return isIdea ? colorByName("GREEN") : new Color(0, 150, 0);
    }

    public static Color DARK_GRAY() {
        return isIdea ? colorByName("DARK_GRAY") : Color.DARK_GRAY;
    }

    public static Color GRAY() {
        return isIdea ? colorByName("GRAY") : Color.GRAY;
    }

    private static Color colorByName(String name) {
        try {
            Class<?> jbColor = Class.forName("com.intellij.ui.JBColor");
            java.lang.reflect.Field field = jbColor.getField(name);
            return (Color) field.get(null);
        } catch (Exception e) {
            return Color.GRAY;
        }
    }
}
