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
import java.util.concurrent.ConcurrentHashMap;

/**
 * 语言管理工具类
 * 提供多语言支持，从语言文件中获取文本
 * 使用缓存机制优化 MessageFormat 性能
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
     * MessageFormat 缓存
     * 避免重复创建 MessageFormat 对象，提升性能
     * 使用 ConcurrentHashMap 保证线程安全
     */
    private static final ConcurrentHashMap<String, MessageFormat> formatCache = new ConcurrentHashMap<>(64);

    /**
     * 语言文本缓存
     * 缓存常用的语言文本，减少配置文件读取
     */
    private static final ConcurrentHashMap<String, String> textCache = new ConcurrentHashMap<>(128);

    /**
     * 缓存最大容量
     * 防止缓存无限增长
     */
    private static final int MAX_CACHE_SIZE = 512;

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
            LogUtil.error("Language system initialization failed: plugin instance is null");
            return;
        }

        langDir = new File(Main.plugin.getDataFolder(), "lang");
        if (!langDir.exists()) {
            langDir.mkdirs();
        }

        extractDefaultLangFile("zh_cn.yml");
        extractDefaultLangFile("en_us.yml");

        reload();

        initialized = true;

        LogUtil.debug("Language system initialized, current language: " + currentLang);
    }

    /**
     * 重新加载语言配置
     * 同时清空所有缓存
     */
    public static void reload() {
        formatCache.clear();
        textCache.clear();

        if (Main.config != null) {
            currentLang = Main.config.getString("Language", "zh_cn").toLowerCase();
        }

        File langFile = new File(langDir, currentLang + ".yml");

        if (!langFile.exists()) {
            LogUtil.warning("Language file not found: " + currentLang + ".yml, using default zh_cn");
            currentLang = "zh_cn";
            langFile = new File(langDir, "zh_cn.yml");
        }

        if (langFile.exists()) {
            langConfig = YamlConfiguration.loadConfiguration(langFile);
            LogUtil.debug("Language file loaded: " + langFile.getName());
        } else {
            LogUtil.error("Default language file not found, language system may not work properly");
            langConfig = new YamlConfiguration();
        }
    }

    /**
     * 从jar中释放默认语言文件
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
                LogUtil.debug("Language file extracted: " + fileName);
            } else {
                LogUtil.warning("Language file not found in jar: lang/" + fileName);
            }
        } catch (IOException e) {
            LogUtil.error("Failed to extract language file: " + fileName, e);
        }
    }

    /**
     * 获取语言文本（带缓存）
     *
     * @param key 语言键
     * @return 对应的文本，如果不存在返回键本身
     */
    @Nonnull
    public static String get(@Nonnull String key) {
        if (langConfig == null) {
            return key;
        }

        String cached = textCache.get(key);
        if (cached != null) {
            return cached;
        }

        String value = langConfig.getString(key);
        if (value == null) {
            return key;
        }

        if (textCache.size() < MAX_CACHE_SIZE) {
            textCache.put(key, value);
        }

        return value;
    }

    /**
     * 获取语言文本并替换占位符（带 MessageFormat 缓存）
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

        MessageFormat format = formatCache.get(key);
        if (format == null) {
            try {
                format = new MessageFormat(template);
                if (formatCache.size() < MAX_CACHE_SIZE) {
                    formatCache.put(key, format);
                }
            } catch (IllegalArgumentException e) {
                return fallbackFormat(template, args);
            }
        }

        try {
            return format.format(args);
        } catch (IllegalArgumentException e) {
            return fallbackFormat(template, args);
        }
    }

    /**
     * 备用格式化方法
     * 当 MessageFormat 失败时使用简单字符串替换
     *
     * @param template 模板字符串
     * @param args     参数数组
     * @return 格式化后的字符串
     */
    private static String fallbackFormat(String template, Object[] args) {
        String result = template;
        for (int i = 0; i < args.length; i++) {
            result = result.replace("{" + i + "}", String.valueOf(args[i]));
        }
        return result;
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

    /**
     * 清空所有缓存
     * 在内存紧张时可调用此方法
     */
    public static void clearCache() {
        formatCache.clear();
        textCache.clear();
    }

    /**
     * 获取当前缓存大小
     *
     * @return 缓存条目数量
     */
    public static int getCacheSize() {
        return formatCache.size() + textCache.size();
    }
}