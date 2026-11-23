// 文件：LanguageManager.java
// 路径：src/main/java/pers/tany/yukinoaapi/config/LanguageManager.java
package pers.tany.yukinoaapi.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import pers.tany.yukinoaapi.Main;
import pers.tany.yukinoaapi.interfacepart.configuration.IConfig;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 语言管理器
 * 负责加载和管理多语言文件
 *
 * @author tany
 * @since 3.0.0
 */
public class LanguageManager {

    /** 插件实例 */
    private final Main plugin;

    /** 当前使用的语言代码 */
    private String currentLanguage;

    /** 语言文件配置对象 */
    private FileConfiguration langConfig;

    /** 默认语言 */
    private static final String DEFAULT_LANGUAGE = "en_us";

    /** 支持的语言文件列表（需要手动维护） */
    private static final List<String> SUPPORTED_LANGUAGES = Arrays.asList(
            "en_us",
            "zh_cn"
            // 在这里添加更多支持的语言文件
    );

    /**
     * 构造函数
     *
     * @param plugin 插件实例
     */
    public LanguageManager(Main plugin) {
        this.plugin = plugin;
    }

    /**
     * 加载语言文件
     */
    public void loadLanguage() {
        // 检查并处理配置版本
        handleConfigVersion();

        // 确保所有语言文件都已释放
        ensureAllLanguageFiles();

        // 获取配置的语言
        String configLang = plugin.getConfig().getString("Language", DEFAULT_LANGUAGE);

        // 检测系统语言
        if ("default".equalsIgnoreCase(configLang)) {
            this.currentLanguage = detectSystemLanguage();
            plugin.getServer().getConsoleSender().sendMessage(
                    "§8[§fYukiNoaAPI§8] §7Auto-detected system language: §b" + this.currentLanguage);
        } else {
            this.currentLanguage = configLang.toLowerCase();
        }

        // 加载目标语言文件
        YamlConfiguration targetLangConfig = IConfig.loadConfig(plugin, "lang", currentLanguage);

        if (targetLangConfig == null) {
            plugin.getServer().getConsoleSender().sendMessage(
                    "§e[YukiNoaAPI] Failed to load language file '" + currentLanguage + "', using default");
            this.currentLanguage = DEFAULT_LANGUAGE;
            targetLangConfig = IConfig.loadConfig(plugin, "lang", DEFAULT_LANGUAGE);
        }

        if (targetLangConfig == null) {
            plugin.getServer().getConsoleSender().sendMessage(
                    "§c[YukiNoaAPI] Failed to load language file, using empty config");
            this.langConfig = new YamlConfiguration();
            return;
        }

        this.langConfig = targetLangConfig;

        // 加载默认语言作为后备
        try {
            InputStream defStream = plugin.getResource("lang/" + DEFAULT_LANGUAGE + ".yml");
            if (defStream != null) {
                FileConfiguration defConfig = YamlConfiguration.loadConfiguration(
                        new InputStreamReader(defStream, StandardCharsets.UTF_8));
                defStream.close();
                // 语言文件只在主版本号变化时覆盖，小版本新加的消息先找 jar 里同一种语言的，再退回英文
                InputStream sameStream = DEFAULT_LANGUAGE.equals(currentLanguage)
                        ? null : plugin.getResource("lang/" + currentLanguage + ".yml");
                if (sameStream != null) {
                    YamlConfiguration sameConfig = YamlConfiguration.loadConfiguration(
                            new InputStreamReader(sameStream, StandardCharsets.UTF_8));
                    sameStream.close();
                    sameConfig.setDefaults(defConfig);
                    langConfig.setDefaults(sameConfig);
                } else {
                    langConfig.setDefaults(defConfig);
                }
            }
        } catch (Exception e) {
            plugin.getServer().getConsoleSender().sendMessage(
                    "§e[YukiNoaAPI] Failed to load default language fallback: " + e.getMessage());
        }
    }

    /**
     * 检查并处理配置文件版本
     */
    private void handleConfigVersion() {
        String pluginVersion = plugin.getDescription().getVersion();
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        boolean forceOverwrite = false;

        if (configFile.exists()) {
            YamlConfiguration existingConfig = YamlConfiguration.loadConfiguration(configFile);
            String configVersion = existingConfig.getString("ConfigVersion", "");

            int pluginMajor = extractMajorVersion(pluginVersion);
            int configMajor = extractMajorVersion(configVersion);

            if (pluginMajor != configMajor) {
                forceOverwrite = true;
                plugin.getServer().getConsoleSender().sendMessage(
                        "§e[YukiNoaAPI] Major version changed (" + configVersion + " → " + pluginVersion + "), overwriting config files");
            }
        } else {
            forceOverwrite = true;
        }

        // 创建配置文件
        IConfig.createResource(plugin, "", "config.yml", forceOverwrite);

        if (forceOverwrite) {
            // 强制覆盖所有语言文件
            IConfig.createDir(plugin, "lang");
            for (String langCode : SUPPORTED_LANGUAGES) {
                IConfig.createResource(plugin, "lang", langCode + ".yml", true);
            }

            if (configFile.exists()) {
                plugin.getServer().getConsoleSender().sendMessage(
                        "§a[YukiNoaAPI] Config files updated to new version");
            }
        }
    }

