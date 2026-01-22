package pers.tany.yukinoaapi.config;

import org.bukkit.configuration.file.YamlConfiguration;
import pers.tany.yukinoaapi.Main;
import pers.tany.yukinoaapi.interfacepart.configuration.IConfig;

import java.io.File;

/**
 * 配置管理器
 * 负责加载、缓存和提供配置数据
 *
 * @author tany
 * @since 3.0.0
 */
public class ConfigManager {

    /** 插件实例 */
    private final Main plugin;

    /** 配置文件对象 */
    private YamlConfiguration config;

    /** 是否启用调试模式 */
    private boolean debugEnabled;

    /** 是否打印堆栈信息 */
    private boolean printStackTrace;

    /** 是否使用 ProtocolLib 序列化物品 */
    private boolean protocolLibItem;

    /** 自定义数据目录 */
    private String customDataDirectory;

    /** MySQL 连接参数 */
    private String mysqlParameters;

    /** 是否启用头颅预加载 */
    private boolean skullPreload;

    /** 是否在退出时清除头颅缓存 */
    private boolean skullClearOnQuit;

    /** 头颅预加载延迟 */
    private int skullPreloadDelay;

    /**
     * 构造函数
     *
     * @param plugin 插件实例
     */
    public ConfigManager(Main plugin) {
        this.plugin = plugin;
    }

    /**
     * 加载配置文件
     */
    public void loadConfig() {
        // 检查并处理配置版本
        handleConfigVersion();

        // 加载配置
        config = IConfig.loadConfig(plugin, "", "config");

        if (config == null) {
            plugin.getServer().getConsoleSender().sendMessage("§c[YukiNoaAPI] Failed to load config.yml");
            config = new YamlConfiguration();
            return;
        }

        // 加载配置项到缓存
        loadSettings();

        // 验证配置
        validateConfig();
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

        if (forceOverwrite && configFile.exists()) {
            plugin.getServer().getConsoleSender().sendMessage(
                    "§a[YukiNoaAPI] Config files updated to new version");
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
     * 加载配置项到内存缓存
     */
    private void loadSettings() {
        // 日志设置
        this.debugEnabled = config.getBoolean("Logging.Debug", false);
        this.printStackTrace = config.getBoolean("Logging.PrintStackTrace", true);

        // 物品序列化
        this.protocolLibItem = config.getBoolean("ProtocolLibItem", true);

        // 数据目录
        this.customDataDirectory = config.getString("CustomDataDirectory", "");

        // 数据库设置
        this.mysqlParameters = config.getString("Database.MySQL.Parameters",
                "useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=utf8");

        // 头颅设置
        this.skullPreload = config.getBoolean("Skull.Preload", false);
        this.skullClearOnQuit = config.getBoolean("Skull.ClearOnQuit", false);
        this.skullPreloadDelay = config.getInt("Skull.PreloadDelay", 20);
    }

    /**
     * 验证配置的有效性
     */
    private void validateConfig() {
        // 验证头颅预加载延迟
        if (skullPreloadDelay < 1) {
            plugin.getServer().getConsoleSender().sendMessage(
                    "§e[YukiNoaAPI] " +
                            (plugin.getLanguageManager() != null ?
                                    plugin.getLanguageManager().getMessage("config.invalid-skull-delay") :
                                    "Invalid skull preload delay, adjusted to 1"));
            this.skullPreloadDelay = 1;
        }

        // 验证 MySQL 参数
        if (mysqlParameters == null || mysqlParameters.trim().isEmpty()) {
            plugin.getServer().getConsoleSender().sendMessage(
                    "§e[YukiNoaAPI] MySQL parameters empty, using default");
            this.mysqlParameters = "useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=utf8";
        }
    }

    // ==================== Getter 方法 ====================

    public boolean isDebugEnabled() {
        return debugEnabled;
    }

    public boolean isPrintStackTrace() {
        return printStackTrace;
    }

    public boolean isProtocolLibItem() {
        return protocolLibItem;
    }

    public String getCustomDataDirectory() {
        return customDataDirectory;
    }

    public String getMysqlParameters() {
        return mysqlParameters;
    }

    public boolean isSkullPreload() {
        return skullPreload;
    }

    public boolean isSkullClearOnQuit() {
        return skullClearOnQuit;
    }

    public int getSkullPreloadDelay() {
        return skullPreloadDelay;
    }

    public YamlConfiguration getConfig() {
        return config;
    }
}