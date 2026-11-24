package pers.tany.yukinoaapi;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bstats.bukkit.Metrics;
import pers.tany.yukinoaapi.command.DebugCommands;
import pers.tany.yukinoaapi.command.HologramCommands;
import pers.tany.yukinoaapi.config.ConfigManager;
import pers.tany.yukinoaapi.config.LanguageManager;
import pers.tany.yukinoaapi.hologram.HologramManager;
import pers.tany.yukinoaapi.interfacepart.register.IRegister;
import pers.tany.yukinoaapi.interfacepart.player.TempOpLedger;
import pers.tany.yukinoaapi.listenevent.DebugEvent;
import pers.tany.yukinoaapi.realizationpart.item.GlassPaneUtil;
import pers.tany.yukinoaapi.realizationpart.item.SkullUtil;
import pers.tany.yukinoaapi.realizationpart.item.WoolUtil;
import pers.tany.yukinoaapi.realizationpart.listenevent.PlayerEatFood;
import pers.tany.yukinoaapi.util.LogUtil;
import pers.tany.yukinoaapi.util.NMSUtil;

import javax.annotation.Nullable;

/**
 * YukiNoaAPI 主类
 * 插件的入口点,负责初始化所有模块和功能
 *
 * @author tany
 * @since 3.0.0
 */
public class Main extends JavaPlugin {

    /**
     * 插件实例
     */
    @Nullable
    public static Plugin plugin = null;

    /**
     * 主配置文件（保留向后兼容）
     */
    public static YamlConfiguration config;

    /**
     * 配置管理器
     */
    private ConfigManager configManager;

    /**
     * 语言管理器
     */
    private LanguageManager languageManager;

    /** bStats统计对象 */
    private Metrics metrics;

    /** bStats服务ID */
    private static final int BSTATS_PLUGIN_ID = 28590;

    /**
     * 插件禁用时调用
     */
    @Override
    public void onDisable() {
        // 等各插件排队中的异步配置保存写完（依赖本库的插件先于本库卸载，它们最后提交的快照都在队列里）
        pers.tany.yukinoaapi.interfacepart.configuration.IConfig.flushAsyncSaves();

        // 关闭全息图管理器
        if (HologramManager.getInstance() != null) {
            HologramManager.getInstance().shutdown();
        }

        if (languageManager != null) {
            LogUtil.warning(languageManager.getMessage("main.disabled"));
        }
    }

    /**
     * 插件启用时调用
     */
    @Override
    public void onEnable() {
        plugin = this;

        try {
            TempOpLedger.init(this);
        } catch (Throwable e) {
            getLogger().warning("Failed to check temp-op.yml: " + e);
        }

        // ==================== 配置系统初始化 ====================
        configManager = new ConfigManager(this);
        configManager.loadConfig();

        // 保留 config 静态变量用于向后兼容
        config = configManager.getConfig();

        // ==================== 语言系统初始化 ====================
        languageManager = new LanguageManager(this);
        languageManager.loadLanguage();

        // ==================== 启动信息输出 ====================
        LogUtil.success(languageManager.getMessage("main.enabled"));
        LogUtil.debug(languageManager.getMessage("main.server-version", NMSUtil.getVersion()));
        LogUtil.debug(languageManager.getMessage("main.current-language", languageManager.getCurrentLanguage()));

        // NMS 初始化状态检查
        if (NMSUtil.isInitialized()) {
            LogUtil.debug(languageManager.getMessage("main.nms-init-success"));
        } else {
            LogUtil.warning(languageManager.getMessage("main.nms-init-fail",
                    NMSUtil.getInitError() != null ? NMSUtil.getInitError() : "Unknown"));
        }

        // ==================== 命令和事件注册 ====================
        IRegister.registerCommands(this, "YukiNoaAPI", new DebugCommands());
        IRegister.registerEvents(this, new DebugEvent());

        // ==================== 工具类初始化 ====================
        GlassPaneUtil.getGlassPane(0);
        WoolUtil.getWool(0);

        // 初始化头颅工具
        SkullUtil.getInstance().init();

        // 初始化事件监听器
        PlayerEatFood.getInstance();

        // 初始化bStats统计
        metrics = new Metrics(this, BSTATS_PLUGIN_ID);

        // ==================== 全息图系统初始化 ====================
        HologramManager.getInstance();
        IRegister.registerCommands(this, "hologram", new HologramCommands());

        LogUtil.debug(languageManager.getMessage("main.init-complete"));
    }

    /**
     * 重载配置和语言
     */
    public static void reload() {
        Main instance = (Main) plugin;

        // 保存并重载全息图
        if (HologramManager.getInstance() != null) {
            HologramManager.getInstance().saveHolograms();
        }

        // 重载配置
        if (instance.configManager != null) {
            instance.configManager.loadConfig();
            config = instance.configManager.getConfig();
        }

        // 重载语言
        if (instance.languageManager != null) {
            instance.languageManager.loadLanguage();
        }

        // 清除缓存
        LogUtil.invalidateCache();

        LogUtil.info(instance.languageManager.getMessage("main.config-reloaded"));
    }

    /**
     * 获取配置管理器
     */
    public ConfigManager getConfigManager() {
        return configManager;
    }

    /**
     * 获取语言管理器
     */
    public LanguageManager getLanguageManager() {
        return languageManager;
    }
}