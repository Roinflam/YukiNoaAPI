package pers.tany.yukinoaapi.interfacepart.other;

import org.bukkit.ChatColor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;


/**
 * 字符串相关
 */
public interface IString {
    /**
     * 字符串replace，但只替换一次(无视转义)
     *
     * @param string    涉及的字符串
     * @param oldString 被替换的字符串
     * @param newString 替换后的字符串
     * @return 替换后的结果
     */
    
    static String replaceFirst(String string, String oldString, String newString) {
        string = string.replace("[", "%前括号%").replace("]", "%后括号%").replace("+", "%加号%").replace("-", "%减号%").replace("$", "%钱号%");
        oldString = oldString.replace("[", "%前括号%").replace("]", "%后括号%").replace("+", "%加号%").replace("-", "%减号%").replace("$", "%钱号%");
        newString = newString.replace("[", "%前括号%").replace("]", "%后括号%").replace("+", "%加号%").replace("-", "%减号%").replace("$", "%钱号%");
        return string.replaceFirst(oldString, newString).replace("%前括号%", "[").replace("%后括号%", "]").replace("%加号%", "+").replace("%减号%", "-").replace("%钱号%", "$");
    }

    /**
     * 同translateAlternateColorCodes
     *
     * @param message 被替换的字符串
     * @return 替换后的结果
     */

    static String color(@Nullable String message) {
        return message == null ? null : ChatColor.translateAlternateColorCodes('&', message);
    }

    /**
     * 去除字符串里的所有颜色
     *
     * @param message 需要去除的字符串
     * @return 去除后的结果
     */
    static String strip(@Nullable String message) {
        return message == null ? null : ChatColor.stripColor(message);
    }
}

