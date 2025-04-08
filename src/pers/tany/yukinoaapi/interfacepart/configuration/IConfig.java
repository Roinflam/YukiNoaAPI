package pers.tany.yukinoaapi.interfacepart.configuration;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import pers.tany.yukinoaapi.Main;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * 配置文件操作相关
 */
public interface IConfig {
    /**
     * 获取有效的插件数据文件夹
     *
     * @param plugin 插件实例
     * @return 有效的插件数据文件夹
     */
    static File getValidPluginDataFolder(Plugin plugin) {
        // 如果插件是 YukiNoaAPI，直接返回默认数据文件夹
        if ("YukiNoaAPI".equals(plugin.getName())) {
            return plugin.getDataFolder();
        }

        String customDir = Main.config.getString("CustomDataDirectory", "");
        boolean showDefaultPathLogs = Main.config.getBoolean("Logging.ShowDefaultPathLogs", false);
        File baseDir;

        if (customDir != null && !customDir.isEmpty()) {
            baseDir = new File(customDir, plugin.getName());
            if (!baseDir.exists() && !baseDir.mkdirs()) {
                Bukkit.getConsoleSender().sendMessage("§7[§f" + plugin.getName() + "§7] §c自定义数据目录无效或无法创建: " + baseDir.getAbsolutePath());
                Bukkit.getConsoleSender().sendMessage("§7[§f" + plugin.getName() + "§7] §c将使用默认插件数据文件夹");
                baseDir = plugin.getDataFolder();
            } else {
                Bukkit.getConsoleSender().sendMessage("§7[§f" + plugin.getName() + "§7] §a使用自定义数据目录: " + baseDir.getAbsolutePath());
            }
        } else {
            baseDir = plugin.getDataFolder();
            if (showDefaultPathLogs) {
                Bukkit.getConsoleSender().sendMessage("§7[§f" + plugin.getName() + "§7] §a使用默认数据目录: " + baseDir.getAbsolutePath());
            }
        }
        return baseDir;
    }

    /**
     * 在插件文件夹里生成文件夹
     *
     * @param plugin  主类
     * @param dirName 文件夹路径/名
     * @return 是否创建成功
     */
    static boolean createDir(Plugin plugin, String dirName) {
        return createDir(plugin, dirName, true);
    }

    /**
     * 在插件文件夹里生成文件夹
     *
     * @param plugin       主类
     * @param dirName      文件夹路径/名
     * @param subdirectory 是否生成子目录
     * @return 是否创建成功
     */
    static boolean createDir(Plugin plugin, String dirName, boolean subdirectory) {
        try {
            File baseDir = getValidPluginDataFolder(plugin);
            File dir = new File(IFile.formattedPath(baseDir.getCanonicalPath() + "\\" + dirName));
            boolean result = subdirectory ? dir.mkdirs() : dir.mkdir();
            if (!result) {
                Bukkit.getConsoleSender().sendMessage("§7[§f" + plugin.getName() + "§7] §c无法创建目录: " + dir.getAbsolutePath());
            }
            return result;
        } catch (Exception exception) {
            Bukkit.getConsoleSender().sendMessage("§7[§f" + plugin.getName() + "§7] §c创建目录时发生错误: " + exception.getMessage());
            return false;
        }
    }

