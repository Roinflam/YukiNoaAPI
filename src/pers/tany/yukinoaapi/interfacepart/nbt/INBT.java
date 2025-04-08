package pers.tany.yukinoaapi.interfacepart.nbt;

import de.tr7zw.nbtapi.NBTCompound;
import de.tr7zw.nbtapi.NBTEntity;
import de.tr7zw.nbtapi.NBTItem;
import de.tr7zw.nbtapi.NBTTileEntity;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;


/**
 * NBT操作相关
 */
public interface INBT {
    /**
     * 获取这个键下的值
     * 如果键不存在、值不为字符串或者数字则为null
     *
     * @param nbtCompound nbt对象
     * @param key         键
     * @return 值
     */
    @Nullable
    static Object getValue(NBTCompound nbtCompound, String key) {
        @Nullable Object value = null;
        if (nbtCompound.hasKey(key)) {
            if (nbtCompound.getString(key) != null && !nbtCompound.getString(key).isEmpty()) {
                value = nbtCompound.getString(key);
            } else if (nbtCompound.getDouble(key) != 0) {
                value = nbtCompound.getDouble(key);
            }
        }
        return value;
    }

    /**
     * 获取string形式的物品tagnbt
     *
     * @param itemStack 涉及的物品
     * @return string形式的nbt
     */
    static String getNBT(ItemStack itemStack) {
        return new NBTItem(itemStack).toString();
    }

    /**
     * 获取string形式的实体nbt
     *
     * @param entity 涉及的实体
     * @return string形式的nbt
     */
    static String getNBT(Entity entity) {
        return new NBTEntity(entity).toString();
    }

    /**
     * 获取string形式的方块nbt
     *
     * @param block 涉及的方块
     * @return string形式的nbt
     */
    static String getNBT(Block block) {
        return new NBTTileEntity(block.getState()).toString();
    }
}
