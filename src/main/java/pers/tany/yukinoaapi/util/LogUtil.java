package pers.tany.yukinoaapi.util;

import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import pers.tany.yukinoaapi.Main;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * 日志管理工具类
 * 提供统一的日志输出接口，支持调试模式控制
 * 使用缓存机制优化性能，避免频繁获取控制台发送者
 */
public final class LogUtil {

    /**
     * 插件名称前缀 - 使用灰白配色
     */
    private static final String PREFIX = "§8[§fYukiNoaAPI§8]";

    /**
     * 调试模式前缀 - 使用蓝色高亮
     */
    private static final String DEBUG_PREFIX = "§8[§bDebug§8]";

    /**
     * 缓存的控制台发送者实例
     * 使用 volatile 保证多线程可见性
     */
    private static volatile ConsoleCommandSender cachedConsoleSender;

    /**
     * 调试模式状态缓存
     * 避免每次都读取配置文件
     */
    private static volatile Boolean cachedDebugEnabled;

    /**
     * 堆栈打印状态缓存
     */
    private static volatile Boolean cachedPrintStackTrace;

    /**
     * 缓存失效时间戳
     * 每隔一段时间刷新缓存，确保配置变更生效
     */
    private static volatile long cacheExpireTime = 0;

    /**
     * 缓存有效期（毫秒）
     * 5秒后缓存失效，重新读取配置
     */
    private static final long CACHE_DURATION = 5000L;

