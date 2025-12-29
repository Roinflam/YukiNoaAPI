package pers.tany.yukinoaapi.interfacepart.builder;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import pers.tany.yukinoaapi.realizationpart.builder.ItemBuilder;

import javax.annotation.Nullable;
import java.util.List;

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

    /**
     * 获得被建造模式对象修改后的物品
     *
     * @return 修改后的物品
     */
    @Nullable
    ItemStack getItemStack();
}
