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
 */
public final class LangUtil {

    private static YamlConfiguration langConfig;
    private static String currentLang = "zh_cn";
    private static File langDir;

    private LangUtil() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }

    /**
     * 初始化语言系统
     */
    public static void init() {
        if (Main.plugin == null) {
            LogUtil.error("语言系统初始化失败: 插件实例为空");
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

        LogUtil.debug("语言系统初始化完成，当前语言: " + currentLang);
    }

    /**
     * 重新加载语言配置
     */
    public static void reload() {
        if (Main.config != null) {
            currentLang = Main.config.getString("Language", "zh_cn").toLowerCase();
        }

        File langFile = new File(langDir, currentLang + ".yml");

        if (!langFile.exists()) {
            LogUtil.warning("语言文件不存在: " + currentLang + ".yml，使用默认语言 zh_cn");
            currentLang = "zh_cn";
            langFile = new File(langDir, "zh_cn.yml");
        }

        if (langFile.exists()) {
            langConfig = YamlConfiguration.loadConfiguration(langFile);
            LogUtil.debug("已加载语言文件: " + langFile.getName());
        } else {
            LogUtil.error("默认语言文件也不存在，语言系统可能无法正常工作");
            langConfig = new YamlConfiguration();
        }
    }

    /**
     * 从jar中释放默认语言文件
     */
    private static void extractDefaultLangFile(String fileName) {
        File targetFile = new File(langDir, fileName);

        if (targetFile.exists()) {
            return;
        }

        // 注意这里的路径是 lang/文件名
        try (InputStream inputStream = Main.class.getClassLoader().getResourceAsStream("lang/" + fileName)) {
            if (inputStream != null) {
                Files.copy(inputStream, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                LogUtil.debug("已释放语言文件: " + fileName);
            } else {
                LogUtil.warning("在jar中未找到语言文件: lang/" + fileName);
            }
        } catch (IOException e) {
            LogUtil.error("释放语言文件失败: " + fileName, e);
        }
    }

    /**
     * 获取语言文本
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
            String result = template;
            for (int i = 0; i < args.length; i++) {
                result = result.replace("{" + i + "}", String.valueOf(args[i]));
            }
            return result;
        }
    }

    /**
     * 获取带前缀的语言文本
     */
    @Nonnull
    public static String getWithPrefix(@Nonnull String key) {
        return get("common.prefix") + get(key);
    }

    /**
     * 获取带前缀的语言文本并替换占位符
     */
    @Nonnull
    public static String getWithPrefix(@Nonnull String key, Object... args) {
        return get("common.prefix") + get(key, args);
    }

    /**
     * 检查语言键是否存在
     */
    public static boolean has(@Nonnull String key) {
        return langConfig != null && langConfig.contains(key);
    }

    /**
     * 获取当前语言代码
     */
    @Nonnull
    public static String getCurrentLang() {
        return currentLang;
    }

    /**
     * 设置当前语言
     */
    public static void setCurrentLang(@Nonnull String lang) {
        currentLang = lang.toLowerCase();
        reload();
    }
}