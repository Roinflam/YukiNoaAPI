package pers.tany.yukinoaapi.interfacepart.inventory;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import pers.tany.yukinoaapi.interfacepart.builder.IItemBuilder;
import pers.tany.yukinoaapi.interfacepart.item.IItem;
import pers.tany.yukinoaapi.interfacepart.other.IString;
import pers.tany.yukinoaapi.realizationpart.builder.ItemBuilder;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * 容器物品相关
 */
public interface IInventoryItem {
    /**
     * 返回物品栏剩余的空位格子数量
     *
     * @param inventory 射击的物品栏
     * @return 空位的数量
     */
    static int backpackEmpty(Inventory inventory) {
        int empty = 0;
        for (int index = 0; index < inventory.getSize(); index++) {
            if (IItem.isEmpty(inventory.getItem(index))) {
                empty++;
            }
        }
        return empty;
    }

    /**
     * 检查物品栏这个物品有多少个
     *
     * @param inventory 涉及的物品栏
     * @param itemStack 物品
     * @return 物品的数量
     */
    static int hasAmount(Inventory inventory, ItemStack itemStack) {
        return hasAmount(inventory, itemStack, true);
    }

    /**
     * 检查物品栏这个物品有多少个
     *
     * @param inventory 涉及的物品栏
     * @param itemStack 物品
     * @param isSimilar 是否完全匹配，否则为匹配id
     * @return 物品的数量
     */
    static int hasAmount(Inventory inventory, ItemStack itemStack, boolean isSimilar) {
        int amount = 0;
        if (isSimilar) {
            for (ItemStack i : new ArrayList<ItemStack>(Arrays.asList(inventory.getContents()))) {
                if (itemStack.isSimilar(i)) {
                    amount += i.getAmount();
                }
            }
        } else {
            for (ItemStack i : new ArrayList<ItemStack>(Arrays.asList(inventory.getContents()))) {
                if (!IItem.isEmpty(i) && i.getTypeId() == itemStack.getTypeId() && i.getDurability() == itemStack.getDurability()) {
                    amount += i.getAmount();
                }
            }
        }
        return amount;
    }

    /**
     * 检查物品栏是否有这么多数量的物品
     *
     * @param inventory 涉及的物品栏
     * @param itemStack 物品
     * @param number    数量
     * @return 如果有为true，否则为false
     */
    static boolean hasAmount(Inventory inventory, ItemStack itemStack, int number) {
        return hasAmount(inventory, itemStack) >= number;
    }

    /**
     * 检查物品栏是否有这么多数量的物品
     *
     * @param inventory 涉及的物品栏
     * @param itemStack 物品
     * @param number    数量
     * @param isSimilar 是否完全匹配，否则为匹配id
     * @return 如果有为true，否则为false
     */
    static boolean hasAmount(Inventory inventory, ItemStack itemStack, int number, boolean isSimilar) {
        return hasAmount(inventory, itemStack, isSimilar) >= number;
    }

    /**
     * 移除物品栏这物品一个(考虑堆叠数量)
     *
     * @param inventory 涉及的物品栏
     * @param itemStack 物品
     * @return 是否移除成功
     */
    static boolean removeItem(Inventory inventory, ItemStack itemStack) {
        int index = 0;
        while (index < inventory.getContents().length) {
            ItemStack i = inventory.getItem(index);
            if (!IItem.isEmpty(i) && i.equals(itemStack)) {
                inventory.setItem(index, null);
                return true;
            }
            index++;
        }
        return false;
    }


    /**
     * 移除物品栏这个物品...个(考虑堆叠数量)
     *
     * @param inventory 涉及的物品栏
     * @param itemStack 物品
     * @param number    数量
     * @return 剩余的次数(物品会被移除)
     */
    static int removeItem(Inventory inventory, ItemStack itemStack, int number) {
        int index = 0;
        while (index < inventory.getContents().length) {
            if (number <= 0) {
                return 0;
            }
            ItemStack i = inventory.getItem(index);
            if (!IItem.isEmpty(itemStack) && i.equals(itemStack)) {
                inventory.setItem(index, null);
                number--;
            }
            index++;
        }
        return number;
    }

    /**
     * 从物品栏扣除这个物品，数量为传入物品的堆叠数量
     *
     * @param inventory 涉及的物品栏
     * @param itemStack 物品
     * @return 剩余的数量
     */
    static int deductItem(Inventory inventory, ItemStack itemStack) {
        return deductItem(inventory, itemStack, itemStack.getAmount());
    }

    /**
     * 从物品栏扣除这个物品...个
     *
     * @param inventory 涉及的物品栏
     * @param name      物品名称
     * @param amount    个数
     * @return 剩余的数量
     */
    static int deductItem(Inventory inventory, String name, int amount) {
        for (ItemStack i : new ArrayList<ItemStack>(Arrays.asList(inventory.getContents()))) {
            IItemBuilder itemBuilder = new ItemBuilder(i);
            if (!IItem.isEmpty(i) && itemBuilder.getName().equals(IString.color(name))) {
                if (i.getAmount() >= amount) {
                    if (i.getAmount() > amount) {
                        i.setAmount(i.getAmount() - amount);
                    } else {
                        removeItem(inventory, i);
                    }
                    amount = 0;
                    break;
                } else {
                    amount -= i.getAmount();
                    removeItem(inventory, i);
                }
            }
        }
        return amount;
    }

    /**
     * 从物品栏扣除这个物品，数量为传入物品的堆叠数量
     *
     * @param inventory 涉及的物品栏
     * @param itemStack 物品
     * @param amount    数量
     * @return 剩余的数量
     */
    static int deductItem(Inventory inventory, ItemStack itemStack, int amount) {
        for (ItemStack i : new ArrayList<ItemStack>(Arrays.asList(inventory.getContents()))) {
            if (itemStack.isSimilar(i)) {
                if (i.getAmount() >= amount) {
                    if (i.getAmount() > amount) {
                        i.setAmount(i.getAmount() - amount);
                    } else {
                        removeItem(inventory, i);
                    }
                    amount = 0;
                    break;
                } else {
                    amount -= i.getAmount();
                    removeItem(inventory, i);
                }
            }
        }
        return amount;
    }
}
