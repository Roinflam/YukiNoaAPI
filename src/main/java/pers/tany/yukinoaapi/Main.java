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
import pers.tany.yukinoaapi.realizationpart.item.WoolUtil;
import pers.tany.yukinoaapi.realizationpart.listenevent.PlayerEatFood;
import pers.tany.yukinoaapi.util.LangUtil;
import pers.tany.yukinoaapi.util.LogUtil;
import pers.tany.yukinoaapi.util.NMSUtil;

import javax.annotation.Nullable;

/**
 * YukiNoaAPI 主类
 */
public class Main extends JavaPlugin {

    @Nullable
    public static Plugin plugin = null;

    public static YamlConfiguration config;

    @Override
    public void onDisable() {
        LogUtil.warning(LangUtil.get("main.disabled"));
    }

    @Override
    public void onEnable() {
        plugin = this;

        // 创建并加载配置文件
        IConfig.createResource(this, "", "config.yml", false);
        config = IConfig.loadConfig(this, "", "config");

        // 初始化语言系统
        LangUtil.init();

        // 输出启动信息
        LogUtil.success(LangUtil.get("main.enabled"));
        LogUtil.debug(LangUtil.get("main.debug-enabled"));
        LogUtil.debug(LangUtil.get("main.server-version", NMSUtil.getVersion()));
        LogUtil.debug(LangUtil.get("main.current-language", LangUtil.getCurrentLang()));

        if (NMSUtil.isInitialized()) {
            LogUtil.debug(LangUtil.get("main.nms-init-success"));
        } else {
            LogUtil.warning(LangUtil.get("main.nms-init-fail", NMSUtil.getInitError()));
        }

        // 注册命令和事件
        IRegister.registerCommands(this, "YukiNoaAPI", new DebugCommands());
        IRegister.registerEvents(this, new DebugEvent());

        // 初始化工具类
        GlassPaneUtil.getStainedGlass(0);
        WoolUtil.getWool(0);
        PlayerEatFood.getInstance();

        LogUtil.debug(LangUtil.get("main.init-complete"));
    }

    /**
     * 重载配置和语言
     */
    public static void reload() {
        config = IConfig.loadConfig((JavaPlugin) plugin, "", "config");
        LangUtil.reload();
        LogUtil.info(LangUtil.get("main.config-reloaded"));
    }
}