    /**
     * 从插件里把对应名称的资源创建到对应插件文件夹里
     *
     * @param plugin   主类
     * @param dirName  文件夹名
     * @param fileName 文件名
     * @param cover    是否覆盖
     * @return 是否成功生成
     */
    static boolean createResource(Plugin plugin, String dirName, String fileName, boolean cover) {
        try {
            File baseDir = getValidPluginDataFolder(plugin);
            File dir = new File(IFile.formattedPath(baseDir.getCanonicalPath() + "\\" + dirName));
            dir.mkdirs();
            File file = new File(IFile.formattedPath(dir + "\\" + fileName));
            if (!file.exists() || cover) {
                Files.copy(plugin.getClass().getClassLoader().getResourceAsStream(fileName), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                if (plugin.getName().equals("YukiNoaAPI") || Main.config.getBoolean("Logging.VerboseConfigLogs", false)) {
                    Bukkit.getConsoleSender().sendMessage("§7[§f" + plugin.getName() + "§7] §a成功创建资源文件: " + file.getAbsolutePath());
                }
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            Bukkit.getConsoleSender().sendMessage("§7[§f" + plugin.getName() + "§7] §c创建资源文件时发生错误: " + e.getMessage());
            return false;
        }
        return false;
    }

    /**
     * 读取插件的yml
     *
     * @param plugin   主类
     * @param dirName  文件夹名
     * @param fileName 文件名
     * @return 文件的Configuration状态格式
     */
    @Nullable
    static YamlConfiguration loadConfig(Plugin plugin, String dirName, String fileName) {
        try {
            File baseDir = getValidPluginDataFolder(plugin);
            File configFile = new File(IFile.formattedPath(baseDir.getCanonicalPath() + "\\" + dirName), fileName.endsWith(".yml") ? fileName : fileName + ".yml");
            YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
            if (config.getBoolean("Logging.VerboseConfigLogs", false)) {
                Bukkit.getConsoleSender().sendMessage("§7[§f" + plugin.getName() + "§7] §a成功加载配置文件: " + configFile.getAbsolutePath());
            }
            return config;
        } catch (IOException ioException) {
            Bukkit.getConsoleSender().sendMessage("§7[§f" + plugin.getName() + "§7] §c加载配置文件时发生错误: " + ioException.getMessage());
            return null;
        }
    }

    /**
     * 读取插件的yml
     *
     * @param plugin   主类
     * @param dirName  文件夹名
     * @param fileName 文件名
     * @return 文件的Configuration状态格式
     */
    
    static YamlConfiguration loadSystemConfig(Plugin plugin, String dirName, String fileName) {
        return YamlConfiguration.loadConfiguration(new File(dirName, fileName.endsWith(".yml") ? fileName : fileName + ".yml"));
    }

    /**
     * 保存配置文件
     *
     * @param plugin   主类
     * @param config   配置文件
     * @param dirName  文件夹名称
     * @param fileName 文件名称(无需.yml)
     */
    static void saveSystemConfig(Plugin plugin, FileConfiguration config, String dirName, String fileName) {
        try {
            new File(dirName).mkdirs();
            File file = new File(dirName, fileName.endsWith(".yml") ? fileName : fileName + ".yml");
            try {
                try {
                    config.save(file);
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            } catch (Exception exception) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        try {
                            config.save(file);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }.runTask(plugin);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 保存配置文件
     *
     * @param plugin   主类
     * @param config   配置文件
     * @param dirName  文件夹名称
     * @param fileName 文件名称(无需.yml)
     */
    static void saveConfig(Plugin plugin, FileConfiguration config, String dirName, String fileName) {
        try {
            File baseDir = getValidPluginDataFolder(plugin);
            new File(IFile.formattedPath(baseDir.getCanonicalPath() + "\\" + dirName)).mkdirs();
            File file = new File(IFile.formattedPath(baseDir.getCanonicalPath() + "\\" + dirName), fileName.endsWith(".yml") ? fileName : fileName + ".yml");
            try {
                config.save(file);
                if (Main.config.getBoolean("Logging.VerboseConfigLogs", false)) {
                    Bukkit.getConsoleSender().sendMessage("§7[§f" + plugin.getName() + "§7] §a成功保存配置文件: " + file.getAbsolutePath());
                }
            } catch (IOException ioException) {
                Bukkit.getConsoleSender().sendMessage("§7[§f" + plugin.getName() + "§7] §c保存配置文件时发生错误: " + ioException.getMessage());
            }
        } catch (Exception e) {
            Bukkit.getConsoleSender().sendMessage("§7[§f" + plugin.getName() + "§7] §c保存配置文件时发生未知错误: " + e.getMessage());
        }
    }
}