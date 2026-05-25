package pers.tany.yukinoaapi.realizationpart.builder;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import pers.tany.yukinoaapi.interfacepart.builder.IItemBuilder;
import pers.tany.yukinoaapi.interfacepart.item.IItem;
import pers.tany.yukinoaapi.interfacepart.other.IList;
import pers.tany.yukinoaapi.interfacepart.other.IString;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 物品建造器
 * 快速设置物品各项属性，支持链式调用
 *
 * <p>版本兼容性：</p>
 * <ul>
 *     <li>1.7.x - 1.12.x：使用 durability 设置附加值</li>
 *     <li>1.13+：durability 已废弃，仅用于工具耐久</li>
 * </ul>
 *
 * <p>parseMaterial 已优化为不依赖 NMSUtil 的版本判断，
 * 直接通过尝试 Material.valueOf 与 LEGACY_ 前缀双路径解析，
 * 兼容新版 Paper（1.21+）的版本字符串变化。</p>
 *
 * @author tany
 * @since 1.0.0
 */
public class ItemBuilder implements IItemBuilder {

    /**
     * 正在构建的物品
     */
    @Nullable
    private ItemStack itemStack;

    /**
     * 物品元数据
     */
    private final ItemMeta itemMeta;

    /**
     * 以物品 type，附加值为 0 创建一个堆叠数量为 1 的物品建造模式对象
     *
     * @param material 物品的 type
     */
    public ItemBuilder(Material material) {
        this.itemStack = new ItemStack(material);
        this.itemMeta = this.itemStack.getItemMeta();
    }

    /**
     * 以物品 type，附加值为 0 创建一个物品建造模式对象
     *
     * @param material 物品的 type
     * @param amount   物品的数量
     */
    public ItemBuilder(Material material, int amount) {
        this.itemStack = new ItemStack(material, amount);
        this.itemMeta = this.itemStack.getItemMeta();
    }

    /**
     * 以物品 type，附加值创建一个物品建造模式对象
     *
     * @param material   物品的 type
     * @param amount     物品的数量
     * @param durability 物品的附加值
     */
    @SuppressWarnings("deprecation")
    public ItemBuilder(Material material, int amount, short durability) {
        this.itemStack = new ItemStack(material, amount, durability);
        this.itemMeta = this.itemStack.getItemMeta();
    }

    /**
     * 以物品 type 名称，附加值为 0 创建一个堆叠数量为 1 的物品建造模式对象
     *
     * @param type 物品的 type 名称
     */
    public ItemBuilder(String type) {
        this.itemStack = new ItemStack(parseMaterial(type));
        this.itemMeta = this.itemStack.getItemMeta();
    }

    /**
     * 以物品 type 名称，附加值为 0 创建一个物品建造模式对象
     *
     * @param type   物品的 type 名称
     * @param amount 物品的数量
     */
    public ItemBuilder(String type, int amount) {
        this.itemStack = new ItemStack(parseMaterial(type), amount);
        this.itemMeta = this.itemStack.getItemMeta();
    }

    /**
     * 以物品 type 名称，附加值创建一个物品建造模式对象
     *
     * @param type       物品的 type
     * @param amount     物品的数量
     * @param durability 物品的附加值
     */
    @SuppressWarnings("deprecation")
    public ItemBuilder(String type, int amount, short durability) {
        this.itemStack = new ItemStack(parseMaterial(type), amount, durability);
        this.itemMeta = this.itemStack.getItemMeta();
    }

    /**
     * 以传入的物品创建一个物品建造模式对象
     *
     * @param itemStack 物品
     */
    public ItemBuilder(ItemStack itemStack) {
        this.itemStack = itemStack.clone();
        this.itemMeta = this.itemStack.getItemMeta();
    }

    /**
     * 解析物品类型名称
     * 优先尝试直接解析，失败则尝试 LEGACY_ 前缀（兼容 1.13+ 中的旧物品名）
     * 不再依赖 NMSUtil 的版本字符串判断，避免在新版 Paper 上误判。
     *
     * @param type 类型名称
     * @return Material 对象
     * @throws IllegalArgumentException 如果两种命名方式均无法解析
     */
    private static Material parseMaterial(String type) {
        // 先尝试直接解析（1.13+ 新名称 / 1.12 及以下原始名称）
        try {
            return Material.valueOf(type);
        } catch (IllegalArgumentException e) {
            // 尝试 LEGACY_ 前缀（部分 1.13-1.20 版本兼容旧名称）
            try {
                return Material.valueOf("LEGACY_" + type);
            } catch (IllegalArgumentException ex) {
                // 两种方式都失败则抛出原始异常，便于定位问题
                throw e;
            }
        }
    }

