package pers.tany.yukinoaapi.interfacepart.entity;

import de.tr7zw.nbtapi.NBTEntity;
import de.tr7zw.nbtapi.NBTList;
import de.tr7zw.nbtapi.NBTListCompound;
import org.bukkit.entity.LivingEntity;

import javax.annotation.Nonnull;


/**
 * 生物属性操作相关
 */
public interface ILivingEntityAttributes {

    /**
     * 获取这个生物的最大生命值
     *
     * @param livingEntity 涉及的生物
     * @return 最大生命值
     */
    static double getMaxHealth(LivingEntity livingEntity) {
        NBTEntity nbtEntity = new NBTEntity(livingEntity);
        NBTList<NBTListCompound> list = nbtEntity.getCompoundList("Attributes");
        for (NBTListCompound nbtListCompound : list) {
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
    static void setMaxHealth(LivingEntity livingEntity, double maxHealth) {
        setMaxHealth(livingEntity, maxHealth, true);
    }

    /**
     * 设置这个生物的最大生命值
     *
     * @param livingEntity 涉及的生物
     * @param maxHealth    最大生命值
     * @param restore      是否恢复值满血
     */
    static void setMaxHealth(LivingEntity livingEntity, double maxHealth, boolean restore) {
        NBTEntity nbtEntity = new NBTEntity(livingEntity);
        NBTList<NBTListCompound> list = nbtEntity.getCompoundList("Attributes");
        for (NBTListCompound nbtListCompound : list) {
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
    static double getMovementSpeed(LivingEntity livingEntity) {
        NBTEntity nbtEntity = new NBTEntity(livingEntity);
        NBTList<NBTListCompound> list = nbtEntity.getCompoundList("Attributes");
        for (NBTListCompound nbtListCompound : list) {
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
    static void setMovementSpeed(LivingEntity livingEntity, double movementSpeed) {
        NBTEntity nbtEntity = new NBTEntity(livingEntity);
        NBTList<NBTListCompound> list = nbtEntity.getCompoundList("Attributes");
        for (NBTListCompound nbtListCompound : list) {
            if (nbtListCompound.getString("Name").contains("generic.movementSpeed") || nbtListCompound.getString("Name").contains("generic.movement_speed")) {
                nbtListCompound.setDouble("Base", movementSpeed);
                break;
            }
        }
    }
}
