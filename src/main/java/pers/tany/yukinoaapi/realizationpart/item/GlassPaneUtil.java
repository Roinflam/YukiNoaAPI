// 文件：GlassPaneUtil.java
// 路径：src/main/java/pers/tany/yukinoaapi/realizationpart/item/GlassPaneUtil.java
package pers.tany.yukinoaapi.realizationpart.item;

import org.bukkit.DyeColor;
import org.bukkit.inventory.ItemStack;
import pers.tany.yukinoaapi.interfacepart.builder.IItemBuilder;
import pers.tany.yukinoaapi.realizationpart.builder.ItemBuilder;
import pers.tany.yukinoaapi.util.NMSUtil;

import javax.annotation.Nonnull;

/**
 * 跨版本染色玻璃板工具类
 * 兼容 1.7-1.21+ 所有版本
 *
 * <p>版本差异：</p>
 * <ul>
 *     <li>1.12 及以下：使用 STAINED_GLASS_PANE + 子ID（0-15）</li>
 *     <li>1.13+：使用独立的 Material（WHITE_STAINED_GLASS_PANE 等）</li>
 * </ul>
 *
 * <p>颜色对照表：</p>
 * <pre>
 * 0 = 白色    4 = 黄色    8 = 淡灰色   12 = 棕色
 * 1 = 橙色    5 = 黄绿色  9 = 青色     13 = 绿色
 * 2 = 品红色  6 = 粉色    10 = 紫色    14 = 红色
 * 3 = 淡蓝色  7 = 灰色    11 = 蓝色    15 = 黑色
 * </pre>
 *
 * @author tany
 * @since 1.0.0
 */
public final class GlassPaneUtil {

    /**
     * 1.13+ 版本的染色玻璃板 Material 名称（按子ID顺序排列）
     */
    private static final String[] GLASS_PANE_NAMES = {
            "WHITE_STAINED_GLASS_PANE",       // 0
            "ORANGE_STAINED_GLASS_PANE",      // 1
            "MAGENTA_STAINED_GLASS_PANE",     // 2
            "LIGHT_BLUE_STAINED_GLASS_PANE",  // 3
            "YELLOW_STAINED_GLASS_PANE",      // 4
            "LIME_STAINED_GLASS_PANE",        // 5
            "PINK_STAINED_GLASS_PANE",        // 6
            "GRAY_STAINED_GLASS_PANE",        // 7
            "LIGHT_GRAY_STAINED_GLASS_PANE",  // 8
            "CYAN_STAINED_GLASS_PANE",        // 9
            "PURPLE_STAINED_GLASS_PANE",      // 10
            "BLUE_STAINED_GLASS_PANE",        // 11
            "BROWN_STAINED_GLASS_PANE",       // 12
            "GREEN_STAINED_GLASS_PANE",       // 13
            "RED_STAINED_GLASS_PANE",         // 14
            "BLACK_STAINED_GLASS_PANE"        // 15
    };

    /**
     * 是否为高版本（1.13+）
     */
    private static final boolean IS_HIGH_VERSION = NMSUtil.isVersionAtLeast(13);

    /**
     * 私有构造函数，防止实例化
     */
    private GlassPaneUtil() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }

    /**
     * 获取指定颜色的染色玻璃板物品
     *
     * @param colorId 颜色ID（0-15），对应 DyeColor 的顺序
     * @return 玻璃板物品建造器
     * @throws IllegalArgumentException 如果颜色ID不在 0-15 范围内
     *
     * @example
     * <pre>{@code
     * // 获取黑色玻璃板作为 GUI 背景
     * ItemStack background = GlassPaneUtil.getGlassPane(15)
     *     .setDisplayName(" ")
     *     .getItemStack();
     *
     * // 填充整个 GUI
     * for (int i = 0; i < 54; i++) {
     *     inventory.setItem(i, background);
     * }
     * }</pre>
     */
    @Nonnull
    public static IItemBuilder getGlassPane(int colorId) {
        // 参数校验
        if (colorId < 0 || colorId > 15) {
            throw new IllegalArgumentException("颜色ID必须在 0-15 范围内，当前值：" + colorId);
        }

        if (IS_HIGH_VERSION) {
            // 1.13+ 使用独立的 Material
            return new ItemBuilder(GLASS_PANE_NAMES[colorId]);
        } else {
            // 1.12 及以下使用 STAINED_GLASS_PANE + 子ID
            return new ItemBuilder("STAINED_GLASS_PANE", 1, (short) colorId);
        }
    }

    /**
     * 根据 DyeColor 获取染色玻璃板物品
     *
     * @param dyeColor 染料颜色枚举
     * @return 玻璃板物品建造器
     *
     * @example
     * <pre>{@code
     * // 使用枚举获取红色玻璃板
     * ItemStack red = GlassPaneUtil.getGlassPane(DyeColor.RED).getItemStack();
     * }</pre>
     */
    @Nonnull
    public static IItemBuilder getGlassPane(@Nonnull DyeColor dyeColor) {
        return getGlassPane(dyeColor.ordinal());
    }

    /**
     * 获取指定颜色的染色玻璃板 ItemStack
     * 便捷方法，直接返回物品而非建造器
     *
     * @param colorId 颜色ID（0-15）
     * @return 玻璃板物品
     */
    @Nonnull
    public static ItemStack getGlassPaneItem(int colorId) {
        return getGlassPane(colorId).getItemStack();
    }

    /**
     * 获取指定颜色的染色玻璃板 ItemStack
     * 便捷方法，直接返回物品而非建造器
     *
     * @param colorId 颜色ID（0-15）
     * @param amount  物品数量
     * @return 玻璃板物品
     */
    @Nonnull
    public static ItemStack getGlassPaneItem(int colorId, int amount) {
        return getGlassPane(colorId).setAmount(amount).getItemStack();
    }

    /**
     * 获取染色玻璃板（保留旧方法名，兼容旧代码）
     *
     * @param colorId 颜色ID（0-15）
     * @return 玻璃板物品建造器
     * @deprecated 请使用 {@link #getGlassPane(int)} 代替，方法名更规范
     */
    @Deprecated
    @Nonnull
    public static IItemBuilder getStainedGlass(int colorId) {
        return getGlassPane(colorId);
    }
}