    @Nullable
    @Override
    public String getNBT() {
        return IItem.getItemNBT(itemStack);
    }

    @Nullable
    @Override
    public String getName() {
        return IItem.getName(itemStack);
    }

    @Nonnull
    @Override
    public ItemBuilder setDisplayName(String name) {
        itemMeta.setDisplayName(IString.color(name));
        return this;
    }

    @Override
    @SuppressWarnings("deprecation")
    public short getDurability() {
        return itemStack != null ? itemStack.getDurability() : 0;
    }

    @Nonnull
    @Override
    @SuppressWarnings("deprecation")
    public ItemBuilder setDurability(short durability) {
        if (itemStack != null) {
            itemStack.setDurability(durability);
        }
        return this;
    }

    @Override
    public int getAmount() {
        return itemStack != null ? itemStack.getAmount() : 0;
    }

    @Nonnull
    @Override
    public ItemBuilder setAmount(int amount) {
        if (amount > 0) {
            if (itemStack != null) {
                itemStack.setAmount(amount);
            }
        } else {
            itemStack = null;
        }
        return this;
    }

    @Override
    public Material getType() {
        return itemStack != null ? itemStack.getType() : Material.AIR;
    }

    @Nonnull
    @Override
    public ItemBuilder addLore(String... insertLore) {
        List<String> lore = getLore();
        lore.addAll(Arrays.asList(insertLore));
        itemMeta.setLore(IList.listReplace(lore, "&", "§"));
        return this;
    }

    @Nonnull
    @Override
    public ItemBuilder addLore(int index, String... insertLore) {
        List<String> lore = getLore();
        lore.addAll(index, Arrays.asList(insertLore));
        itemMeta.setLore(IList.listReplace(lore, "&", "§"));
        return this;
    }

    @Nonnull
    @Override
    public ItemBuilder addLoreAll(List<String> l) {
        List<String> lore = getLore();
        lore.addAll(l);
        itemMeta.setLore(IList.listReplace(lore, "&", "§"));
        return this;
    }

    @Nonnull
    @Override
    public ItemBuilder addLoreAll(int index, List<String> l) {
        List<String> lore = getLore();
        lore.addAll(index, l);
        itemMeta.setLore(IList.listReplace(lore, "&", "§"));
        return this;
    }

    @Nonnull
    @Override
    public ItemBuilder setLore(List<String> lore) {
        itemMeta.setLore(IList.listReplace(lore, "&", "§"));
        return this;
    }

    @Nonnull
    @Override
    public ItemBuilder setLore(int index, String l) {
        List<String> list = getLore();
        if (index >= 0 && index < list.size()) {
            list.set(index, l);
            setLore(list);
        }
        return this;
    }

    @Override
    public List<String> getLore() {
        return itemMeta.hasLore() ? new ArrayList<>(itemMeta.getLore()) : new ArrayList<>();
    }

    @Nonnull
    @Override
    public ItemBuilder clearLore() {
        itemMeta.setLore(new ArrayList<>());
        return this;
    }

    // ==================== 附魔相关方法 ====================

    /**
     * 添加附魔到物品
     *
     * @param enchantment 附魔类型
     * @param level       附魔等级
     * @return 当前建造器实例
     */
    @Nonnull
    public ItemBuilder addEnchant(@Nonnull Enchantment enchantment, int level) {
        itemMeta.addEnchant(enchantment, level, true);
        return this;
    }

    /**
     * 添加附魔到物品
     *
     * @param enchantment            附魔类型
     * @param level                  附魔等级
     * @param ignoreLevelRestriction 是否忽略等级限制
     * @return 当前建造器实例
     */
    @Nonnull
    public ItemBuilder addEnchant(@Nonnull Enchantment enchantment, int level, boolean ignoreLevelRestriction) {
        itemMeta.addEnchant(enchantment, level, ignoreLevelRestriction);
        return this;
    }

