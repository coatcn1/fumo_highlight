package cn.coatcn.bookhighlight;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/**
 * 配置管理：
 * 1）配置文件路径：.minecraft/config/book_highlight/targets_cn.json
 * 2）内容包含：高亮颜色（ARGB 16 进制字符串）与中文附魔名列表（names）
 * 3）首次运行若不存在，则从 resources/book_highlight/targets_cn.json 拷贝默认文件
 */
public class ConfigManager {

    private static final String CONFIG_DIR_NAME = "book_highlight";
    private static final String CONFIG_FILE_NAME = "targets_cn.json";
    private static final String INTERNAL_DEFAULT = "/book_highlight/targets_cn.json";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final ConfigManager INSTANCE = new ConfigManager();

    public static ConfigManager getInstance() {
        return INSTANCE;
    }

    private Set<String> targetNamesCn = new HashSet<>();
    private int highlightColor = 0x80FFD700; // 默认半透明金色
    private Path configPath;
    private long lastModified = 0L;

    private ConfigManager() {}

    public synchronized void loadOrInit() {
        try {
            Path cfgDir = FabricLoader.getInstance().getConfigDir().resolve(CONFIG_DIR_NAME);
            Path cfgFile = cfgDir.resolve(CONFIG_FILE_NAME);
            if (!Files.exists(cfgDir)) {
                Files.createDirectories(cfgDir);
            }
            if (!Files.exists(cfgFile)) {
                // 从 jar 内置默认文件拷贝一份到 config 目录，便于用户修改
                try (InputStream in = getClass().getResourceAsStream(INTERNAL_DEFAULT)) {
                    if (in == null) {
                        throw new IOException("找不到内置默认配置：" + INTERNAL_DEFAULT);
                    }
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                         BufferedWriter bw = Files.newBufferedWriter(cfgFile, StandardCharsets.UTF_8)) {
                        String line;
                        while ((line = br.readLine()) != null) {
                            bw.write(line);
                            bw.newLine();
                        }
                    }
                }
            }
            this.configPath = cfgFile;
            // 读取配置
            try (BufferedReader reader = Files.newBufferedReader(cfgFile, StandardCharsets.UTF_8)) {
                JsonObject obj = GSON.fromJson(reader, JsonObject.class);
                this.highlightColor = parseColor(obj.get("highlightColor").getAsString(), 0x80FFD700);
                this.targetNamesCn.clear();
                if (obj.has("names") && obj.get("names").isJsonArray()) {
                    obj.getAsJsonArray("names").forEach(e -> {
                        String s = e.getAsString();
                        if (s != null && !s.isBlank()) {
                            this.targetNamesCn.add(s.trim());
                        }
                    });
                }
            }
            this.lastModified = Files.getLastModifiedTime(cfgFile).toMillis();
        } catch (Exception e) {
            e.printStackTrace();
            // 如果外部读失败，退回到内置默认
            loadFromInternalDefault();
        }
    }

    private void loadFromInternalDefault() {
        this.targetNamesCn.clear();
        this.highlightColor = 0x80FFD700;
        try (InputStream in = getClass().getResourceAsStream(INTERNAL_DEFAULT)) {
            if (in == null) return;
            try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                JsonObject obj = GSON.fromJson(br, JsonObject.class);
                this.highlightColor = parseColor(obj.get("highlightColor").getAsString(), 0x80FFD700);
                if (obj.has("names") && obj.get("names").isJsonArray()) {
                    obj.getAsJsonArray("names").forEach(e -> {
                        String s = e.getAsString();
                        if (s != null && !s.isBlank()) {
                            this.targetNamesCn.add(s.trim());
                        }
                    });
                }
            }
        } catch (IOException ignored) {}
    }

    private int parseColor(String hex, int fallback) {
        try {
            String h = hex.trim().toLowerCase();
            if (h.startsWith("0x")) {
                h = h.substring(2);
            }
            if (h.startsWith("#")) {
                h = h.substring(1);
            }
            // 允许 ARGB 或 RGBA，不足补齐
            long v = Long.parseUnsignedLong(h, 16);
            if (h.length() <= 6) {
                // 仅 RGB，则补上 alpha 0x80
                v |= 0x80_00_00_00L;
            }
            return (int) v;
        } catch (Exception e) {
            return fallback;
        }
    }

    public Set<String> getTargetNamesCn() {
        return targetNamesCn;
    }

    public int getHighlightColor() {
        return highlightColor;
    }

    // 将当前配置写回并更新修改时间
    public synchronized void saveCurrentToConfig() {
        try {
            Path cfgDir = FabricLoader.getInstance().getConfigDir().resolve(CONFIG_DIR_NAME);
            Path cfgFile = cfgDir.resolve(CONFIG_FILE_NAME);
            if (!Files.exists(cfgDir)) Files.createDirectories(cfgDir);

            JsonObject obj = new JsonObject();
            obj.addProperty("highlightColor", String.format("0x%08X", highlightColor));
            var arr = new com.google.gson.JsonArray();
            for (String s : targetNamesCn) arr.add(s);
            obj.add("names", arr);

            try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(cfgFile), StandardCharsets.UTF_8))) {
                bw.write(GSON.toJson(obj));
            }
            this.configPath = cfgFile;
            this.lastModified = Files.getLastModifiedTime(cfgFile).toMillis();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 检测文件是否被外部修改，若是则重新加载
    public void reloadIfChanged() {
        if (configPath == null) return;
        try {
            long lm = Files.getLastModifiedTime(configPath).toMillis();
            if (lm != lastModified) {
                loadOrInit();
            }
        } catch (IOException ignored) {}
    }

    // 更新目标附魔集合（来自界面），并立即保存
    public void updateTargets(Set<String> newTargets) {
        this.targetNamesCn.clear();
        if (newTargets != null) {
            this.targetNamesCn.addAll(newTargets);
        }
        saveCurrentToConfig();
    }
}
