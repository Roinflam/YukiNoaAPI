// 文件：Main.java
// 路径：src/main/java/pers/tany/yukinoaapi/Main.java
package pers.tany.yukinoaapi;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import pers.tany.yukinoaapi.command.DebugCommands;
import pers.tany.yukinoaapi.interfacepart.configuration.IConfig;
import pers.tany.yukinoaapi.interfacepart.register.IRegister;
import pers.tany.yukinoaapi.listenevent.DebugEvent;
import pers.tany.yukinoaapi.realizationpart.item.GlassPaneUtil;
import pers.tany.yukinoaapi.realizationpart.item.SkullUtil;
import pers.tany.yukinoaapi.realizationpart.item.WoolUtil;
import pers.tany.yukinoaapi.realizationpart.listenevent.PlayerEatFood;
import pers.tany.yukinoaapi.util.LangUtil;
import pers.tany.yukinoaapi.util.LogUtil;
import pers.tany.yukinoaapi.util.NMSUtil;

import javax.annotation.Nullable;

/**
 * YukiNoaAPI 主类
 * 插件的入口点，负责初始化所有模块和功能
 */
public class Main extends JavaPlugin {

    /**
     * 插件实例
     * 供其他类访问插件功能使用
     */
    @Nullable
    public static Plugin plugin = null;

    /**
     * 主配置文件
     */
    public static YamlConfiguration config;

    /**
     * 插件禁用时调用
     */
    @Override
    public void onDisable() {
        LogUtil.warning(LangUtil.get("main.disabled"));
    }

    /**
     * 插件启用时调用
     * 初始化所有模块和功能
     */
    @Override
    public void onEnable() {
        plugin = this;

        // ==================== 配置文件初始化 ====================
        // 创建并加载配置文件
        IConfig.createResource(this, "", "config.yml", false);
        config = IConfig.loadConfig(this, "", "config");

        // ==================== 语言系统初始化 ====================
        LangUtil.init();

        // ==================== 启动信息输出 ====================
        LogUtil.success(LangUtil.get("main.enabled"));
        LogUtil.debug(LangUtil.get("main.debug-enabled"));
        LogUtil.debug(LangUtil.get("main.server-version", NMSUtil.getVersion()));
        LogUtil.debug(LangUtil.get("main.current-language", LangUtil.getCurrentLang()));

        // NMS 初始化状态检查
        if (NMSUtil.isInitialized()) {
            LogUtil.debug(LangUtil.get("main.nms-init-success"));
        } else {
            LogUtil.warning(LangUtil.get("main.nms-init-fail", NMSUtil.getInitError()));
        }

        // ==================== 命令和事件注册 ====================
        IRegister.registerCommands(this, "YukiNoaAPI", new DebugCommands());
        IRegister.registerEvents(this, new DebugEvent());

        // ==================== 工具类初始化 ====================
        // 预热物品工具类（触发静态初始化块）
        GlassPaneUtil.getStainedGlass(0);
        WoolUtil.getWool(0);

        // 初始化事件监听器
        PlayerEatFood.getInstance();

        // 初始化头颅工具（读取配置，按需注册监听器）
        SkullUtil.getInstance().init();

        LogUtil.debug(LangUtil.get("main.init-complete"));
    }

    /**
     * 重载配置和语言
     * 供管理员命令调用，热重载配置而无需重启服务器
     */
    public static void reload() {
        // 重载主配置
        config = IConfig.loadConfig((JavaPlugin) plugin, "", "config");

        // 重载语言文件
        LangUtil.reload();

        // 清除日志工具缓存（配置可能更改了日志设置）
        LogUtil.invalidateCache();

        // 重载头颅工具配置
        SkullUtil.getInstance().reload();

        LogUtil.info(LangUtil.get("main.config-reloaded"));
    }
}