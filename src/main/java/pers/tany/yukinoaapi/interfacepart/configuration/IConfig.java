// 文件：IConfig.java
// 路径：src/pers/tany/yukinoaapi/interfacepart/configuration/IConfig.java
package pers.tany.yukinoaapi.interfacepart.configuration;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import pers.tany.yukinoaapi.Main;
import pers.tany.yukinoaapi.util.LogUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * 配置文件操作相关接口
 * 提供配置文件的创建、读取、保存等功能
 *
 * <p>使用示例：</p>
 * <pre>
 * // 创建配置文件
 * IConfig.createResource(plugin, "", "config.yml", false);
 *
 * // 加载配置文件
 * YamlConfiguration config = IConfig.loadConfig(plugin, "", "config");
 *
 * // 保存配置文件
 * IConfig.saveConfig(plugin, config, "", "config");
 * </pre>
 *
 * @author tany
 * @since 1.0.0
 */
public interface IConfig {

    /**
     * 获取有效的插件数据文件夹
     *
     * <p>根据配置文件中的 CustomDataDirectory 设置决定使用哪个目录：</p>
     * <ul>
     *     <li>如果插件是 YukiNoaAPI，直接返回默认数据文件夹</li>
     *     <li>如果设置了自定义目录且有效，使用自定义目录</li>
     *     <li>否则使用默认的 plugins/插件名 目录</li>
     * </ul>
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
                LogUtil.error(plugin.getName(), "自定义数据目录无效或无法创建: " + baseDir.getAbsolutePath());
                LogUtil.warning(plugin.getName(), "将使用默认插件数据文件夹");
                baseDir = plugin.getDataFolder();
            } else {
                LogUtil.success(plugin.getName(), "使用自定义数据目录: " + baseDir.getAbsolutePath());
            }
        } else {
            baseDir = plugin.getDataFolder();
            if (showDefaultPathLogs) {
                LogUtil.success(plugin.getName(), "使用默认数据目录: " + baseDir.getAbsolutePath());
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
            if (!result && !dir.exists()) {
                LogUtil.error(plugin.getName(), "无法创建目录: " + dir.getAbsolutePath());
            } else {
                LogUtil.debug(plugin.getName(), "目录创建成功: " + dir.getAbsolutePath());
            }
            return result || dir.exists();
        } catch (Exception e) {
            LogUtil.error(plugin.getName(), "创建目录时发生错误", e);
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
                // 检查资源是否存在
                if (plugin.getClass().getClassLoader().getResourceAsStream(fileName) == null) {
                    LogUtil.error(plugin.getName(), "资源文件不存在: " + fileName);
                    return false;
                }

                Files.copy(
                        plugin.getClass().getClassLoader().getResourceAsStream(fileName),
                        file.toPath(),
                        StandardCopyOption.REPLACE_EXISTING
                );

                // 根据配置决定是否输出日志
                if (plugin.getName().equals("YukiNoaAPI") || Main.config.getBoolean("Logging.VerboseConfigLogs", false)) {
                    LogUtil.success(plugin.getName(), "成功创建资源文件: " + file.getAbsolutePath());
                }
                LogUtil.debug(plugin.getName(), "资源文件已创建: " + file.getName());
                return true;
            } else {
                LogUtil.debug(plugin.getName(), "资源文件已存在，跳过创建: " + file.getName());
            }
        } catch (Exception e) {
            LogUtil.error(plugin.getName(), "创建资源文件时发生错误: " + fileName, e);
            return false;
        }
        return false;
    }

    /**
     * 读取插件的yml配置文件
     *
     * @param plugin   主类
     * @param dirName  文件夹名
     * @param fileName 文件名（可带或不带.yml后缀）
     * @return 文件的YamlConfiguration实例，加载失败返回null
     */
    @Nullable
    static YamlConfiguration loadConfig(Plugin plugin, String dirName, String fileName) {
        try {
            File baseDir = getValidPluginDataFolder(plugin);
            File configFile = new File(
                    IFile.formattedPath(baseDir.getCanonicalPath() + "\\" + dirName),
                    fileName.endsWith(".yml") ? fileName : fileName + ".yml"
            );

            // 检查文件是否存在
            if (!configFile.exists()) {
                LogUtil.warning(plugin.getName(), "配置文件不存在: " + configFile.getAbsolutePath());
                return null;
            }

            YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);

            // 根据配置决定是否输出日志
            if (Main.config != null && Main.config.getBoolean("Logging.VerboseConfigLogs", false)) {
                LogUtil.success(plugin.getName(), "成功加载配置文件: " + configFile.getAbsolutePath());
            }
            LogUtil.debug(plugin.getName(), "配置文件已加载: " + configFile.getName());

            return config;
        } catch (IOException e) {
            LogUtil.error(plugin.getName(), "加载配置文件时发生IO错误", e);
            return null;
        } catch (Exception e) {
            LogUtil.error(plugin.getName(), "加载配置文件时发生未知错误", e);
            return null;
        }
    }

    /**
     * 读取系统路径下的yml配置文件
     * 不受插件数据文件夹限制
     *
     * @param plugin   主类（用于日志输出）
     * @param dirName  文件夹完整路径
     * @param fileName 文件名（可带或不带.yml后缀）
     * @return 文件的YamlConfiguration实例
     */
    @Nonnull
    static YamlConfiguration loadSystemConfig(Plugin plugin, String dirName, String fileName) {
        File configFile = new File(dirName, fileName.endsWith(".yml") ? fileName : fileName + ".yml");
        LogUtil.debug(plugin.getName(), "加载系统配置文件: " + configFile.getAbsolutePath());
        return YamlConfiguration.loadConfiguration(configFile);
    }

    /**
     * 保存配置文件到系统路径
     * 不受插件数据文件夹限制
     *
     * @param plugin   主类
     * @param config   配置文件实例
     * @param dirName  文件夹完整路径
     * @param fileName 文件名称（可带或不带.yml后缀）
     */
    static void saveSystemConfig(Plugin plugin, FileConfiguration config, String dirName, String fileName) {
        try {
            new File(dirName).mkdirs();
            File file = new File(dirName, fileName.endsWith(".yml") ? fileName : fileName + ".yml");

            try {
                config.save(file);
                LogUtil.debug(plugin.getName(), "系统配置文件已保存: " + file.getAbsolutePath());
            } catch (IOException e) {
                // 如果在异步线程中保存失败，尝试同步保存
                LogUtil.debug(plugin.getName(), "异步保存失败，尝试同步保存...");
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        try {
                            config.save(file);
                            LogUtil.debug(plugin.getName(), "同步保存成功: " + file.getName());
                        } catch (IOException ex) {
                            LogUtil.error(plugin.getName(), "同步保存配置文件失败", ex);
                        }
                    }
                }.runTask(plugin);
            }
        } catch (Exception e) {
            LogUtil.error(plugin.getName(), "保存系统配置文件时发生错误", e);
        }
    }

    /**
     * 保存配置文件到插件数据文件夹
     *
     * @param plugin   主类
     * @param config   配置文件实例
     * @param dirName  相对于插件数据文件夹的子目录名称
     * @param fileName 文件名称（可带或不带.yml后缀）
     */
    static void saveConfig(Plugin plugin, FileConfiguration config, String dirName, String fileName) {
        try {
            File baseDir = getValidPluginDataFolder(plugin);
            File dir = new File(IFile.formattedPath(baseDir.getCanonicalPath() + "\\" + dirName));
            dir.mkdirs();

            File file = new File(dir, fileName.endsWith(".yml") ? fileName : fileName + ".yml");

            try {
                config.save(file);

                // 根据配置决定是否输出日志
                if (Main.config != null && Main.config.getBoolean("Logging.VerboseConfigLogs", false)) {
                    LogUtil.success(plugin.getName(), "成功保存配置文件: " + file.getAbsolutePath());
                }
                LogUtil.debug(plugin.getName(), "配置文件已保存: " + file.getName());

            } catch (IOException e) {
                LogUtil.error(plugin.getName(), "保存配置文件时发生IO错误", e);
            }
        } catch (Exception e) {
            LogUtil.error(plugin.getName(), "保存配置文件时发生未知错误", e);
        }
    }

    /**
     * 异步保存配置文件
     * 适用于需要频繁保存但不需要立即生效的场景
     *
     * @param plugin   主类
     * @param config   配置文件实例
     * @param dirName  相对于插件数据文件夹的子目录名称
     * @param fileName 文件名称（可带或不带.yml后缀）
     */
    static void saveConfigAsync(Plugin plugin, FileConfiguration config, String dirName, String fileName) {
        new BukkitRunnable() {
            @Override
            public void run() {
                saveConfig(plugin, config, dirName, fileName);
            }
        }.runTaskAsynchronously(plugin);
    }
}