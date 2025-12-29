package pers.tany.yukinoaapi.realizationpart.builder;

import org.bukkit.Material;
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
import java.util.Objects;

/**
 * 快速设置物品各项属性
 */
public class ItemBuilder implements IItemBuilder {
    private final ItemMeta itemMeta;
    @Nullable
    private ItemStack itemStack;

    /**
     * 以物品主ID，附加值为0创建一个堆叠数量为1的物品建造模式对象
     *
     * @param id 物品的主id
     */
    public ItemBuilder(int id) {
        ItemStack itemStack;
        try {
            itemStack = new ItemStack(id);
        } catch (IllegalArgumentException illegalArgumentException) {
            itemStack = new ItemStack(id);
        }
        Objects.requireNonNull(itemStack);
        itemStack = itemStack.clone();

        this.itemStack = itemStack.clone();
        this.itemMeta = itemStack.clone().getItemMeta();
    }

    /**
     * 以物品主ID，附加值为0创建一个物品建造模式对象
     *
     * @param id     物品的主id
     * @param amount 物品的数量
     */
    public ItemBuilder(int id, int amount) {
        ItemStack itemStack;
        try {
            itemStack = new ItemStack(id, amount);
        } catch (IllegalArgumentException illegalArgumentException) {
            itemStack = new ItemStack(id, amount);
        }
        Objects.requireNonNull(itemStack);
        itemStack = itemStack.clone();

        this.itemStack = itemStack.clone();
        this.itemMeta = itemStack.clone().getItemMeta();
    }

    /**
     * 以物品主ID，附加值创建一个物品建造模式对象
     *
     * @param id         物品的主id
     * @param amount     物品的数量
     * @param durability 物品的附加值
     */
    public ItemBuilder(int id, int amount, short durability) {
        ItemStack itemStack;
        try {
            itemStack = new ItemStack(id, amount, durability);
        } catch (IllegalArgumentException illegalArgumentException) {
            itemStack = new ItemStack(id, amount, durability);
        }
        Objects.requireNonNull(itemStack, "物品为空！");
        itemStack = itemStack.clone();

        this.itemStack = itemStack.clone();
        this.itemMeta = itemStack.clone().getItemMeta();
    }

    /**
     * 以物品type，附加值为0创建一个堆叠数量为1的物品建造模式对象
     *
     * @param material 物品的type
     */
    public ItemBuilder(Material material) {
        ItemStack itemStack;
        try {
            itemStack = new ItemStack(material);
        } catch (IllegalArgumentException illegalArgumentException) {
            itemStack = new ItemStack(material);
        }
        Objects.requireNonNull(itemStack);
        itemStack = itemStack.clone();

        this.itemStack = itemStack.clone();
        this.itemMeta = itemStack.clone().getItemMeta();
    }

    /**
     * 以物品type，附加值为0创建一个物品建造模式对象
     *
     * @param material 物品的type
     * @param amount   物品的数量
     */
    public ItemBuilder(Material material, int amount) {
        ItemStack itemStack;
        try {
            itemStack = new ItemStack(material, amount);
        } catch (IllegalArgumentException illegalArgumentException) {
            itemStack = new ItemStack(material, amount);
        }
        Objects.requireNonNull(itemStack);
        itemStack = itemStack.clone();

        this.itemStack = itemStack.clone();
        this.itemMeta = itemStack.clone().getItemMeta();
    }

    /**
     * 以物品type，附加值创建一个物品建造模式对象
     *
     * @param material   物品的type
     * @param amount     物品的数量
     * @param durability 物品的附加值
     */
    public ItemBuilder(Material material, int amount, short durability) {
        ItemStack itemStack;
        try {
            itemStack = new ItemStack(material, amount, durability);
        } catch (IllegalArgumentException illegalArgumentException) {
            itemStack = new ItemStack(material, amount, durability);
        }
        Objects.requireNonNull(itemStack);
        itemStack = itemStack.clone();

        this.itemStack = itemStack.clone();
        this.itemMeta = itemStack.clone().getItemMeta();
    }

    /**
     * 以物品type名称，附加值为0创建一个堆叠数量为1的物品建造模式对象
     *
     * @param type 物品的type名称
     */
    public ItemBuilder(String type) {
        ItemStack itemStack;
        try {
            itemStack = new ItemStack(Material.valueOf(type));
        } catch (IllegalArgumentException illegalArgumentException) {
            itemStack = new ItemStack(Material.valueOf("LEGACY_" + type));
        }
        Objects.requireNonNull(itemStack);
        itemStack = itemStack.clone();

        this.itemStack = itemStack.clone();
        this.itemMeta = itemStack.clone().getItemMeta();
    }

