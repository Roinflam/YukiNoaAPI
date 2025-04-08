package pers.tany.yukinoaapi.interfacepart.item;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import pers.tany.yukinoaapi.NMSUtil;
import pers.tany.yukinoaapi.realizationpart.builder.ItemBuilder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Method;

/**
 * 物品堆相关
 */
public interface IItem {
    /**
     * 检查玩家手上是否为空
     *
     * @param player 涉及玩家
     * @return 如果空则为true，否则为false
     */
    static boolean isEmptyHand(Player player) {
        return isEmpty(player.getItemInHand());
    }

    /**
     * 检查物品是否为空
     *
     * @param itemStack 涉及物品
     * @return 如果空则为true，否则为false
     */
    static boolean isEmpty(@Nullable ItemStack itemStack) {
        return itemStack == null || itemStack.getType().equals(Material.AIR);
    }

    /**
     * 返回物品名称
     * 优先自定义，其次翻译后的名称，如果没有翻译则为Material枚举名
     *
     * @param itemStack 涉及的物品
     * @return 物品名称
     */
    @Nullable
    static String getName(ItemStack itemStack) {
        if (itemStack.getItemMeta().hasDisplayName()) {
            return itemStack.getItemMeta().getDisplayName();
        }

        try {
            Class<?> nmsItemStackClass = NMSUtil.nmsItemStackClass;
            Class<?> nmsItemClass = NMSUtil.nmsItemClass;
            Method asNMSCopy = NMSUtil.asNMSCopy;
            try {
                Object nmsItem = asNMSCopy.invoke(null, itemStack);
                String name;
                try {
                    Class<?> iChatBaseComponentClass = NMSUtil.iChatBaseComponentClass;
                    Method getItem = NMSUtil.getItem;
                    Method getName_Item = NMSUtil.getName_Item;
                    Method getString = NMSUtil.getString_IChatBaseComponent;
                    Object item = getItem.invoke(nmsItem);
                    name = getString.invoke(getName_Item.invoke(item, nmsItem)).toString();
                } catch (Exception e) {
                    Method getName_itemStack = NMSUtil.getName_ItemStack;
                    name = getName_itemStack.invoke(nmsItem).toString();
                }
                return name.split("\\.").length > 2 ? itemStack.getType().toString() : name;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        } catch (Exception e) {
            return itemStack.getType().toString();
        }
    }

    /**
     * 获取物品的NBT
     *
     * @param itemStack 涉及物品
     * @return 物品的NBT
     */
    @Nullable
    static String getItemNBT(ItemStack itemStack) {
        try {
            Class<?> nbttagCompound = NMSUtil.nbttagCompoundClass;
            Method asNMSCopy = NMSUtil.asNMSCopy;
            Method save = NMSUtil.save_ItemStack;
            Class<?> nmsItemStackClass = NMSUtil.nmsItemStackClass;
            Object tag = nbttagCompound.newInstance();
            Object nmsItem = asNMSCopy.invoke(null, itemStack);
            return save.invoke(nmsItem, tag).toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    /**
     * 根据物品tag获取物品（仅限低版本使用）
     *
     * @param tag 物品nbt
     * @return 设置后的物品
     */
    @Nullable
    static ItemStack getItemStack(String tag) {
        try {
            final Class<?> nbttagCompoundClass = NMSUtil.nbttagCompoundClass;
            final Method asNMSCopy = NMSUtil.asNMSCopy;
            final Method parse = NMSUtil.parse;
            final Method load = NMSUtil.load_ItemStack;
            final Method asBukkitCopy = NMSUtil.asBukkitCopy;
            final Object nbttagCompound = parse.invoke(null, tag);
            final Object nmsItem = asNMSCopy.invoke(null, new ItemBuilder("STONE").getItemStack());
            load.invoke(nmsItem, nbttagCompound);
            return (ItemStack) asBukkitCopy.invoke(null, nmsItem);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 根据物品tag获取物品
     *
     * @param itemStack 涉及物品
     * @param tag       物品nbt
     * @return 设置后的物品
     */
    @Nullable
    static ItemStack getItemStack(ItemStack itemStack, String tag) {
        try {
            Class<?> nbttagCompoundClass = NMSUtil.nbttagCompoundClass;
            Method asNMSCopy = NMSUtil.asNMSCopy;
            Method parse = NMSUtil.parse;
            Method setTag = NMSUtil.setTag;
            Method asBukkitCopy = NMSUtil.asBukkitCopy;

            Object nbttagCompound = parse.invoke(null, tag);
            Object nmsItem = asNMSCopy.invoke(null, itemStack);
            setTag.invoke(nmsItem, nbttagCompound);

            return (ItemStack) asBukkitCopy.invoke(null, nmsItem);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