    /**
     * 提取主版本号
     *
     * @param version 版本号字符串
     * @return 主版本号
     */
    private int extractMajorVersion(String version) {
        if (version == null || version.isEmpty()) {
            return -1;
        }

        try {
            String[] parts = version.split("\\.");
            if (parts.length > 0) {
                return Integer.parseInt(parts[0].trim());
            }
        } catch (NumberFormatException e) {
            plugin.getServer().getConsoleSender().sendMessage(
                    "§e[YukiNoaAPI] Unable to parse version: " + version);
        }

        return -1;
    }

    /**
     * 确保所有语言文件都已从jar包中释放
     */
    private void ensureAllLanguageFiles() {
        // 确保 lang 目录存在
        IConfig.createDir(plugin, "lang");

        List<String> createdFiles = new ArrayList<>();
        List<String> skippedFiles = new ArrayList<>();

        for (String langCode : SUPPORTED_LANGUAGES) {
            String fileName = langCode + ".yml";
            File langFile = new File(plugin.getDataFolder(), "lang/" + fileName);

            if (!langFile.exists()) {
                // 尝试从jar包中释放该语言文件
                boolean created = IConfig.createResource(plugin, "lang", fileName, false);

                if (created) {
                    createdFiles.add(fileName);
                } else {
                    skippedFiles.add(fileName);
                }
            }
        }

        // 输出创建结果
        if (!createdFiles.isEmpty()) {
            plugin.getServer().getConsoleSender().sendMessage(
                    "§a[YukiNoaAPI] Created language files: §b" + String.join(", ", createdFiles));
        }

        if (!skippedFiles.isEmpty()) {
            plugin.getServer().getConsoleSender().sendMessage(
                    "§e[YukiNoaAPI] Skipped (not in jar): §7" + String.join(", ", skippedFiles));
        }
    }

    /**
     * 检测系统语言
     *
     * @return 语言代码
     */
    private String detectSystemLanguage() {
        Locale systemLocale = Locale.getDefault();
        String language = systemLocale.getLanguage().toLowerCase();
        String country = systemLocale.getCountry().toLowerCase();
        String langCode = language + "_" + country;

        // 检查语言文件是否存在
        File langFile = new File(plugin.getDataFolder(), "lang/" + langCode + ".yml");
        if (langFile.exists()) {
            return langCode;
        }

        // 尝试常见变体
        String[] commonVariants = {
                language + "_" + language,
                language + "_cn",
                language + "_us",
                language + "_gb"
        };

        for (String variant : commonVariants) {
            File variantFile = new File(plugin.getDataFolder(), "lang/" + variant + ".yml");
            if (variantFile.exists()) {
                return variant;
            }
        }

        return DEFAULT_LANGUAGE;
    }

    /**
     * 获取消息文本
     *
     * @param path 消息路径
     * @return 消息文本
     */
    public String getMessage(String path) {
        if (langConfig == null) {
            return "§c[Language Error] " + path;
        }

        String message = langConfig.getString(path);

        if (message == null) {
            plugin.getServer().getConsoleSender().sendMessage("§e[YukiNoaAPI] Missing message: §7" + path);
            return "§c[Missing: " + path + "]";
        }

        return message;
    }

    /**
     * 获取带数字占位符替换的消息（使用 {0}, {1}, {2} 等）
     *
     * @param path  消息路径
     * @param args  占位符值
     * @return 替换后的消息文本
     */
    public String getMessage(String path, Object... args) {
        String message = getMessage(path);

        if (args != null && args.length > 0) {
            for (int i = 0; i < args.length; i++) {
                if (args[i] != null) {
                    message = message.replace("{" + i + "}", args[i].toString());
                }
            }
        }

        return message;
    }

    /**
     * 获取带占位符替换的消息
     *
     * @param path         消息路径
     * @param placeholders 占位符映射
     * @return 替换后的消息文本
     */
    public String getMessage(String path, Map<String, String> placeholders) {
        String message = getMessage(path);

        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                message = message.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }

        return message;
    }

    /**
     * 获取带单个占位符的消息
     *
     * @param path  消息路径
     * @param key   占位符键
     * @param value 占位符值
     * @return 替换后的消息文本
     */
    public String getMessage(String path, String key, String value) {
        return getMessage(path).replace("{" + key + "}", value);
    }

    /**
     * 获取当前语言代码
     *
     * @return 当前语言代码
     */
    public String getCurrentLanguage() {
        return currentLanguage;
    }

    /**
     * 获取底层配置对象
     *
     * @return FileConfiguration 对象
     */
    public FileConfiguration getConfig() {
        return langConfig;
    }
}