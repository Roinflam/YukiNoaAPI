package pers.tany.yukinoaapi;

import com.google.common.reflect.ClassPath;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import pers.tany.yukinoaapi.command.DebugCommands;
import pers.tany.yukinoaapi.interfacepart.configuration.IConfig;
import pers.tany.yukinoaapi.interfacepart.register.IRegister;
import pers.tany.yukinoaapi.realizationpart.item.GlassPaneUtil;
import pers.tany.yukinoaapi.realizationpart.item.WoolUtil;
import pers.tany.yukinoaapi.realizationpart.listenevent.PlayerEatFood;

import javax.annotation.Nullable;

/**
 * 主类
 */
public class Main extends JavaPlugin {
    @Nullable
    public static Plugin plugin = null;
    public static YamlConfiguration config;

    @Override
    public void onDisable() {
        Bukkit.getConsoleSender().sendMessage("§7[§fYukiNoaAPI§7]§c已卸载");
    }

    @Override
    public void onEnable() {
        plugin = this;

        Bukkit.getConsoleSender().sendMessage("§7[§fYukiNoaAPI§7]§a已启用");

        IConfig.createResource(this, "", "config.yml", false);

        config = IConfig.loadConfig(this, "", "config");

        IRegister.registerCommands(this, "YukiNoaAPI", new DebugCommands());
        IRegister.registerEvents(this, new DebugEvent());

        GlassPaneUtil.getStainedGlass(0);
        WoolUtil.getWool(0);
//        SkullUtil.getInstance();
        PlayerEatFood.getInstance();

        // 在静态初始化块中或需要的地方调用
//        printAllClasses();  // 第一个版本
    }


    public static void printAllClasses() {
        System.out.println("开始扫描可用的 Minecraft 相关类...");

        // 尝试的基础包路径
        String[] basePaths = {
                "net.minecraft",
                "net.minecraft.server",
                "net.minecraft.world",
                "net.minecraft.world.item",
                "org.bukkit.craftbukkit"
        };

        for (String basePath : basePaths) {
            try {
                System.out.println("\n--- 扫描包: " + basePath + " ---");
                ClassLoader classLoader = NMSUtil.class.getClassLoader();
                ClassPath classPath = ClassPath.from(classLoader);

                for (ClassPath.ClassInfo classInfo : classPath.getTopLevelClasses()) {
                    if (classInfo.getName().startsWith(basePath)) {
                        try {
                            System.out.println("找到类: " + classInfo.getName());
                            // 可选: 尝试加载类以验证其可用性
                            Class.forName(classInfo.getName());
                            System.out.println("  ✓ 类加载成功");
                        } catch (Throwable e) {
                            System.out.println("  × 类加载失败: " + e.getMessage());
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("扫描包 " + basePath + " 时出错: " + e.getMessage());
            }
        }
    }
}
