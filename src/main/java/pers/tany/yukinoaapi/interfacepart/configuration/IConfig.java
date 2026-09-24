package pers.tany.yukinoaapi.interfacepart.configuration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import pers.tany.yukinoaapi.Main;

/**
 * 配置文件操作接口
 * 提供统一的配置文件管理功能
 */
public interface IConfig {

    /**
     * 获取有效的插件数据文件夹
     *
     * @param plugin 插件实例
     * @return 数据文件夹
     */
    static File getValidPluginDataFolder(@Nonnull Plugin plugin) {
        if ("YukiNoaAPI".equals(plugin.getName())) {
            return plugin.getDataFolder();
        } else {
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
    }

    /**
     * 创建目录（包含子目录）
     *
     * @param plugin 插件实例
     * @param dirName 目录名称
     * @return 是否成功
     */
    static boolean createDir(@Nonnull Plugin plugin, String dirName) {
        return createDir(plugin, dirName, true);
    }

    /**
     * 创建目录
     * 跨平台修复：使用 File 构造函数而非硬编码路径分隔符
     *
     * @param plugin 插件实例
     * @param dirName 目录名称
     * @param subdirectory 是否创建子目录
     * @return 是否成功
     */
    static boolean createDir(@Nonnull Plugin plugin, String dirName, boolean subdirectory) {
        try {
            File baseDir = getValidPluginDataFolder(plugin);
            // 跨平台修复：使用 File 构造函数
            File dir = new File(baseDir, dirName);
            boolean result = subdirectory ? dir.mkdirs() : dir.mkdir();
            if (!result && !dir.exists()) {
                Bukkit.getConsoleSender().sendMessage("§7[§f" + plugin.getName() + "§7] §c无法创建目录: " + dir.getAbsolutePath());
            }

            return result || dir.exists();
        } catch (Exception exception) {
            Bukkit.getConsoleSender().sendMessage("§7[§f" + plugin.getName() + "§7] §c创建目录时发生错误: " + exception.getMessage());
            return false;
        }
    }

    /**
     * 从 jar 包中创建资源文件
     * 跨平台修复：
     * 1. 使用 File 构造函数处理文件系统路径
     * 2. 使用 / 作为jar内部资源路径分隔符
     * 3. 正确拼接资源路径
     *
     * @param plugin 插件实例
     * @param dirName 目录名称
     * @param fileName 文件名称
     * @param cover 是否覆盖现有文件
     * @return 是否成功
     */
    static boolean createResource(@Nonnull Plugin plugin, String dirName, String fileName, boolean cover) {
        try {
            File baseDir = getValidPluginDataFolder(plugin);
            // 跨平台修复：使用 File 构造函数
            File dir = new File(baseDir, dirName);
            dir.mkdirs();
            File file = new File(dir, fileName);

            if (file.exists() && !cover) {
                return false;
            }

            // 构造jar内部资源路径（统一使用正斜杠）
            String resourcePath;
            if (dirName == null || dirName.isEmpty()) {
                resourcePath = fileName;
            } else {
                // jar内部资源路径必须使用 / 作为分隔符
                resourcePath = dirName.replace("\\", "/").replace(File.separator, "/") + "/" + fileName;
            }

            // 尝试获取资源输入流
            InputStream resourceStream = plugin.getClass().getClassLoader().getResourceAsStream(resourcePath);

            if (resourceStream == null) {
                Bukkit.getConsoleSender().sendMessage("§7[§f" + plugin.getName() + "§7] §c资源文件未找到: " + resourcePath);
                return false;
            }

            // 复制资源文件
            Files.copy(resourceStream, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            resourceStream.close();

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 加载配置文件
     * 跨平台修复：使用 File 构造函数
     *
     * @param plugin 插件实例
     * @param dirName 目录名称
     * @param fileName 文件名称
     * @return 配置对象，加载失败返回 null
     */
    @Nullable
    static YamlConfiguration loadConfig(@Nonnull Plugin plugin, String dirName, @Nonnull String fileName) {
        try {
            File baseDir = getValidPluginDataFolder(plugin);
            // 跨平台修复：使用 File 构造函数
            File dir = new File(baseDir, dirName);
            File configFile = new File(dir, fileName.endsWith(".yml") ? fileName : fileName + ".yml");

            if (!configFile.exists()) {
                Bukkit.getConsoleSender().sendMessage("§7[§f" + plugin.getName() + "§7] §c配置文件不存在: " + configFile.getAbsolutePath());
                return null;
            }

            return YamlConfiguration.loadConfiguration(configFile);
        } catch (Exception e) {
            Bukkit.getConsoleSender().sendMessage("§7[§f" + plugin.getName() + "§7] §c加载配置文件时发生错误: " + e.getMessage());
            return null;
        }
    }

    /**
     * 加载系统配置文件
     *
     * @param plugin 插件实例
     * @param dirName 目录名称
     * @param fileName 文件名称
     * @return 配置对象
     */
    @Nonnull
    static YamlConfiguration loadSystemConfig(Plugin plugin, String dirName, @Nonnull String fileName) {
        return YamlConfiguration.loadConfiguration(new File(dirName, fileName.endsWith(".yml") ? fileName : fileName + ".yml"));
    }

    /**
     * 保存系统配置文件
     *
     * @param plugin 插件实例
     * @param config 配置对象
     * @param dirName 目录名称
     * @param fileName 文件名称
     */
    static void saveSystemConfig(Plugin plugin, @Nonnull final FileConfiguration config, @Nonnull String dirName, @Nonnull String fileName) {
        try {
            (new File(dirName)).mkdirs();
            final File file = new File(dirName, fileName.endsWith(".yml") ? fileName : fileName + ".yml");

            try {
                try {
                    config.save(file);
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            } catch (Exception var7) {
                (new BukkitRunnable() {
                    public void run() {
                        try {
                            config.save(file);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }).runTask(plugin);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 保存配置文件
     * 跨平台修复：使用 File 构造函数
     *
     * @param plugin 插件实例
     * @param config 配置对象
     * @param dirName 目录名称
     * @param fileName 文件名称
     */
    static void saveConfig(@Nonnull Plugin plugin, @Nonnull FileConfiguration config, String dirName, @Nonnull String fileName) {
        try {
            File baseDir = getValidPluginDataFolder(plugin);
            // 跨平台修复：使用 File 构造函数
            File dir = new File(baseDir, dirName);
            dir.mkdirs();
            File file = new File(dir, fileName.endsWith(".yml") ? fileName : fileName + ".yml");

            try {
                // 与 saveConfigAsync 的后台写入互斥，并作废排队中的旧快照：
                // 保证同步保存之后，文件不会被一份更早提交的异步快照覆盖回去（例如插件 onDisable 里的同步保存）
                Object lock = AsyncConfigSaver.existingLock(file);
                if (lock == null) {
                    // 这个文件从没用过异步保存：与 3.3.0 完全相同，不建任何条目
                    config.save(file);
                } else {
                    synchronized (lock) {
                        long id = AsyncConfigSaver.supersede(file);
                        try {
                            config.save(file);
                        } finally {
                            AsyncConfigSaver.release(file, id);
                        }
                    }
                }
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

    /**
     * 异步保存配置文件（3.4.0 新增）：在调用线程上拍快照，YAML 序列化与写盘在后台线程完成。
     * <p>适合定时保存大数据文件：序列化是保存的主要耗时，在主线程做会造成卡顿。
     * 写出的内容与 {@link #saveConfig} 读回等价（含注释、文件头）；先写临时文件再替换；
     * 只接受 {@code YamlConfiguration} 本类，其他配置类型与认不出的值类型会自动退回同步保存；
     * 同一文件连续提交只写最新一份；之后若对同一文件调用 {@link #saveConfig}，以同步那次为准。</p>
     * <p>必须在拥有这个配置对象的线程上调用（通常是主线程），调用返回后可以继续修改 config。
     * 插件 onDisable 里请继续用同步的 {@link #saveConfig}。</p>
     * <p>兼容旧版本库：调用方可以用 {@code try { saveConfigAsync(...) } catch (NoSuchMethodError e) { saveConfig(...) }}
     * 在旧版 YukiNoaAPI 上自动退回同步保存。</p>
     *
     * @param plugin 插件实例
     * @param config 配置对象
     * @param dirName 目录名称
     * @param fileName 文件名称
     */
    static void saveConfigAsync(@Nonnull Plugin plugin, @Nonnull FileConfiguration config, String dirName, @Nonnull String fileName) {
        // 快照固定是 YamlConfiguration：其他格式（JSON 等）或重写了序列化的子类，走原来的同步保存，保证格式不被改写
        if (config.getClass() != YamlConfiguration.class) {
            saveConfig(plugin, config, dirName, fileName);
            return;
        }
        try {
            File baseDir = getValidPluginDataFolder(plugin);
            File dir = new File(baseDir, dirName);
            dir.mkdirs();
            File file = new File(dir, fileName.endsWith(".yml") ? fileName : fileName + ".yml");
            AsyncConfigSaver.save(plugin, config, file, Main.config.getBoolean("Logging.VerboseConfigLogs", false));
        } catch (Exception e) {
            Bukkit.getConsoleSender().sendMessage("§7[§f" + plugin.getName() + "§7] §c异步保存配置文件时发生未知错误，改为同步保存: " + e.getMessage());
            saveConfig(plugin, config, dirName, fileName);
        }
    }

    /**
     * 等待所有排队中的异步保存写完（最多 30 秒，3.4.0 新增）。插件 reload 前可调用；本库卸载时会自动调用。
     */
    static void flushAsyncSaves() {
        AsyncConfigSaver.flush(30_000L);
    }
}
