package pers.tany.yukinoaapi.realizationpart.builder;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import pers.tany.yukinoaapi.interfacepart.builder.IItemBuilder;
import pers.tany.yukinoaapi.interfacepart.item.IItem;
import pers.tany.yukinoaapi.interfacepart.other.IList;
import pers.tany.yukinoaapi.interfacepart.other.IString;
import pers.tany.yukinoaapi.util.NMSUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
     * 1.13+ 优先使用新名称，失败则尝试 LEGACY_ 前缀
     *
     * @param type 类型名称
     * @return Material 对象
     * @throws IllegalArgumentException 如果类型名称无效
     */
    private static Material parseMaterial(String type) {
        // 先尝试直接解析
        try {
            return Material.valueOf(type);
        } catch (IllegalArgumentException e) {
            // 1.13+ 尝试 LEGACY_ 前缀
            if (NMSUtil.isVersionAtLeast(13)) {
                return Material.valueOf("LEGACY_" + type);
            }
            throw e;
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
}