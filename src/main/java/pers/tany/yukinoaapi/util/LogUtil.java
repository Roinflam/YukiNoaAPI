package pers.tany.yukinoaapi.util;

import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import pers.tany.yukinoaapi.Main;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * 日志管理工具类
 * 提供统一的日志输出接口,支持调试模式控制
 *
 * @author tany
 * @since 3.0.0
 */
public final class LogUtil {

    /** 缓存的控制台发送者 */
    private static volatile ConsoleCommandSender cachedConsoleSender;

    /** 调试模式缓存 */
    private static volatile Boolean cachedDebugEnabled;

    /** 堆栈打印缓存 */
    private static volatile Boolean cachedPrintStackTrace;

    /** 缓存失效时间 */
    private static volatile long cacheExpireTime = 0;

    /** 缓存有效期（毫秒） */
    private static final long CACHE_DURATION = 5000L;

    /**
     * 私有构造函数
     */
    private LogUtil() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }

    /**
     * 获取控制台发送者
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
     * 获取语言文本
     *
     * @param key  语言键
     * @param args 参数
     * @return 语言文本
     */
    @Nonnull
    private static String getLang(@Nonnull String key, Object... args) {
        if (Main.plugin == null || ((Main) Main.plugin).getLanguageManager() == null) {
            return key;
        }

        if (args == null || args.length == 0) {
            return ((Main) Main.plugin).getLanguageManager().getMessage(key);
        } else {
            return ((Main) Main.plugin).getLanguageManager().getMessage(key, key, String.valueOf(args[0]));
        }
    }

    /**
     * 刷新缓存
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
     */
    public static void invalidateCache() {
        synchronized (LogUtil.class) {
            cachedDebugEnabled = null;
            cachedPrintStackTrace = null;
            cacheExpireTime = 0;
        }
    }

    /**
     * 检查调试模式是否开启
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

            if (Main.plugin == null || ((Main) Main.plugin).getConfigManager() == null) {
                cachedDebugEnabled = false;
                return false;
            }

            cached = ((Main) Main.plugin).getConfigManager().isDebugEnabled();
            cachedDebugEnabled = cached;
            return cached;
        }
    }

    /**
     * 检查是否打印堆栈
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

            if (Main.plugin == null || ((Main) Main.plugin).getConfigManager() == null) {
                cachedPrintStackTrace = true;
                return true;
            }

            cached = ((Main) Main.plugin).getConfigManager().isPrintStackTrace();
            cachedPrintStackTrace = cached;
            return cached;
        }
    }

    /**
     * 输出信息日志
     */
    public static void info(@Nonnull String message) {
        getConsoleSender().sendMessage(getLang("common.prefix") + "§7" + message);
    }

    /**
     * 输出调试日志
     */
    public static void debug(@Nonnull String message) {
        if (isDebugEnabled()) {
            getConsoleSender().sendMessage(getLang("common.prefix") + "§8[§bDebug§8] §7" + message);
        }
    }

    /**
     * 输出警告日志
     */
    public static void warning(@Nonnull String message) {
        getConsoleSender().sendMessage(getLang("common.prefix") + "§e" + message);
    }

    /**
     * 输出错误日志
     */
    public static void error(@Nonnull String message) {
        getConsoleSender().sendMessage(getLang("common.prefix") + "§c" + message);
    }

    /**
     * 输出错误日志（带异常）
     */
    public static void error(@Nonnull String message, @Nullable Throwable throwable) {
        ConsoleCommandSender sender = getConsoleSender();
        sender.sendMessage(getLang("common.prefix") + "§c" + message);

        if (throwable != null) {
            if (isPrintStackTraceEnabled()) {
                throwable.printStackTrace();
            } else {
                sender.sendMessage(getLang("common.prefix") + "§c" +
                        getLang("error.type", throwable.getClass().getSimpleName()));
                String errorMsg = throwable.getMessage();
                if (errorMsg != null) {
                    sender.sendMessage(getLang("common.prefix") + "§c" +
                            getLang("error.message", errorMsg));
                }
            }
        }
    }

    /**
     * 输出成功日志
     */
    public static void success(@Nonnull String message) {
        getConsoleSender().sendMessage(getLang("common.prefix") + "§b" + message);
    }
}