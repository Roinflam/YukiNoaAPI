// 文件：LogUtil.java
// 路径：src/pers/tany/yukinoaapi/util/LogUtil.java
package pers.tany.yukinoaapi.util;

import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import pers.tany.yukinoaapi.Main;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * 日志管理工具类
 * 提供统一的日志输出接口，支持调试模式控制
 *
 * <p>使用示例：</p>
 * <pre>
 * // 普通信息日志
 * LogUtil.info("插件已启动");
 *
 * // 调试日志（仅在调试模式开启时输出）
 * LogUtil.debug("正在处理玩家数据: " + playerName);
 *
 * // 警告日志
 * LogUtil.warning("配置文件格式有误，使用默认值");
 *
 * // 错误日志（带异常）
 * LogUtil.error("数据库连接失败", exception);
 *
 * // 错误日志（不带异常）
 * LogUtil.error("未找到指定的玩家");
 * </pre>
 *
 * @author tany
 * @since 3.0.0
 */
public final class LogUtil {

    /**
     * 插件名称前缀
     */
    private static final String PREFIX = "§7[§fYukiNoaAPI§7]";

    /**
     * 调试模式前缀
     */
    private static final String DEBUG_PREFIX = "§7[§eDebug§7]";

    /**
     * 私有构造函数，防止实例化
     */
    private LogUtil() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }

    /**
     * 获取控制台发送者
     *
     * @return 控制台发送者实例
     */
    @Nonnull
    private static ConsoleCommandSender getConsoleSender() {
        return Bukkit.getConsoleSender();
    }

    /**
     * 检查调试模式是否开启
     *
     * @return 如果调试模式开启返回true，否则返回false
     */
    public static boolean isDebugEnabled() {
        // 如果配置文件未加载，默认关闭调试模式
        if (Main.config == null) {
            return false;
        }
        return Main.config.getBoolean("Logging.Debug", false);
    }

    /**
     * 检查是否打印异常堆栈
     *
     * @return 如果需要打印堆栈返回true，否则返回false
     */
    public static boolean isPrintStackTraceEnabled() {
        // 如果配置文件未加载，默认打印堆栈
        if (Main.config == null) {
            return true;
        }
        return Main.config.getBoolean("Logging.PrintStackTrace", true);
    }

    /**
     * 输出信息级别日志
     *
     * @param message 日志消息
     */
    public static void info(@Nonnull String message) {
        getConsoleSender().sendMessage(PREFIX + "§a " + message);
    }

    /**
     * 输出信息级别日志（带插件名称）
     *
     * @param pluginName 插件名称
     * @param message    日志消息
     */
    public static void info(@Nonnull String pluginName, @Nonnull String message) {
        getConsoleSender().sendMessage("§7[§f" + pluginName + "§7]§a " + message);
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
            getConsoleSender().sendMessage("§7[§f" + pluginName + "§7]" + DEBUG_PREFIX + "§7 " + message);
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
        getConsoleSender().sendMessage("§7[§f" + pluginName + "§7]§e " + message);
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
        getConsoleSender().sendMessage("§7[§f" + pluginName + "§7]§c " + message);
    }

    /**
     * 输出错误级别日志（带异常信息）
     * 根据配置决定是否打印完整堆栈
     *
     * @param message   日志消息
     * @param throwable 异常对象
     */
    public static void error(@Nonnull String message, @Nullable Throwable throwable) {
        getConsoleSender().sendMessage(PREFIX + "§c " + message);
        if (throwable != null) {
            if (isPrintStackTraceEnabled()) {
                throwable.printStackTrace();
            } else {
                // 仅输出简要错误信息
                getConsoleSender().sendMessage(PREFIX + "§c 错误类型: " + throwable.getClass().getSimpleName());
                if (throwable.getMessage() != null) {
                    getConsoleSender().sendMessage(PREFIX + "§c 错误信息: " + throwable.getMessage());
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
        String prefix = "§7[§f" + pluginName + "§7]§c ";
        getConsoleSender().sendMessage(prefix + message);
        if (throwable != null) {
            if (isPrintStackTraceEnabled()) {
                throwable.printStackTrace();
            } else {
                // 仅输出简要错误信息
                getConsoleSender().sendMessage(prefix + "错误类型: " + throwable.getClass().getSimpleName());
                if (throwable.getMessage() != null) {
                    getConsoleSender().sendMessage(prefix + "错误信息: " + throwable.getMessage());
                }
            }
        }
    }

    /**
     * 输出成功级别日志（绿色）
     *
     * @param message 日志消息
     */
    public static void success(@Nonnull String message) {
        getConsoleSender().sendMessage(PREFIX + "§a " + message);
    }

    /**
     * 输出成功级别日志（带插件名称）
     *
     * @param pluginName 插件名称
     * @param message    日志消息
     */
    public static void success(@Nonnull String pluginName, @Nonnull String message) {
        getConsoleSender().sendMessage("§7[§f" + pluginName + "§7]§a " + message);
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
        getConsoleSender().sendMessage("§7[§f" + pluginName + "§7]" + colorCode + " " + message);
    }
}