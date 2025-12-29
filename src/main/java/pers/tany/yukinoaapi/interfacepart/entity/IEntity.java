package pers.tany.yukinoaapi.interfacepart.entity;

import org.bukkit.entity.Entity;
import pers.tany.yukinoaapi.NMSUtil;
import pers.tany.yukinoaapi.util.LangUtil;
import pers.tany.yukinoaapi.util.LogUtil;

import javax.annotation.Nullable;
import java.lang.reflect.Method;

/**
 * 实体操作相关接口
 * 提供获取实体名称、NBT等功能
 *
 * @author tany
 * @since 1.0.0
 */
public interface IEntity {

    /**
     * 获取实体的显示名称
     *
     * @param entity 目标实体
     * @return 实体名称，失败返回实体类型名
     */
    @Nullable
    static String getName(Entity entity) {
        if (entity == null) {
            return null;
        }

        if (!NMSUtil.isInitialized()) {
            LogUtil.debug(LangUtil.get("entity.nms-not-init"));
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

            // 如果是翻译键（包含多个点），返回类型名
            if (name != null && name.split("\\.").length > 2) {
                return entity.getType().toString();
            }

            return name != null ? name : entity.getType().toString();

        } catch (Exception e) {
            LogUtil.debug(LangUtil.get("entity.get-name-fail", e.getMessage()));
            return entity.getType().toString();
        }
    }

    /**
     * 获取实体的保存名称（命名空间格式）
     *
     * @param entity 目标实体
     * @return 格式如 minecraft:zombie
     */
    @Nullable
    static String getSaveName(Entity entity) {
        if (entity == null) {
            return null;
        }

        if (!NMSUtil.isInitialized()) {
            LogUtil.debug(LangUtil.get("entity.nms-not-init"));
            return entity.getType().toString();
        }

        Method getHandle = NMSUtil.getHandle_Entity;
        Method getSaveID = NMSUtil.getSaveID;

        if (getHandle == null) {
            return entity.getType().toString();
        }

        if (getSaveID == null) {
            LogUtil.debug(LangUtil.get("entity.get-save-id-unavailable"));
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
                LogUtil.debug(LangUtil.get("entity.get-save-fail", e.getMessage()));
            }

            return entity.getType().toString();

        } catch (Exception e) {
            LogUtil.debug(LangUtil.get("entity.get-save-name-fail", e.getMessage()));
            return entity.getType().toString();
        }
    }

    /**
     * 获取实体的NBT数据
     *
     * @param entity 目标实体
     * @return NBT字符串，失败返回null
     */
    @Nullable
    static String getEntityNBT(Entity entity) {
        if (entity == null) {
            return null;
        }

        if (!NMSUtil.isInitialized()) {
            LogUtil.debug(LangUtil.get("entity.nbt-reflect-unavailable"));
            return null;
        }

        Class<?> nbttagCompoundClass = NMSUtil.nbttagCompoundClass;
        Method getHandle = NMSUtil.getHandle_Entity;
        Method save = NMSUtil.save_Entity;

        if (nbttagCompoundClass == null || getHandle == null || save == null) {
            LogUtil.debug(LangUtil.get("entity.nbt-reflect-unavailable"));
            return null;
        }

        try {
            Object nmsEntity = getHandle.invoke(entity);
            Object nbttagCompound = nbttagCompoundClass.getDeclaredConstructor().newInstance();
            save.invoke(nmsEntity, nbttagCompound);
            return nbttagCompound.toString();

        } catch (Exception e) {
            LogUtil.debug(LangUtil.get("entity.get-nbt-fail", e.getMessage()));
            return null;
        }
    }
}