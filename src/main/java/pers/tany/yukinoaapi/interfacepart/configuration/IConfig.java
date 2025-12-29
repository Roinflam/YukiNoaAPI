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
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 配置文件操作相关接口
 * 提供配置文件的创建、读取、保存等功能
 * 使用缓存机制优化路径计算和文件访问
 */
public interface IConfig {

    /**
     * 路径分隔符（缓存，避免重复判断系统类型）
     */
    String PATH_SEPARATOR = System.getProperty("os.name").toLowerCase().contains("windows") ? "\\" : "/";

    /**
     * 是否为Windows系统
     */
    boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().contains("windows");

    /**
     * 插件数据目录缓存
     * 避免重复计算和创建目录
     */
    ConcurrentHashMap<String, File> DATA_FOLDER_CACHE = new ConcurrentHashMap<>(16);

    /**
     * 获取有效的插件数据文件夹（带缓存）
     *
     * @param plugin 插件实例
     * @return 有效的插件数据文件夹
     */
    static File getValidPluginDataFolder(Plugin plugin) {
        String pluginName = plugin.getName();

        if ("YukiNoaAPI".equals(pluginName)) {
            return plugin.getDataFolder();
        }

        File cached = DATA_FOLDER_CACHE.get(pluginName);
        if (cached != null && cached.exists()) {
            return cached;
        }

        String customDir = Main.config.getString("CustomDataDirectory", "");
        boolean showDefaultPathLogs = Main.config.getBoolean("Logging.ShowDefaultPathLogs", false);
        File baseDir;

        if (customDir != null && !customDir.isEmpty()) {
            baseDir = new File(customDir, pluginName);
            if (!baseDir.exists() && !baseDir.mkdirs()) {
                LogUtil.error(pluginName, LangUtil.get("config.custom-dir-invalid", baseDir.getAbsolutePath()));
                LogUtil.warning(pluginName, LangUtil.get("config.custom-dir-fallback"));
                baseDir = plugin.getDataFolder();
            } else {
                LogUtil.success(pluginName, LangUtil.get("config.custom-dir-using", baseDir.getAbsolutePath()));
            }
        } else {
            baseDir = plugin.getDataFolder();
            if (showDefaultPathLogs) {
                LogUtil.success(pluginName, LangUtil.get("config.default-dir-using", baseDir.getAbsolutePath()));
            }
        }

        DATA_FOLDER_CACHE.put(pluginName, baseDir);
        return baseDir;
    }

    /**
     * 清空目录缓存
     * 在配置重载后调用
     */
    static void clearCache() {
        DATA_FOLDER_CACHE.clear();
    }

    /**
     * 格式化路径（使用缓存的分隔符）
     *
     * @param path 原始路径
     * @return 格式化后的路径
     */
    static String formatPath(String path) {
        if (IS_WINDOWS) {
            return path.replace("/", "\\");
        }
        return path.replace("\\", "/");
    }

