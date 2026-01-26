// RainbowText.java - 彩虹文本处理器
package pers.tany.yukinoaapi.hologram;

import org.bukkit.ChatColor;

/**
 * 彩虹文本处理器
 * 实现 &u 彩虹颜色动画
 *
 * @author Roinflam
 * @since 3.1.0
 */
public class RainbowText {

    /** 彩虹颜色序列 */
    private static final ChatColor[] RAINBOW_COLORS = {
            ChatColor.RED,           // 红
            ChatColor.GOLD,          // 橙
            ChatColor.YELLOW,        // 黄
            ChatColor.GREEN,         // 绿
            ChatColor.AQUA,          // 青
            ChatColor.BLUE,          // 蓝
            ChatColor.LIGHT_PURPLE   // 紫
    };

    /** 当前颜色索引 */
    private int colorIndex = 0;

    /**
     * 检测文本是否包含彩虹标记
     */
    public static boolean hasRainbow(String text) {
        return text != null && text.contains("&u");
    }

    /**
     * 应用彩虹效果
     *
     * @param text 原始文本
     * @return 应用了彩虹色的文本
     */
    public String applyRainbow(String text) {
        if (text == null || !text.contains("&u")) {
            return ChatColor.translateAlternateColorCodes('&', text);
        }

        StringBuilder result = new StringBuilder();
        boolean inRainbow = false;
        ChatColor currentFormat = null;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            // 检测颜色代码
            if (c == '&' && i + 1 < text.length()) {
                char code = text.charAt(i + 1);

                if (code == 'u') {
                    // 开启彩虹模式
                    inRainbow = true;
                    i++; // 跳过 'u'
                    continue;
                } else if ("0123456789abcdefklmnor".indexOf(code) >= 0) {
                    // 遇到其他颜色代码，关闭彩虹模式
                    inRainbow = false;
                    currentFormat = null;

                    // 处理格式代码（加粗、斜体等）
                    if ("klmnor".indexOf(code) >= 0) {
                        result.append('§').append(code);
                        if (code != 'r') {
                            currentFormat = ChatColor.getByChar(code);
                        }
                    } else {
                        result.append('§').append(code);
                    }
                    i++; // 跳过颜色代码
                    continue;
                }
            }

            // 应用彩虹色
            if (inRainbow && c != ' ') {
                result.append(RAINBOW_COLORS[colorIndex % RAINBOW_COLORS.length]);
                if (currentFormat != null) {
                    result.append(currentFormat);
                }
                colorIndex++;
            }

            result.append(c);
        }

        return result.toString();
    }

    /**
     * 更新彩虹循环
     */
    public void tick() {
        colorIndex++;
        if (colorIndex >= RAINBOW_COLORS.length * 100) {
            colorIndex = 0; // 防止溢出
        }
    }

    /**
     * 重置颜色索引
     */
    public void reset() {
        colorIndex = 0;
    }
}