package pers.tany.yukinoaapi.interfacepart.entity;

import org.bukkit.entity.Entity;
import pers.tany.yukinoaapi.NMSUtil;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @description: 实体操作相关接口
 */
public interface IEntity {
    Logger logger = Logger.getLogger("YukiNoaAPI");

    /**
     * @description: 获取实体的显示名称
     * @param entity - 目标实体
     * @return String - 实体名称
     */
    @Nullable
    static String getName(Entity entity) {
        if (entity == null) {
            return null;
        }

        if (!NMSUtil.isInitialized()) {
            logger.warning("[IEntity] NMSUtil 未初始化: " + NMSUtil.getInitError());
            return entity.getType().toString();
        }

        Method getHandle = NMSUtil.getHandle_Entity;
        if (getHandle == null) {
            return entity.getType().toString();
        }

        try {
            Object nmsEntity = getHandle.invoke(entity);
            String name = null;

            // 尝试获取显示名称
            Method getDisplayName = NMSUtil.getDisplayName_Entity;
            Method getString = NMSUtil.getString_IChatBaseComponent;

            if (getDisplayName != null && getString != null) {
                try {
                    Object nameComponent = getDisplayName.invoke(nmsEntity);
                    if (nameComponent != null) {
                        name = getString.invoke(nameComponent).toString();
                    }
                } catch (Exception ignored) {
                }
            }

            // 如果没有显示名称，获取默认名称
            if (name == null || name.isEmpty()) {
                Method getName = NMSUtil.getName_Entity;
                if (getName != null) {
                    try {
                        Object nameObj = getName.invoke(nmsEntity);
                        if (nameObj != null) {
                            // 如果返回的是 IChatBaseComponent，需要转字符串
                            if (getString != null && NMSUtil.iChatBaseComponentClass != null
                                    && NMSUtil.iChatBaseComponentClass.isInstance(nameObj)) {
                                name = getString.invoke(nameObj).toString();
                            } else {
                                name = nameObj.toString();
                            }
                        }
                    } catch (Exception ignored) {
                    }
                }
            }

            // 如果是翻译键，返回类型名
            if (name != null && name.contains(".")) {
                return entity.getType().toString();
            }

            return name != null ? name : entity.getType().toString();

        } catch (Exception e) {
            logger.log(Level.FINE, "[IEntity] 获取实体名称失败", e);
            return entity.getType().toString();
        }
    }

    /**
     * @description: 获取实体的保存名称
     * @param entity - 目标实体
     * @return String - 格式如 minecraft:zombie
     */
    @Nullable
    static String getSaveName(Entity entity) {
        if (entity == null) {
            return null;
        }

        if (!NMSUtil.isInitialized()) {
            logger.warning("[IEntity] NMSUtil 未初始化");
            return entity.getType().toString();
        }

        Method getHandle = NMSUtil.getHandle_Entity;
        Method getSaveID = NMSUtil.getSaveID;

        if (getHandle == null) {
            return entity.getType().toString();
        }

        if (getSaveID == null) {
            logger.fine("[IEntity] getSaveID 方法不可用（可能是低版本）");
            return entity.getType().toString();
        }

        try {
            Object nmsEntity = getHandle.invoke(entity);

            try {
                Object saveID = getSaveID.invoke(nmsEntity);
                if (saveID != null) {
                    return saveID.toString();
                }
            } catch (Exception e) {
                logger.fine("[IEntity] getSaveID 调用失败: " + e.getMessage());
            }

            return entity.getType().toString();

        } catch (Exception e) {
            logger.log(Level.FINE, "[IEntity] 获取保存名称失败", e);
            return entity.getType().toString();
        }
    }

    /**
     * @description: 获取实体的 NBT 数据
     * @param entity - 目标实体
     * @return String - NBT 字符串
     */
    @Nullable
    static String getEntityNBT(Entity entity) {
        if (entity == null) {
            return null;
        }

        if (!NMSUtil.isInitialized()) {
            logger.warning("[IEntity] NMSUtil 未初始化");
            return null;
        }

        Class<?> nbttagCompoundClass = NMSUtil.nbttagCompoundClass;
        Method getHandle = NMSUtil.getHandle_Entity;
        Method save = NMSUtil.save_Entity;

        if (nbttagCompoundClass == null || getHandle == null || save == null) {
            logger.fine("[IEntity] NBT 相关反射对象不可用");
            return null;
        }

        try {
            Object nmsEntity = getHandle.invoke(entity);
            Object nbttagCompound = nbttagCompoundClass.getDeclaredConstructor().newInstance();
            save.invoke(nmsEntity, nbttagCompound);
            return nbttagCompound.toString();

        } catch (Exception e) {
            logger.log(Level.WARNING, "[IEntity] 获取 NBT 失败", e);
            return null;
        }
    }
}