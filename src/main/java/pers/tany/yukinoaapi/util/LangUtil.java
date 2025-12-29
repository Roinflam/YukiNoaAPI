package pers.tany.yukinoaapi.util;

import org.bukkit.configuration.file.YamlConfiguration;
import pers.tany.yukinoaapi.Main;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.MessageFormat;

/**
 * 语言管理工具类
 * 提供多语言支持，从语言文件中获取文本
 *
 * <p>注意：此类自身的日志使用硬编码，因为初始化时自己还不可用</p>
 *
 * @author tany
 * @since 3.0.0
 */
public final class LangUtil {

    /**
     * 语言配置文件
     */
    private static YamlConfiguration langConfig;

    /**
     * 当前语言代码
     */
    private static String currentLang = "zh_cn";

    /**
     * 语言文件目录
     */
    private static File langDir;

    /**
     * 是否已初始化
     */
    private static boolean initialized = false;

    /**
     * 私有构造函数，防止实例化
     */
    private LangUtil() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }

    /**
     * 检查语言系统是否已初始化
     *
     * @return 已初始化返回true
     */
    public static boolean isInitialized() {
        return initialized;
    }

    /**
     * 初始化语言系统
     * 此方法的日志使用硬编码，因为初始化时 LangUtil 自身还不可用
     */
    public static void init() {
        if (Main.plugin == null) {
            // 硬编码日志：语言系统初始化失败
            LogUtil.error("Language system initialization failed: plugin instance is null");
            return;
        }

        // 创建语言文件目录
        langDir = new File(Main.plugin.getDataFolder(), "lang");
        if (!langDir.exists()) {
            langDir.mkdirs();
        }

        // 释放默认语言文件
        extractDefaultLangFile("zh_cn.yml");
        extractDefaultLangFile("en_us.yml");

        // 加载配置的语言
        reload();

        // 标记已初始化
        initialized = true;

        // 硬编码日志：初始化完成
        LogUtil.debug("Language system initialized, current language: " + currentLang);
    }

    /**
     * 重新加载语言配置
     * 此方法的日志使用硬编码
     */
    public static void reload() {
        if (Main.config != null) {
            currentLang = Main.config.getString("Language", "zh_cn").toLowerCase();
        }

        File langFile = new File(langDir, currentLang + ".yml");

        if (!langFile.exists()) {
            // 硬编码日志：语言文件不存在
            LogUtil.warning("Language file not found: " + currentLang + ".yml, using default zh_cn");
            currentLang = "zh_cn";
            langFile = new File(langDir, "zh_cn.yml");
        }

        if (langFile.exists()) {
            langConfig = YamlConfiguration.loadConfiguration(langFile);
            // 硬编码日志：已加载语言文件
            LogUtil.debug("Language file loaded: " + langFile.getName());
        } else {
            // 硬编码日志：默认语言文件也不存在
            LogUtil.error("Default language file not found, language system may not work properly");
            langConfig = new YamlConfiguration();
        }
    }

    /**
     * 从jar中释放默认语言文件
     * 此方法的日志使用硬编码
     *
     * @param fileName 语言文件名
     */
    private static void extractDefaultLangFile(String fileName) {
        File targetFile = new File(langDir, fileName);

        if (targetFile.exists()) {
            return;
        }

        try (InputStream inputStream = Main.class.getClassLoader().getResourceAsStream("lang/" + fileName)) {
            if (inputStream != null) {
                Files.copy(inputStream, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                // 硬编码日志：已释放语言文件
                LogUtil.debug("Language file extracted: " + fileName);
            } else {
                // 硬编码日志：在jar中未找到语言文件
                LogUtil.warning("Language file not found in jar: lang/" + fileName);
            }
        } catch (IOException e) {
            // 硬编码日志：释放语言文件失败
            LogUtil.error("Failed to extract language file: " + fileName, e);
        }
    }

    /**
     * 获取语言文本
     *
     * @param key 语言键
     * @return 对应的文本，如果不存在返回键本身
     */
    @Nonnull
    public static String get(@Nonnull String key) {
        if (langConfig == null) {
            return key;
        }
        String value = langConfig.getString(key);
        return value != null ? value : key;
    }

    /**
     * 获取语言文本并替换占位符
     *
     * @param key  语言键
     * @param args 占位符参数
     * @return 替换占位符后的文本
     */
    @Nonnull
    public static String get(@Nonnull String key, Object... args) {
        String template = get(key);

        if (args == null || args.length == 0) {
            return template;
        }

        try {
            return MessageFormat.format(template, args);
        } catch (IllegalArgumentException e) {
            // MessageFormat 失败时使用简单替换
            String result = template;
            for (int i = 0; i < args.length; i++) {
                result = result.replace("{" + i + "}", String.valueOf(args[i]));
            }
            return result;
        }
    }

    /**
     * 安全获取语言文本
     * 如果 LangUtil 未初始化，返回默认值
     *
     * @param key          语言键
     * @param defaultValue 默认值
     * @return 对应的文本或默认值
     */
    @Nonnull
    public static String getSafe(@Nonnull String key, @Nonnull String defaultValue) {
        if (!initialized || langConfig == null) {
            return defaultValue;
        }
        String value = langConfig.getString(key);
        return value != null ? value : defaultValue;
    }

    /**
     * 安全获取语言文本并替换占位符
     * 如果 LangUtil 未初始化，返回默认值
     *
     * @param key          语言键
     * @param defaultValue 默认值
     * @param args         占位符参数
     * @return 替换占位符后的文本或默认值
     */
    @Nonnull
    public static String getSafe(@Nonnull String key, @Nonnull String defaultValue, Object... args) {
        if (!initialized || langConfig == null) {
            return defaultValue;
        }
        return get(key, args);
    }

    /**
     * 获取带前缀的语言文本
     *
     * @param key 语言键
     * @return 带前缀的文本
     */
    @Nonnull
    public static String getWithPrefix(@Nonnull String key) {
        return get("common.prefix") + get(key);
    }

    /**
     * 获取带前缀的语言文本并替换占位符
     *
     * @param key  语言键
     * @param args 占位符参数
     * @return 带前缀且替换占位符后的文本
     */
    @Nonnull
    public static String getWithPrefix(@Nonnull String key, Object... args) {
        return get("common.prefix") + get(key, args);
    }

    /**
     * 检查语言键是否存在
     *
     * @param key 语言键
     * @return 存在返回true
     */
    public static boolean has(@Nonnull String key) {
        return langConfig != null && langConfig.contains(key);
    }

    /**
     * 获取当前语言代码
     *
     * @return 当前语言代码
     */
    @Nonnull
    public static String getCurrentLang() {
        return currentLang;
    }

    /**
     * 设置当前语言并重新加载
     *
     * @param lang 语言代码
     */
    public static void setCurrentLang(@Nonnull String lang) {
        currentLang = lang.toLowerCase();
        reload();
    }
}