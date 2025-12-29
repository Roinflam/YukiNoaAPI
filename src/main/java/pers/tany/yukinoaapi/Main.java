package pers.tany.yukinoaapi;

import com.google.common.reflect.ClassPath;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import pers.tany.yukinoaapi.command.DebugCommands;
import pers.tany.yukinoaapi.interfacepart.configuration.IConfig;
import pers.tany.yukinoaapi.interfacepart.register.IRegister;
import pers.tany.yukinoaapi.realizationpart.item.GlassPaneUtil;
import pers.tany.yukinoaapi.realizationpart.item.WoolUtil;
import pers.tany.yukinoaapi.realizationpart.listenevent.PlayerEatFood;
import pers.tany.yukinoaapi.util.LangUtil;
import pers.tany.yukinoaapi.util.LogUtil;

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
        LogUtil.warning("已卸载");
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
        LogUtil.success("已启用");
        LogUtil.debug("调试模式已开启");
        LogUtil.debug("服务器版本: " + NMSUtil.getVersion());
        LogUtil.debug("当前语言: " + LangUtil.getCurrentLang());

        if (NMSUtil.isInitialized()) {
            LogUtil.debug("NMS反射初始化成功");
        } else {
            LogUtil.warning("NMS反射初始化失败: " + NMSUtil.getInitError());
        }

        // 注册命令和事件
        IRegister.registerCommands(this, "YukiNoaAPI", new DebugCommands());
        IRegister.registerEvents(this, new DebugEvent());

        // 初始化工具类
        GlassPaneUtil.getStainedGlass(0);
        WoolUtil.getWool(0);
        PlayerEatFood.getInstance();

        LogUtil.debug("插件初始化完成");
    }

    /**
     * 重载配置和语言
     */
    public static void reload() {
        config = IConfig.loadConfig((JavaPlugin) plugin, "", "config");
        LangUtil.reload();
        LogUtil.info("配置已重载");
    }

    /**
     * 打印所有可用的Minecraft相关类
     */
    public static void printAllClasses() {
        LogUtil.info("开始扫描可用的 Minecraft 相关类...");

        String[] basePaths = {
                "net.minecraft",
                "net.minecraft.server",
                "net.minecraft.world",
                "net.minecraft.world.item",
                "org.bukkit.craftbukkit"
        };

        for (String basePath : basePaths) {
            try {
                LogUtil.info("--- 扫描包: " + basePath + " ---");
                ClassLoader classLoader = NMSUtil.class.getClassLoader();
                ClassPath classPath = ClassPath.from(classLoader);

                for (ClassPath.ClassInfo classInfo : classPath.getTopLevelClasses()) {
                    if (classInfo.getName().startsWith(basePath)) {
                        try {
                            LogUtil.debug("找到类: " + classInfo.getName());
                            Class.forName(classInfo.getName());
                            LogUtil.debug("  ✓ 类加载成功");
                        } catch (Throwable e) {
                            LogUtil.debug("  × 类加载失败: " + e.getMessage());
                        }
                    }
                }
            } catch (Exception e) {
                LogUtil.error("扫描包 " + basePath + " 时出错", e);
            }
        }
    }
}