    /**
     * 批量添加附魔
     *
     * @param enchantments 附魔映射（附魔类型 -> 等级）
     * @return 当前建造器实例
     */
    @Nonnull
    public ItemBuilder addEnchants(@Nonnull Map<Enchantment, Integer> enchantments) {
        for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
            itemMeta.addEnchant(entry.getKey(), entry.getValue(), true);
        }
        return this;
    }

    /**
     * 批量添加附魔
     *
     * @param enchantments           附魔映射（附魔类型 -> 等级）
     * @param ignoreLevelRestriction 是否忽略等级限制
     * @return 当前建造器实例
     */
    @Nonnull
    public ItemBuilder addEnchants(@Nonnull Map<Enchantment, Integer> enchantments, boolean ignoreLevelRestriction) {
        for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
            itemMeta.addEnchant(entry.getKey(), entry.getValue(), ignoreLevelRestriction);
        }
        return this;
    }

    /**
     * 移除指定附魔
     *
     * @param enchantment 要移除的附魔类型
     * @return 当前建造器实例
     */
    @Nonnull
    public ItemBuilder removeEnchant(@Nonnull Enchantment enchantment) {
        itemMeta.removeEnchant(enchantment);
        return this;
    }

    /**
     * 清除所有附魔
     *
     * @return 当前建造器实例
     */
    @Nonnull
    public ItemBuilder clearEnchants() {
        for (Enchantment enchantment : itemMeta.getEnchants().keySet()) {
            itemMeta.removeEnchant(enchantment);
        }
        return this;
    }

    /**
     * 检查是否有指定附魔
     *
     * @param enchantment 附魔类型
     * @return 如果有该附魔返回 true
     */
    public boolean hasEnchant(@Nonnull Enchantment enchantment) {
        return itemMeta.hasEnchant(enchantment);
    }

    /**
     * 获取指定附魔的等级
     *
     * @param enchantment 附魔类型
     * @return 附魔等级，如果没有该附魔返回 0
     */
    public int getEnchantLevel(@Nonnull Enchantment enchantment) {
        return itemMeta.getEnchantLevel(enchantment);
    }

    /**
     * 获取所有附魔
     *
     * @return 附魔映射（附魔类型 -> 等级）
     */
    @Nonnull
    public Map<Enchantment, Integer> getEnchants() {
        return itemMeta.getEnchants();
    }

    /**
     * 检查是否有任何附魔
     *
     * @return 如果有附魔返回 true
     */
    public boolean hasEnchants() {
        return itemMeta.hasEnchants();
    }

    /**
     * 添加物品标志（1.8+）
     * 用于隐藏附魔、属性等信息
     *
     * @param flags 物品标志
     * @return 当前建造器实例
     */
    @Nonnull
    public ItemBuilder addItemFlags(@Nonnull ItemFlag... flags) {
        try {
            itemMeta.addItemFlags(flags);
        } catch (NoSuchMethodError e) {
            // 1.7 不支持 ItemFlag
        }
        return this;
    }

    /**
     * 移除物品标志（1.8+）
     *
     * @param flags 物品标志
     * @return 当前建造器实例
     */
    @Nonnull
    public ItemBuilder removeItemFlags(@Nonnull ItemFlag... flags) {
        try {
            itemMeta.removeItemFlags(flags);
        } catch (NoSuchMethodError e) {
            // 1.7 不支持 ItemFlag
        }
        return this;
    }

    /**
     * 隐藏所有物品信息（附魔、属性、破坏标志等）
     * 需要 1.8+ 版本支持
     *
     * @return 当前建造器实例
     */
    @Nonnull
    public ItemBuilder hideAllFlags() {
        try {
            itemMeta.addItemFlags(ItemFlag.values());
        } catch (NoSuchMethodError e) {
            // 1.7 不支持 ItemFlag
        }
        return this;
    }

    /**
     * 设置物品为无法破坏（1.8+）
     *
     * @param unbreakable 是否无法破坏
     * @return 当前建造器实例
     */
    @Nonnull
    public ItemBuilder setUnbreakable(boolean unbreakable) {
        try {
            itemMeta.setUnbreakable(unbreakable);
        } catch (NoSuchMethodError e) {
            // 1.7 不支持 Unbreakable
        }
        return this;
    }

    @Nullable
    @Override
    public ItemStack getItemStack() {
        if (itemStack != null) {
            itemStack.setItemMeta(itemMeta);
        }
        return itemStack;
    }

    /**
     * 构建物品并返回克隆副本
     * 可以继续使用此建造器构建更多物品
     *
     * @return 物品副本，如果物品为空返回 null
     */
    @Nullable
    public ItemStack build() {
        ItemStack result = getItemStack();
        return result != null ? result.clone() : null;
    }

    /**
     * 克隆当前建造器
     *
     * @return 新的建造器实例
     */
    @Nonnull
    public ItemBuilder clone() {
        ItemStack item = getItemStack();
        if (item != null) {
            return new ItemBuilder(item);
        }
        return new ItemBuilder(Material.AIR);
    }
}
