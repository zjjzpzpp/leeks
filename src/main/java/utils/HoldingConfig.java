package utils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 持仓配置读写：key_funds / key_stocks，格式 code 或 code,成本,份额/持仓，多条分号分隔。
 */
public final class HoldingConfig {
    private HoldingConfig() {
    }

    public static class Entry {
        public String code = "";
        public String cost = "";
        public String bonds = "";

        public Entry() {
        }

        public Entry(String code, String cost, String bonds) {
            this.code = code == null ? "" : code.trim();
            this.cost = normalizeField(cost);
            this.bonds = normalizeField(bonds);
        }

        public String toConfigLine() {
            if (code.isEmpty()) {
                return "";
            }
            boolean hasCost = cost != null && !cost.isEmpty();
            boolean hasBonds = bonds != null && !bonds.isEmpty();
            if (hasCost || hasBonds) {
                return code + "," + (hasCost ? cost : "") + "," + (hasBonds ? bonds : "");
            }
            return code;
        }

        private static String normalizeField(String s) {
            if (s == null) {
                return "";
            }
            s = s.trim();
            if ("--".equals(s)) {
                return "";
            }
            return s;
        }
    }

    public static List<Entry> load(String configKey) {
        String raw = Configs.get().getValue(configKey, "");
        List<Entry> list = new ArrayList<>();
        if (raw == null || raw.trim().isEmpty()) {
            return list;
        }
        raw = raw.trim();
        if (!raw.contains(";")) {
            list.add(parseOne(raw));
            return list;
        }
        for (String p : raw.split("[;]")) {
            if (p != null && !p.trim().isEmpty()) {
                list.add(parseOne(p.trim()));
            }
        }
        return list;
    }

    public static Entry parseOne(String line) {
        String[] a = line.split(",");
        if (a.length >= 3) {
            return new Entry(a[0].trim(), a[1].trim(), a[2].trim());
        }
        if (a.length == 2) {
            return new Entry(a[0].trim(), a[1].trim(), "");
        }
        return new Entry(line.trim(), "", "");
    }

    public static void save(String configKey, List<Entry> entries) {
        StringBuilder sb = new StringBuilder();
        for (Entry e : entries) {
            String line = e.toConfigLine();
            if (line.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(";");
            }
            sb.append(line);
        }
        Configs.get().setValue(configKey, sb.toString());
    }

    public static List<String> toCodeLines(List<Entry> entries) {
        List<String> lines = new ArrayList<>();
        for (Entry e : entries) {
            String line = e.toConfigLine();
            if (!line.isEmpty()) {
                lines.add(line);
            }
        }
        return lines;
    }

    public static Entry findByCode(List<Entry> entries, String code) {
        if (code == null) {
            return null;
        }
        for (Entry e : entries) {
            if (code.equalsIgnoreCase(e.code)) {
                return e;
            }
        }
        return null;
    }

    public static List<Entry> mergeByCode(List<Entry> entries) {
        Map<String, Entry> map = new LinkedHashMap<>();
        for (Entry e : entries) {
            if (e.code == null || e.code.isEmpty()) {
                continue;
            }
            map.put(e.code.toLowerCase(), e);
        }
        return new ArrayList<>(map.values());
    }
}