    /**
     * 构建文件路径
     * 避免字符串拼接，使用 File 构造器
     *
     * @param base     基础目录
     * @param subPath  子路径
     * @param fileName 文件名
     * @return 完整文件对象
     */
    static File buildFile(File base, String subPath, String fileName) {
        if (subPath == null || subPath.isEmpty()) {
            return new File(base, fileName);
        }
        return new File(new File(base, subPath), fileName);
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
            File dir = new File(baseDir, dirName);

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
     * 从插件里把对应名称的资源创建到对应插件文件夹里（静默模式）
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
            File dir = (dirName == null || dirName.isEmpty()) ? baseDir : new File(baseDir, dirName);
            dir.mkdirs();

            File file = new File(dir, fileName);

            if (!file.exists() || cover) {
                try (InputStream is = plugin.getClass().getClassLoader().getResourceAsStream(fileName)) {
                    if (is == null) {
                        return false;
                    }
                    Files.copy(is, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    return true;
                }
            }
        } catch (Exception ignored) {
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
            File dir = (dirName == null || dirName.isEmpty()) ? baseDir : new File(baseDir, dirName);
            dir.mkdirs();

            File file = new File(dir, fileName);

            if (!file.exists() || cover) {
                try (InputStream is = plugin.getClass().getClassLoader().getResourceAsStream(fileName)) {
                    if (is == null) {
                        LogUtil.error(plugin.getName(), LangUtil.get("config.resource-not-found", fileName));
                        return false;
                    }

                    Files.copy(is, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    LogUtil.debug(plugin.getName(), LangUtil.get("config.resource-created", file.getName()));
                    return true;
                }
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
     * 读取插件的yml配置文件（静默模式）
     *
     * @param plugin   主类
     * @param dirName  文件夹名
     * @param fileName 文件名
     * @return 文件的YamlConfiguration实例
     */
    @Nonnull
    static YamlConfiguration loadConfigSilent(Plugin plugin, String dirName, String fileName) {
        try {
            File baseDir = plugin.getDataFolder();
            File dir = (dirName == null || dirName.isEmpty()) ? baseDir : new File(baseDir, dirName);
            String actualFileName = fileName.endsWith(".yml") ? fileName : fileName + ".yml";
            File configFile = new File(dir, actualFileName);

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
     * @param fileName 文件名
     * @return 文件的YamlConfiguration实例，加载失败返回null
     */
    @Nullable
    static YamlConfiguration loadConfig(Plugin plugin, String dirName, String fileName) {
        try {
            File baseDir = getValidPluginDataFolder(plugin);
            File dir = (dirName == null || dirName.isEmpty()) ? baseDir : new File(baseDir, dirName);
            String actualFileName = fileName.endsWith(".yml") ? fileName : fileName + ".yml";
            File configFile = new File(dir, actualFileName);

            if (!configFile.exists()) {
                LogUtil.warning(plugin.getName(), LangUtil.get("config.file-not-found", configFile.getAbsolutePath()));
                return null;
            }

            YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);

            if (Main.config != null && Main.config.getBoolean("Logging.VerboseConfigLogs", false)) {
                LogUtil.success(plugin.getName(), LangUtil.get("config.load-success", configFile.getAbsolutePath()));
            }
            LogUtil.debug(plugin.getName(), LangUtil.get("config.loaded", configFile.getName()));

            return config;
        } catch (Exception e) {
            LogUtil.error(plugin.getName(), LangUtil.get("config.load-error"), e);
            return null;
        }
    }

    /**
     * 读取系统路径下的yml配置文件
     *
     * @param plugin   主类
     * @param dirName  文件夹完整路径
     * @param fileName 文件名
     * @return 文件的YamlConfiguration实例
     */
    @Nonnull
    static YamlConfiguration loadSystemConfig(Plugin plugin, String dirName, String fileName) {
        String actualFileName = fileName.endsWith(".yml") ? fileName : fileName + ".yml";
        File configFile = new File(dirName, actualFileName);
        LogUtil.debug(plugin.getName(), LangUtil.get("config.system-loaded", configFile.getAbsolutePath()));
        return YamlConfiguration.loadConfiguration(configFile);
    }

    /**
     * 保存配置文件到系统路径
     *
     * @param plugin   主类
     * @param config   配置文件实例
     * @param dirName  文件夹完整路径
     * @param fileName 文件名称
     */
    static void saveSystemConfig(Plugin plugin, FileConfiguration config, String dirName, String fileName) {
        try {
            File dir = new File(dirName);
            dir.mkdirs();

            String actualFileName = fileName.endsWith(".yml") ? fileName : fileName + ".yml";
            File file = new File(dir, actualFileName);

            try {
                config.save(file);
                LogUtil.debug(plugin.getName(), LangUtil.get("config.system-saved", file.getAbsolutePath()));
            } catch (IOException e) {
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
     * @param dirName  子目录名称
     * @param fileName 文件名称
     */
    static void saveConfig(Plugin plugin, FileConfiguration config, String dirName, String fileName) {
        try {
            File baseDir = getValidPluginDataFolder(plugin);
            File dir = (dirName == null || dirName.isEmpty()) ? baseDir : new File(baseDir, dirName);
            dir.mkdirs();

            String actualFileName = fileName.endsWith(".yml") ? fileName : fileName + ".yml";
            File file = new File(dir, actualFileName);

            try {
                config.save(file);

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
     *
     * @param plugin   主类
     * @param config   配置文件实例
     * @param dirName  子目录名称
     * @param fileName 文件名称
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