    /**
     * 私有构造函数，防止实例化
     */
    private LogUtil() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }

    /**
     * 获取控制台发送者（带缓存）
     * 使用双重检查锁定模式确保线程安全
     *
     * @return 控制台发送者实例
     */
    @Nonnull
    private static ConsoleCommandSender getConsoleSender() {
        ConsoleCommandSender sender = cachedConsoleSender;
        if (sender == null) {
            synchronized (LogUtil.class) {
                sender = cachedConsoleSender;
                if (sender == null) {
                    sender = Bukkit.getConsoleSender();
                    cachedConsoleSender = sender;
                }
            }
        }
        return sender;
    }

    /**
     * 检查并刷新配置缓存
     * 当缓存过期时重新读取配置文件
     */
    private static void refreshCacheIfNeeded() {
        long now = System.currentTimeMillis();
        if (now > cacheExpireTime) {
            synchronized (LogUtil.class) {
                if (now > cacheExpireTime) {
                    cachedDebugEnabled = null;
                    cachedPrintStackTrace = null;
                    cacheExpireTime = now + CACHE_DURATION;
                }
            }
        }
    }

    /**
     * 强制刷新缓存
     * 在配置重载后调用此方法
     */
    public static void invalidateCache() {
        synchronized (LogUtil.class) {
            cachedDebugEnabled = null;
            cachedPrintStackTrace = null;
            cacheExpireTime = 0;
        }
    }

    /**
     * 检查调试模式是否开启（带缓存）
     *
     * @return 如果调试模式开启返回true，否则返回false
     */
    public static boolean isDebugEnabled() {
        refreshCacheIfNeeded();

        Boolean cached = cachedDebugEnabled;
        if (cached != null) {
            return cached;
        }

        synchronized (LogUtil.class) {
            cached = cachedDebugEnabled;
            if (cached != null) {
                return cached;
            }

            if (Main.config == null) {
                cachedDebugEnabled = false;
                return false;
            }

            cached = Main.config.getBoolean("Logging.Debug", false);
            cachedDebugEnabled = cached;
            return cached;
        }
    }

    /**
     * 检查是否打印异常堆栈（带缓存）
     *
     * @return 如果需要打印堆栈返回true，否则返回false
     */
    public static boolean isPrintStackTraceEnabled() {
        refreshCacheIfNeeded();

        Boolean cached = cachedPrintStackTrace;
        if (cached != null) {
            return cached;
        }

        synchronized (LogUtil.class) {
            cached = cachedPrintStackTrace;
            if (cached != null) {
                return cached;
            }

            if (Main.config == null) {
                cachedPrintStackTrace = true;
                return true;
            }

            cached = Main.config.getBoolean("Logging.PrintStackTrace", true);
            cachedPrintStackTrace = cached;
            return cached;
        }
    }

    /**
     * 输出信息级别日志
     *
     * @param message 日志消息
     */
    public static void info(@Nonnull String message) {
        getConsoleSender().sendMessage(PREFIX + "§7 " + message);
    }

    /**
     * 输出信息级别日志（带插件名称）
     *
     * @param pluginName 插件名称
     * @param message    日志消息
     */
    public static void info(@Nonnull String pluginName, @Nonnull String message) {
        getConsoleSender().sendMessage("§8[§f" + pluginName + "§8]§7 " + message);
    }

    /**
     * 输出调试级别日志
     * 仅在配置文件中 Logging.Debug 为 true 时才会输出
     *
     * @param message 日志消息
     */
    public static void debug(@Nonnull String message) {
        if (isDebugEnabled()) {
            getConsoleSender().sendMessage(PREFIX + DEBUG_PREFIX + "§7 " + message);
        }
    }

    /**
     * 输出调试级别日志（带插件名称）
     * 仅在配置文件中 Logging.Debug 为 true 时才会输出
     *
     * @param pluginName 插件名称
     * @param message    日志消息
     */
    public static void debug(@Nonnull String pluginName, @Nonnull String message) {
        if (isDebugEnabled()) {
            getConsoleSender().sendMessage("§8[§f" + pluginName + "§8]" + DEBUG_PREFIX + "§7 " + message);
        }
    }

    /**
     * 输出警告级别日志
     *
     * @param message 日志消息
     */
    public static void warning(@Nonnull String message) {
        getConsoleSender().sendMessage(PREFIX + "§e " + message);
    }

    /**
     * 输出警告级别日志（带插件名称）
     *
     * @param pluginName 插件名称
     * @param message    日志消息
     */
    public static void warning(@Nonnull String pluginName, @Nonnull String message) {
        getConsoleSender().sendMessage("§8[§f" + pluginName + "§8]§e " + message);
    }

    /**
     * 输出错误级别日志
     *
     * @param message 日志消息
     */
    public static void error(@Nonnull String message) {
        getConsoleSender().sendMessage(PREFIX + "§c " + message);
    }

    /**
     * 输出错误级别日志（带插件名称）
     *
     * @param pluginName 插件名称
     * @param message    日志消息
     */
    public static void error(@Nonnull String pluginName, @Nonnull String message) {
        getConsoleSender().sendMessage("§8[§f" + pluginName + "§8]§c " + message);
    }

    /**
     * 输出错误级别日志（带异常信息）
     * 根据配置决定是否打印完整堆栈
     *
     * @param message   日志消息
     * @param throwable 异常对象
     */
    public static void error(@Nonnull String message, @Nullable Throwable throwable) {
        ConsoleCommandSender sender = getConsoleSender();
        sender.sendMessage(PREFIX + "§c " + message);

        if (throwable != null) {
            if (isPrintStackTraceEnabled()) {
                throwable.printStackTrace();
            } else {
                sender.sendMessage(PREFIX + "§c 错误类型: " + throwable.getClass().getSimpleName());
                String errorMsg = throwable.getMessage();
                if (errorMsg != null) {
                    sender.sendMessage(PREFIX + "§c 错误信息: " + errorMsg);
                }
            }
        }
    }

    /**
     * 输出错误级别日志（带插件名称和异常信息）
     * 根据配置决定是否打印完整堆栈
     *
     * @param pluginName 插件名称
     * @param message    日志消息
     * @param throwable  异常对象
     */
    public static void error(@Nonnull String pluginName, @Nonnull String message, @Nullable Throwable throwable) {
        String prefix = "§8[§f" + pluginName + "§8]§c ";
        ConsoleCommandSender sender = getConsoleSender();
        sender.sendMessage(prefix + message);

        if (throwable != null) {
            if (isPrintStackTraceEnabled()) {
                throwable.printStackTrace();
            } else {
                sender.sendMessage(prefix + "错误类型: " + throwable.getClass().getSimpleName());
                String errorMsg = throwable.getMessage();
                if (errorMsg != null) {
                    sender.sendMessage(prefix + "错误信息: " + errorMsg);
                }
            }
        }
    }

    /**
     * 输出成功级别日志（蓝色高亮）
     *
     * @param message 日志消息
     */
    public static void success(@Nonnull String message) {
        getConsoleSender().sendMessage(PREFIX + "§b " + message);
    }

    /**
     * 输出成功级别日志（带插件名称）
     *
     * @param pluginName 插件名称
     * @param message    日志消息
     */
    public static void success(@Nonnull String pluginName, @Nonnull String message) {
        getConsoleSender().sendMessage("§8[§f" + pluginName + "§8]§b " + message);
    }

    /**
     * 输出原始消息（不带任何前缀）
     *
     * @param message 日志消息
     */
    public static void raw(@Nonnull String message) {
        getConsoleSender().sendMessage(message);
    }

    /**
     * 输出带自定义颜色的日志
     *
     * @param colorCode 颜色代码（如 "§6" 表示金色）
     * @param message   日志消息
     */
    public static void colored(@Nonnull String colorCode, @Nonnull String message) {
        getConsoleSender().sendMessage(PREFIX + colorCode + " " + message);
    }

    /**
     * 输出带自定义颜色的日志（带插件名称）
     *
     * @param pluginName 插件名称
     * @param colorCode  颜色代码（如 "§6" 表示金色）
     * @param message    日志消息
     */
    public static void colored(@Nonnull String pluginName, @Nonnull String colorCode, @Nonnull String message) {
        getConsoleSender().sendMessage("§8[§f" + pluginName + "§8]" + colorCode + " " + message);
    }
}