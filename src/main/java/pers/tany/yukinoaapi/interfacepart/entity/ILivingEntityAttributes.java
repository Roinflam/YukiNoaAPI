package pers.tany.yukinoaapi.interfacepart.entity;

import de.tr7zw.nbtapi.NBTEntity;
import de.tr7zw.nbtapi.iface.ReadWriteNBT;
import de.tr7zw.nbtapi.iface.ReadWriteNBTCompoundList;
import org.bukkit.entity.LivingEntity;

/**
 * 生物属性操作相关
 * <p>
 * 适配新版 NBT-API：{@code getCompoundList} 返回 {@link ReadWriteNBTCompoundList}
 * （元素为 {@link ReadWriteNBT}），替代旧版已废弃的 {@code NBTList<NBTListCompound>}。
 * 列表元素的 getString/getDouble/setDouble 方法签名与旧版一致，循环体逻辑不变。
 * </p>
 */
public final class ILivingEntityAttributes {

    /**
     * 工具类,禁止实例化
     *
     * @throws UnsupportedOperationException 总是抛出
     */
    private ILivingEntityAttributes() {
        throw new UnsupportedOperationException("ILivingEntityAttributes 是工具类,不允许实例化");
    }


    /**
     * 获取这个生物的最大生命值
     *
     * @param livingEntity 涉及的生物
     * @return 最大生命值
     */
    public static double getMaxHealth(LivingEntity livingEntity) {
        NBTEntity nbtEntity = new NBTEntity(livingEntity);
        ReadWriteNBTCompoundList list = nbtEntity.getCompoundList("Attributes");
        for (ReadWriteNBT nbtListCompound : list) {
            if (nbtListCompound.getString("Name").contains("generic.maxHealth") || nbtListCompound.getString("Name").contains("generic.max_health")) {
                return nbtListCompound.getDouble("Base");
            }
        }
        return -1;
    }

    /**
     * 设置这个生物的最大生命值
     *
     * @param livingEntity 涉及的生物
     * @param maxHealth    最大生命值
     */
    public static void setMaxHealth(LivingEntity livingEntity, double maxHealth) {
        setMaxHealth(livingEntity, maxHealth, true);
    }

    /**
     * 设置这个生物的最大生命值
     *
     * @param livingEntity 涉及的生物
     * @param maxHealth    最大生命值
     * @param restore      是否恢复值满血
     */
    public static void setMaxHealth(LivingEntity livingEntity, double maxHealth, boolean restore) {
        NBTEntity nbtEntity = new NBTEntity(livingEntity);
        ReadWriteNBTCompoundList list = nbtEntity.getCompoundList("Attributes");
        for (ReadWriteNBT nbtListCompound : list) {
            if (nbtListCompound.getString("Name").contains("generic.maxHealth") || nbtListCompound.getString("Name").contains("generic.max_health")) {
                nbtListCompound.setDouble("Base", maxHealth);
                break;
            }
        }
        if (restore) {
            livingEntity.setHealth(livingEntity.getMaxHealth());
        }
    }

    /**
     * 获取这个生物的移动速度
     *
     * @param livingEntity 涉及的生物
     * @return 移动速度
     */
    public static double getMovementSpeed(LivingEntity livingEntity) {
        NBTEntity nbtEntity = new NBTEntity(livingEntity);
        ReadWriteNBTCompoundList list = nbtEntity.getCompoundList("Attributes");
        for (ReadWriteNBT nbtListCompound : list) {
            if (nbtListCompound.getString("Name").contains("generic.movementSpeed") || nbtListCompound.getString("Name").contains("generic.movement_speed")) {
                return nbtListCompound.getDouble("Base");
            }
        }
        return 0;
    }

    /**
     * 设置这个生物的移动速度
     *
     * @param livingEntity  涉及的生物
     * @param movementSpeed 移动速度
     */
    public static void setMovementSpeed(LivingEntity livingEntity, double movementSpeed) {
        NBTEntity nbtEntity = new NBTEntity(livingEntity);
        ReadWriteNBTCompoundList list = nbtEntity.getCompoundList("Attributes");
        for (ReadWriteNBT nbtListCompound : list) {
            if (nbtListCompound.getString("Name").contains("generic.movementSpeed") || nbtListCompound.getString("Name").contains("generic.movement_speed")) {
                nbtListCompound.setDouble("Base", movementSpeed);
                break;
            }
        }
    }
}