    /**
     * 以物品type名称，附加值为0创建一个物品建造模式对象
     *
     * @param type   物品的type名称
     * @param amount 物品的数量
     */
    public ItemBuilder(String type, int amount) {
        ItemStack itemStack;
        try {
            itemStack = new ItemStack(Material.valueOf(type), amount);
        } catch (IllegalArgumentException illegalArgumentException) {
            itemStack = new ItemStack(Material.valueOf("LEGACY_" + type), amount);
        }
        Objects.requireNonNull(itemStack);
        itemStack = itemStack.clone();

        this.itemStack = itemStack.clone();
        this.itemMeta = itemStack.clone().getItemMeta();
    }

    /**
     * 以物品type名称，附加值创建一个物品建造模式对象
     *
     * @param type       物品的type
     * @param amount     物品的数量
     * @param durability 物品的附加值
     */
    public ItemBuilder(String type, int amount, short durability) {
        ItemStack itemStack;
        try {
            itemStack = new ItemStack(Material.valueOf(type), amount, durability);
        } catch (IllegalArgumentException illegalArgumentException) {
            itemStack = new ItemStack(Material.valueOf("LEGACY_" + type), amount, durability);
        }
        Objects.requireNonNull(itemStack);

        this.itemStack = itemStack.clone();
        this.itemMeta = itemStack.clone().getItemMeta();
    }

    /**
     * 以传入的物品创建一个物品建造模式对象
     *
     * @param itemStack 物品
     */
    public ItemBuilder(ItemStack itemStack) {
        Objects.requireNonNull(itemStack);
        itemStack = itemStack.clone();

        this.itemStack = itemStack.clone();
        this.itemMeta = itemStack.clone().getItemMeta();
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

    
    @Override
    public ItemBuilder setDisplayName(String name) {
        itemMeta.setDisplayName(IString.color(name));
        return this;
    }

    @Override
    public short getDurability() {
        return itemStack.getDurability();
    }

    
    @Override
    public ItemBuilder setDurability(short durability) {
        itemStack.setDurability(durability);
        return this;
    }

    @Override
    public int getAmount() {
        return itemStack.getAmount();
    }

    
    @Override
    public ItemBuilder setAmount(int amount) {
        if (amount > 0) {
            itemStack.setAmount(amount);
        } else {
            itemStack = null;
        }
        return this;
    }

    @Override
    public Material getType() {
        return itemStack.getType();
    }

    
    @Override
    public ItemBuilder addLore(String... insertLore) {
        List<String> lore = new ArrayList<>();
        if (itemMeta.hasLore()) {
            lore.addAll(itemMeta.getLore());
        }
        lore.addAll(Arrays.asList(insertLore));
        itemMeta.setLore(IList.listReplace(lore, "&", "§"));
        return this;
    }

    
    @Override
    public ItemBuilder addLore(int index, String... insertLore) {
        List<String> lore = new ArrayList<>();
        if (itemMeta.hasLore()) {
            lore.addAll(itemMeta.getLore());
        }
        lore.addAll(index, Arrays.asList(insertLore));
        itemMeta.setLore(IList.listReplace(lore, "&", "§"));
        return this;
    }

    
    @Override
    public ItemBuilder addLoreAll(List<String> l) {
        List<String> lore = new ArrayList<>();
        if (itemMeta.hasLore()) {
            lore.addAll(itemMeta.getLore());
        }
        lore.addAll(l);
        itemMeta.setLore(IList.listReplace(lore, "&", "§"));
        return this;
    }

    
    @Override
    public ItemBuilder addLoreAll(int index, List<String> l) {
        List<String> lore = new ArrayList<>();
        if (itemMeta.hasLore()) {
            lore.addAll(itemMeta.getLore());
        }
        lore.addAll(index, l);
        itemMeta.setLore(IList.listReplace(lore, "&", "§"));
        return this;
    }

    
    @Override
    public ItemBuilder setLore(List<String> lore) {
        itemMeta.setLore(IList.listReplace(lore, "&", "§"));
        return this;
    }

    
    @Override
    public ItemBuilder setLore(int index, String l) {
        List<String> list = getLore();
        list.set(index, l);
        setLore(list);
        return this;
    }

    @Override
    public List<String> getLore() {
        return itemMeta.hasLore() ? itemMeta.getLore() : new ArrayList<>();
    }

    
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
}
