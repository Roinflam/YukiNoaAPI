package pers.tany.yukinoaapi.interfacepart.builder;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import pers.tany.yukinoaapi.realizationpart.builder.ItemBuilder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

/**
 * 快速设置物品各项属性接口
 */
public interface IItemBuilder {

    /**
     * 获取物品的NBT
     *
     * @return 物品的NBT
     */
    @Nullable
    String getNBT();

    /**
     * 返回物品名称
     * 优先自定义，其次翻译后的名称
     * 若翻译文本有误，则使用Material枚举名进行代替
     * PS:如原版物品名称、IC2模组物品名称
     *
     * @return 物品名称
     */
    @Nullable
    String getName();

    /**
     * 设置这个物品的名称(支持颜色代码)
     *
     * @param name 名称
     * @return 修改后的对象
     */
    ItemBuilder setDisplayName(String name);

    /**
     * 获取该物品的耐久度
     *
     * @return 物品耐久度
     */
    short getDurability();

    /**
     * 设置该物品的耐久度
     *
     * @param durability 耐久度
     * @return 修改后的对象
     */
    ItemBuilder setDurability(short durability);

    /**
     * 获取该物品堆的物品堆叠数量
     *
     * @return 堆叠数量
     */
    int getAmount();

    /**
     * 设置该物品堆的物品堆叠数量.
     *
     * @param amount 堆叠数量
     * @return 修改后的对象
     */
    ItemBuilder setAmount(int amount);

    /**
     * 获取该物品的种类
     *
     * @return 物品的种类
     */
    Material getType();

    /**
     * 添加一行这个物品的lore(支持颜色代码)
     *
     * @param insertLore 添加的lore
     * @return 修改后的对象
     */
    ItemBuilder addLore(String... insertLore);

    /**
     * 在索引...处添加一行这个物品的lore(支持颜色代码)
     *
     * @param index      索引位置
     * @param insertLore 添加的lore
     * @return 修改后的对象
     */
    ItemBuilder addLore(int index, String... insertLore);

    /**
     * 添加list到这个物品的lore(支持颜色代码)
     *
     * @param l 添加的字符串list
     * @return 修改后的对象
     */
    ItemBuilder addLoreAll(List<String> l);

    /**
     * 在索引...处添加list到这个物品的lore(支持颜色代码)
     *
     * @param index 索引位置
     * @param l     添加的字符串list
     * @return 修改后的对象
     */
    ItemBuilder addLoreAll(int index, List<String> l);

    /**
     * 设置这个物品的lore
     *
     * @param lore 设置的lore
     * @return 修改后的对象
     */
    ItemBuilder setLore(List<String> lore);

    /**
     * 设置这个物品lore的这个位置的文字
     *
     * @param index 索引位置
     * @param l     设置的lore
     * @return 修改后的对象
     */
    ItemBuilder setLore(int index, String l);

    /**
     * 获取这个物品的Lore
     *
     * @return 物品的Lore
     */
    List<String> getLore();

    /**
     * 清除所有lore
     *
     * @return 修改后的对象
     */
    ItemBuilder clearLore();

    // ==================== 附魔相关方法 ====================

    /**
     * 添加附魔到物品
     *
     * @param enchantment 附魔类型
     * @param level       附魔等级
     * @return 修改后的对象
     */
    ItemBuilder addEnchant(@Nonnull Enchantment enchantment, int level);

    /**
     * 添加附魔到物品
     *
     * @param enchantment            附魔类型
     * @param level                  附魔等级
     * @param ignoreLevelRestriction 是否忽略等级限制
     * @return 修改后的对象
     */
    ItemBuilder addEnchant(@Nonnull Enchantment enchantment, int level, boolean ignoreLevelRestriction);

    /**
     * 批量添加附魔
     *
     * @param enchantments 附魔映射（附魔类型 -> 等级）
     * @return 修改后的对象
     */
    ItemBuilder addEnchants(@Nonnull Map<Enchantment, Integer> enchantments);

    /**
     * 批量添加附魔
     *
     * @param enchantments           附魔映射（附魔类型 -> 等级）
     * @param ignoreLevelRestriction 是否忽略等级限制
     * @return 修改后的对象
     */
    ItemBuilder addEnchants(@Nonnull Map<Enchantment, Integer> enchantments, boolean ignoreLevelRestriction);

    /**
     * 移除指定附魔
     *
     * @param enchantment 要移除的附魔类型
     * @return 修改后的对象
     */
    ItemBuilder removeEnchant(@Nonnull Enchantment enchantment);

    /**
     * 清除所有附魔
     *
     * @return 修改后的对象
     */
    ItemBuilder clearEnchants();

    /**
     * 检查是否有指定附魔
     *
     * @param enchantment 附魔类型
     * @return 如果有该附魔返回 true
     */
    boolean hasEnchant(@Nonnull Enchantment enchantment);

    /**
     * 获取指定附魔的等级
     *
     * @param enchantment 附魔类型
     * @return 附魔等级，如果没有该附魔返回 0
     */
    int getEnchantLevel(@Nonnull Enchantment enchantment);

    /**
     * 获取所有附魔
     *
     * @return 附魔映射（附魔类型 -> 等级）
     */
    @Nonnull
    Map<Enchantment, Integer> getEnchants();

    /**
     * 检查是否有任何附魔
     *
     * @return 如果有附魔返回 true
     */
    boolean hasEnchants();

    /**
     * 添加物品标志（1.8+）
     * 用于隐藏附魔、属性等信息
     *
     * @param flags 物品标志
     * @return 修改后的对象
     */
    ItemBuilder addItemFlags(@Nonnull ItemFlag... flags);

    /**
     * 移除物品标志（1.8+）
     *
     * @param flags 物品标志
     * @return 修改后的对象
     */
    ItemBuilder removeItemFlags(@Nonnull ItemFlag... flags);

    /**
     * 隐藏所有物品信息（附魔、属性、破坏标志等）
     * 需要 1.8+ 版本支持
     *
     * @return 修改后的对象
     */
    ItemBuilder hideAllFlags();

    /**
     * 设置物品为无法破坏（1.8+）
     *
     * @param unbreakable 是否无法破坏
     * @return 修改后的对象
     */
    ItemBuilder setUnbreakable(boolean unbreakable);

    /**
     * 获得被建造模式对象修改后的物品
     *
     * @return 修改后的物品
     */
    @Nullable
    ItemStack getItemStack();

    /**
     * 构建物品并返回克隆副本
     * 可以继续使用此建造器构建更多物品
     *
     * @return 物品副本，如果物品为空返回 null
     */
    @Nullable
    ItemStack build();

    /**
     * 克隆当前建造器
     *
     * @return 新的建造器实例
     */
    @Nonnull
    ItemBuilder clone();
}