package pers.tany.yukinoaapi.interfacepart.entity;

import org.bukkit.entity.Entity;
import pers.tany.yukinoaapi.NMSUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * 实体操作相关
 */
public interface IEntity {

    /**
     * 获取实体的名称
     *
     * @param entity 涉及的实体
     * @return 优先自定义，其次翻译后的名称，如果没有翻译则为Material枚举名
     */
    @Nullable
    static String getName(Entity entity) {
        Class<?> craftEntityClass = NMSUtil.craftEntityClass;
        Class<?> nmsEntityClass = NMSUtil.nmsEntityClass;
        Method getHandle = NMSUtil.getHandle_Entity;
        try {
            Object nmsEntity = getHandle.invoke(entity);
            @Nullable String name = null;
            try {
                Method getDisplayName = NMSUtil.getDisplayName_Entity;
                Method getString = NMSUtil.getString_IChatBaseComponent;
                name = getString.invoke(getDisplayName.invoke(nmsEntity)).toString();
            } catch (Exception e) {
                Method getName = NMSUtil.getName_Entity;
                try {
                    name = getName.invoke(nmsEntity).toString();
                } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                    e.printStackTrace();
                }
            }
            return name.split("\\.").length > 2 ? entity.getType().toString() : name;
        } catch (Exception exception) {
            exception.printStackTrace();
            return null;
        }
    }

    /**
     * 获取实体的储存名称
     *
     * @param entity 涉及的实体
     * @return 所属模组:生物名称(低版本只返回类型枚举名)
     */
    @Nullable
    static String getSaveName(Entity entity) {
        Class<?> craftEntityClass = NMSUtil.craftEntityClass;
        Class<?> nmsEntityClass = NMSUtil.nmsEntityClass;
        Method getHandle = NMSUtil.getHandle_Entity;
        Method getSaveID = NMSUtil.getSaveID;
        try {
            Object nmsEntity = getHandle.invoke(entity);
            try {
                return getSaveID.invoke(nmsEntity).toString();
            } catch (Exception e) {
                return entity.getType().toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 获取实体的NBT
     *
     * @param entity 涉及实体
     * @return 物品的NBT
     */
    @Nullable
    static String getEntityNBT(Entity entity) {
        Class<?> nmsEntityClass = NMSUtil.nmsEntityClass;
        Class<?> nbttagCompoundClass = NMSUtil.nbttagCompoundClass;
        Method getHandle = NMSUtil.getHandle_Entity;
        Method save = NMSUtil.save_Entity;
        try {
            Object nmsEntity = getHandle.invoke(entity);
            Object nbttagCompound = nbttagCompoundClass.newInstance();
            save.invoke(nmsEntity, nbttagCompound);
            return nbttagCompound.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
