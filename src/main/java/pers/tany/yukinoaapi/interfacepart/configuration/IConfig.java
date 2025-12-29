package pers.tany.yukinoaapi.interfacepart.configuration;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import pers.tany.yukinoaapi.Main;
import pers.tany.yukinoaapi.util.LangUtil;
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
 * @author tany
 * @since 1.0.0
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
                LogUtil.error(plugin.getName(), LangUtil.get("config.custom-dir-invalid", baseDir.getAbsolutePath()));
                LogUtil.warning(plugin.getName(), LangUtil.get("config.custom-dir-fallback"));
                baseDir = plugin.getDataFolder();
            } else {
                LogUtil.success(plugin.getName(), LangUtil.get("config.custom-dir-using", baseDir.getAbsolutePath()));
            }
        } else {
            baseDir = plugin.getDataFolder();
            if (showDefaultPathLogs) {
                LogUtil.success(plugin.getName(), LangUtil.get("config.default-dir-using", baseDir.getAbsolutePath()));
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
                LogUtil.error(plugin.getName(), LangUtil.get("config.dir-create-fail", dir.getAbsolutePath()));
            } else {
                LogUtil.debug(plugin.getName(), LangUtil.get("config.dir-create-success", dir.getAbsolutePath()));
            }
            return result || dir.exists();
        } catch (Exception e) {
            LogUtil.error(plugin.getName(), LangUtil.get("config.dir-create-error"), e);
            return false;
        }
    }

    /**
     * 从插件里把对应名称的资源创建到对应插件文件夹里（静默模式，不输出日志）
     * 用于 LangUtil 初始化之前
     *
     * @param plugin   主类
     * @param dirName  文件夹名
     * @param fileName 文件名
     * @param cover    是否覆盖
     * @return 是否成功生成
     */
    static boolean createResourceSilent(Plugin plugin, String dirName, String fileName, boolean cover) {
        try {
            File baseDir = plugin.getDataFolder();
            File dir = new File(IFile.formattedPath(baseDir.getCanonicalPath() + "\\" + dirName));
            dir.mkdirs();
            File file = new File(IFile.formattedPath(dir + "\\" + fileName));

            if (!file.exists() || cover) {
                if (plugin.getClass().getClassLoader().getResourceAsStream(fileName) == null) {
                    return false;
                }

                Files.copy(
                        plugin.getClass().getClassLoader().getResourceAsStream(fileName),
                        file.toPath(),
                        StandardCopyOption.REPLACE_EXISTING
                );
                return true;
            }
        } catch (Exception e) {
            // 静默模式，不输出日志
        }
        return false;
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
                    LogUtil.error(plugin.getName(), LangUtil.get("config.resource-not-found", fileName));
                    return false;
                }

                Files.copy(
                        plugin.getClass().getClassLoader().getResourceAsStream(fileName),
                        file.toPath(),
                        StandardCopyOption.REPLACE_EXISTING
                );

                // 根据配置决定是否输出日志
                if (plugin.getName().equals("YukiNoaAPI") || Main.config.getBoolean("Logging.VerboseConfigLogs", false)) {
                    LogUtil.success(plugin.getName(), LangUtil.get("config.resource-create-success", file.getAbsolutePath()));
                }
                LogUtil.debug(plugin.getName(), LangUtil.get("config.resource-created", file.getName()));
                return true;
            } else {
                LogUtil.debug(plugin.getName(), LangUtil.get("config.resource-exists", file.getName()));
            }
        } catch (Exception e) {
            LogUtil.error(plugin.getName(), LangUtil.get("config.resource-create-error", fileName), e);
            return false;
        }
        return false;
    }

    /**
     * 读取插件的yml配置文件（静默模式，不输出日志）
     * 用于 LangUtil 初始化之前
     *
     * @param plugin   主类
     * @param dirName  文件夹名
     * @param fileName 文件名（可带或不带.yml后缀）
     * @return 文件的YamlConfiguration实例，加载失败返回空配置
     */
    @Nonnull
    static YamlConfiguration loadConfigSilent(Plugin plugin, String dirName, String fileName) {
        try {
            File baseDir = plugin.getDataFolder();
            File configFile = new File(
                    IFile.formattedPath(baseDir.getCanonicalPath() + "\\" + dirName),
                    fileName.endsWith(".yml") ? fileName : fileName + ".yml"
            );

            if (!configFile.exists()) {
                return new YamlConfiguration();
            }

            return YamlConfiguration.loadConfiguration(configFile);
        } catch (Exception e) {
            return new YamlConfiguration();
        }
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
                LogUtil.warning(plugin.getName(), LangUtil.get("config.file-not-found", configFile.getAbsolutePath()));
                return null;
            }

            YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);

            // 根据配置决定是否输出日志
            if (Main.config != null && Main.config.getBoolean("Logging.VerboseConfigLogs", false)) {
                LogUtil.success(plugin.getName(), LangUtil.get("config.load-success", configFile.getAbsolutePath()));
            }
            LogUtil.debug(plugin.getName(), LangUtil.get("config.loaded", configFile.getName()));

            return config;
        } catch (IOException e) {
            LogUtil.error(plugin.getName(), LangUtil.get("config.load-io-error"), e);
            return null;
        } catch (Exception e) {
            LogUtil.error(plugin.getName(), LangUtil.get("config.load-error"), e);
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
        LogUtil.debug(plugin.getName(), LangUtil.get("config.system-loaded", configFile.getAbsolutePath()));
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
                LogUtil.debug(plugin.getName(), LangUtil.get("config.system-saved", file.getAbsolutePath()));
            } catch (IOException e) {
                // 如果在异步线程中保存失败，尝试同步保存
                LogUtil.debug(plugin.getName(), LangUtil.get("config.sync-save-fail"));
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        try {
                            config.save(file);
                            LogUtil.debug(plugin.getName(), LangUtil.get("config.sync-save-success", file.getName()));
                        } catch (IOException ex) {
                            LogUtil.error(plugin.getName(), LangUtil.get("config.sync-save-error"), ex);
                        }
                    }
                }.runTask(plugin);
            }
        } catch (Exception e) {
            LogUtil.error(plugin.getName(), LangUtil.get("config.save-error"), e);
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
                    LogUtil.success(plugin.getName(), LangUtil.get("config.save-success", file.getAbsolutePath()));
                }
                LogUtil.debug(plugin.getName(), LangUtil.get("config.saved", file.getName()));

            } catch (IOException e) {
                LogUtil.error(plugin.getName(), LangUtil.get("config.save-io-error"), e);
            }
        } catch (Exception e) {
            LogUtil.error(plugin.getName(), LangUtil.get("config.save-error"), e);
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