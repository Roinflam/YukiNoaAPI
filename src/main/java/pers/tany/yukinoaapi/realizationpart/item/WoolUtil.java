package pers.tany.yukinoaapi.realizationpart.item;

import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import pers.tany.yukinoaapi.interfacepart.builder.IItemBuilder;
import pers.tany.yukinoaapi.realizationpart.builder.ItemBuilder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * 跨版本羊毛工具类
 * 兼容 1.7-1.21+ 所有版本
 *
 * <p>版本差异：</p>
 * <ul>
 *     <li>1.12 及以下：使用 WOOL + 子ID（0-15）</li>
 *     <li>1.13+：使用独立的 Material（WHITE_WOOL、ORANGE_WOOL 等）</li>
 * </ul>
 *
 * <p>版本检测方式（已优化）：</p>
 * <p>本类不再依赖 NMSUtil 的版本字符串判断，改为在类加载时通过
 * Material 枚举存在性进行运行时检测，避免在新版 Paper（如 1.21+）
 * 上因服务端版本字符串格式变更导致的判断错误。</p>
 *
 * <p>颜色对照表：</p>
 * <pre>
 * 0 = 白色    4 = 黄色    8 = 淡灰色   12 = 棕色
 * 1 = 橙色    5 = 黄绿色  9 = 青色     13 = 绿色
 * 2 = 品红色  6 = 粉色    10 = 紫色    14 = 红色
 * 3 = 淡蓝色  7 = 灰色    11 = 蓝色    15 = 黑色
 * </pre>
 */
public final class WoolUtil {

    /**
     * 1.13+ 版本的羊毛 Material 名称（按子ID顺序排列）
     */
    private static final String[] WOOL_NAMES = {
            "WHITE_WOOL",       // 0
            "ORANGE_WOOL",      // 1
            "MAGENTA_WOOL",     // 2
            "LIGHT_BLUE_WOOL",  // 3
            "YELLOW_WOOL",      // 4
            "LIME_WOOL",        // 5
            "PINK_WOOL",        // 6
            "GRAY_WOOL",        // 7
            "LIGHT_GRAY_WOOL",  // 8
            "CYAN_WOOL",        // 9
            "PURPLE_WOOL",      // 10
            "BLUE_WOOL",        // 11
            "BROWN_WOOL",       // 12
            "GREEN_WOOL",       // 13
            "RED_WOOL",         // 14
            "BLACK_WOOL"        // 15
    };

    /**
     * 缓存解析后的 1.13+ 独立 Material（每种颜色一个）
     * 若当前服务端不支持新版枚举名，则对应槽位为 null
     */
    private static final Material[] RESOLVED_MATERIALS = new Material[16];

    /**
     * 旧版（1.12 及以下）使用的 WOOL Material
     * 若当前服务端不存在该枚举（如 1.21+），则为 null
     */
    private static final Material LEGACY_WOOL_MATERIAL;

    static {
        // 优先解析 1.13+ 的独立 Material
        for (int i = 0; i < 16; i++) {
            RESOLVED_MATERIALS[i] = tryGetMaterial(WOOL_NAMES[i]);
        }
        // 解析旧版 Material（1.12 及以下使用）
        LEGACY_WOOL_MATERIAL = tryGetMaterial("WOOL");
    }

    /**
     * 尝试通过名称获取 Material，自动尝试 LEGACY_ 前缀回退
     *
     * @param name Material 名称
     * @return Material 对象，如果不存在返回 null
     */
    @Nullable
    private static Material tryGetMaterial(@Nonnull String name) {
        try {
            return Material.valueOf(name);
        } catch (IllegalArgumentException e) {
            // 尝试 LEGACY_ 前缀（部分 1.13-1.20 版本兼容旧名称）
            try {
                return Material.valueOf("LEGACY_" + name);
            } catch (IllegalArgumentException ex) {
                return null;
            }
        }
    }

    /**
     * 私有构造函数，防止实例化
     */
    private WoolUtil() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }

    /**
     * 获取指定颜色的羊毛物品
     *
     * @param colorId 颜色ID（0-15），对应 DyeColor 的顺序
     * @return 羊毛物品建造器
     * @throws IllegalArgumentException 如果颜色ID不在 0-15 范围内
     * @throws IllegalStateException    如果当前服务端版本两种枚举都不存在（极少数情况）
     *
     * @example
     * <pre>{@code
     * // 获取白色羊毛
     * ItemStack white = WoolUtil.getWool(0).getItemStack();
     *
     * // 获取红色羊毛并设置名称
     * ItemStack red = WoolUtil.getWool(14)
     *     .setDisplayName("&c红色方块")
     *     .getItemStack();
     * }</pre>
     */
    @Nonnull
    public static IItemBuilder getWool(int colorId) {
        // 参数校验
        if (colorId < 0 || colorId > 15) {
            throw new IllegalArgumentException("颜色ID必须在 0-15 范围内，当前值：" + colorId);
        }

        // 优先使用 1.13+ 的独立 Material（新版 Paper 走这里）
        Material material = RESOLVED_MATERIALS[colorId];
        if (material != null) {
            return new ItemBuilder(material);
        }

        // 回退到 1.12 及以下的 WOOL + 子ID
        if (LEGACY_WOOL_MATERIAL != null) {
            return new ItemBuilder(LEGACY_WOOL_MATERIAL, 1, (short) colorId);
        }

        // 极少数情况：当前服务端两种枚举都不存在
        throw new IllegalStateException(
                "当前服务端版本不支持羊毛（colorId=" + colorId + "），" +
                        "请检查服务端版本与 Material 枚举是否完整");
    }

    /**
     * 根据 DyeColor 获取羊毛物品
     *
     * @param dyeColor 染料颜色枚举
     * @return 羊毛物品建造器
     *
     * @example
     * <pre>{@code
     * // 使用枚举获取蓝色羊毛
     * ItemStack blue = WoolUtil.getWool(DyeColor.BLUE).getItemStack();
     * }</pre>
     */
    @Nonnull
    public static IItemBuilder getWool(@Nonnull DyeColor dyeColor) {
        return getWool(dyeColor.ordinal());
    }

    /**
     * 获取指定颜色的羊毛 ItemStack
     * 便捷方法，直接返回物品而非建造器
     *
     * @param colorId 颜色ID（0-15）
     * @return 羊毛物品
     */
    @Nonnull
    public static ItemStack getWoolItem(int colorId) {
        return getWool(colorId).getItemStack();
    }

    /**
     * 获取指定颜色的羊毛 ItemStack
     * 便捷方法，直接返回物品而非建造器
     *
     * @param colorId 颜色ID（0-15）
     * @param amount  物品数量
     * @return 羊毛物品
     */
    @Nonnull
    public static ItemStack getWoolItem(int colorId, int amount) {
        return getWool(colorId).setAmount(amount).getItemStack();
    }
}
