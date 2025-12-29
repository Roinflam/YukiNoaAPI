// 文件：NMSUtil.java
// 路径：src/pers/tany/yukinoaapi/NMSUtil.java
package pers.tany.yukinoaapi;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import pers.tany.yukinoaapi.util.LogUtil;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * NMS 跨版本兼容工具类
 * 支持 Minecraft 1.7-1.21 版本
 */
public class NMSUtil {

    /**
     * 服务器版本字符串（如 v1_12_R1）
     */
    private static final String version;

    /**
     * 反射是否初始化成功
     */
    private static boolean initialized = false;

    /**
     * 初始化失败时的错误信息
     */
    private static String initError = null;

    // ==================== 玩家相关 ====================

    /**
     * 玩家连接字段
     */
    public static Field playerConnection;

    /**
     * 聊天数据包类
     */
    public static Class<?> packetPlayOutChat;

    /**
     * 发送数据包方法
     */
    public static Method send;

    /**
     * 获取玩家NMS句柄方法
     */
    public static Method getHandle_Player;

    /**
     * 聊天组件接口
     */
    public static Class<?> iChatBaseComponent;

    /**
     * 文本聊天组件类
     */
    public static Class<?> chatComponentText;

    /**
     * 聊天消息类型枚举
     */
    public static Class<?> chatMessageType;

    // ==================== 物品相关 ====================

    /**
     * NBT复合标签类
     */
    public static Class<?> nbttagCompoundClass;

    /**
     * CraftItemStack类
     */
    public static Class<?> craftItemStack;

    /**
     * 转换为NMS物品方法
     */
    public static Method asNMSCopy;

    /**
     * 保存物品NBT方法
     */
    public static Method save_ItemStack;

    /**
     * NMS物品类
     */
    public static Class<?> nmsItemStackClass;

    /**
     * 获取物品名称方法
     */
    public static Method getName_ItemStack;

    /**
     * 获取物品类型方法
     */
    public static Method getItem;

    /**
     * NMS物品类型类
     */
    public static Class<?> nmsItemClass;

    /**
     * 获取物品类型名称方法
     */
    public static Method getName_Item;

    // ==================== 服务器相关 ====================

    /**
     * MinecraftServer类
     */
    public static Class<?> minecraftServer;

    /**
     * 获取服务器实例方法
     */
    public static Method getServer;

    /**
     * CraftServer类
     */
    public static Class<?> craftServerClass;

    // ==================== 实体相关 ====================

    /**
     * CraftEntity类
     */
    public static Class<?> craftEntityClass;

    /**
     * NMS实体类
     */
    public static Class<?> nmsEntityClass;

    /**
     * 获取实体NMS句柄方法
     */
    public static Method getHandle_Entity;

    /**
     * 获取实体保存ID方法
     */
    public static Method getSaveID;

    /**
     * 获取实体名称方法
     */
    public static Method getName_Entity;

    /**
     * 获取实体显示名称方法
     */
    public static Method getDisplayName_Entity;

    /**
     * 保存实体NBT方法
     */
    public static Method save_Entity;

    /**
     * 加载实体NBT方法
     */
    public static Method setNbttagCompound;

    /**
     * 获取实体方法
     */
    public static Method getEntity;

    // ==================== NBT 解析相关 ====================

    /**
     * Mojangson解析器类
     */
    public static Class<?> mojangsonParser;

    /**
     * 解析NBT字符串方法
     */
    public static Method parse;

    /**
     * 设置物品NBT方法
     */
    public static Method setTag;

    /**
     * 从NBT加载物品方法
     */
    public static Method load_ItemStack;

    /**
     * 转换为Bukkit物品方法
     */
    public static Method asBukkitCopy;

    // ==================== 方块实体相关 ====================

    /**
     * CraftWorld类
     */
    public static Class<?> craftWorldClass;

    /**
     * 获取世界NMS句柄方法
     */
    public static Method getHandle_Word;

    /**
     * NMS世界类
     */
    public static Class<?> nmsWorldClass;

    /**
     * TileEntity类
     */
    public static Class<?> tileEntityClass;

    /**
     * 方块位置类
     */
    public static Class<?> blockPositionClass;

    /**
     * 获取TileEntity方法
     */
    public static Method getTileEntity;

    /**
     * 保存方块NBT方法
     */
    public static Method save_Block;

    /**
     * 加载方块NBT方法
     */
    public static Method load_Block;

    /**
     * 设置方块数据方法
     */
    public static Method setData;

    /**
     * CraftBlock类
     */
    public static Class<?> craftBlockClass;

    /**
     * 设置TileEntity方法
     */
    public static Method setTileEntity;

    // ==================== 文本组件相关 ====================

    /**
     * IChatBaseComponent类
     */
    public static Class<?> iChatBaseComponentClass;

    /**
     * 获取文本组件字符串方法
     */
    public static Method getString_IChatBaseComponent;

    // ==================== 附魔相关 ====================

    /**
     * CraftEnchantment类
     */
    public static Class<?> craftEnchantmentClass;

    /**
     * NMS附魔类
     */
    public static Class<?> nmsEnchantmentClass;

    /**
     * 获取附魔NMS句柄方法
     */
    public static Method getHandle_Enchantment;

    /**
     * 获取附魔名称方法
     */
    public static Method getName_Enchantment;

    /**
     * 附魔目标字段
     */
    public static Field target;

    /*
     * 静态初始化块
     * 检测服务器版本并初始化所有反射对象
     */
    static {
        version = detectVersion();
        LogUtil.info("检测到服务器版本: " + version);

        try {
            initializeReflection();
            initialized = true;
            LogUtil.success("NMS反射初始化成功");
        } catch (Exception e) {
            initError = e.getMessage();
            LogUtil.error("NMS反射初始化失败", e);
        }
    }

    /**
     * 检测服务器版本
     *
     * @return 版本字符串
     */
    private static String detectVersion() {
        try {
            String packageName = Bukkit.getServer().getClass().getPackage().getName();
            LogUtil.debug("服务器完整包名: " + packageName);

            String[] parts = packageName.split("\\.");
            LogUtil.debug("包名分段数: " + parts.length);

            // 标准 CraftBukkit 结构: org.bukkit.craftbukkit.v1_xx_Rx
            if (parts.length >= 4 && parts[2].equals("craftbukkit")) {
                String detectedVersion = parts[3];
                LogUtil.debug("从包名提取版本: " + detectedVersion);
                return detectedVersion;
            }

            // 备用方案：在所有分段中查找版本号
            for (String part : parts) {
                if (part.startsWith("v1_")) {
                    LogUtil.debug("在包名中找到版本: " + part);
                    return part;
                }
            }

            // 最后的备用方案
            LogUtil.warning("无法从包名检测版本，使用默认值 v1_21_R1");
            return "v1_21_R1";

        } catch (Exception e) {
            LogUtil.error("版本检测异常，使用默认值 v1_21_R1", e);
            return "v1_21_R1";
        }
    }

    /**
     * 初始化所有反射类和方法
     *
     * @throws Exception 初始化失败时抛出异常
     */
    private static void initializeReflection() throws Exception {
        initPlayerRelated();
        initItemRelated();
        initServerRelated();
        initEntityRelated();
        initNBTRelated();
        initBlockRelated();
        initTextComponentRelated();
        initEnchantmentRelated();
        setAllMethodsAccessible();
    }

    /**
     * 初始化玩家相关反射
     */
    private static void initPlayerRelated() {
        try {
            playerConnection = safeGetField(
                    safeGetClass(getNMSPack() + ".EntityPlayer", "net.minecraft.server.level.EntityPlayer"),
                    "playerConnection", "c"
            );

            packetPlayOutChat = safeGetClass(
                    getNMSPack() + ".PacketPlayOutChat",
                    "net.minecraft.network.protocol.game.PacketPlayOutChat"
            );

            Class<?> playerConnectionClass = safeGetClass(
                    getNMSPack() + ".PlayerConnection",
                    "net.minecraft.server.network.PlayerConnection"
            );

            Class<?> packetClass = safeGetClass(
                    getNMSPack() + ".Packet",
                    "net.minecraft.network.protocol.Packet"
            );

            if (playerConnectionClass != null && packetClass != null) {
                send = safeGetMethod(
                        playerConnectionClass,
                        new String[]{"sendPacket", "a", "b"},
                        packetClass
                );
            }

            Class<?> craftPlayerClass = safeGetClass(getOBCPack() + ".entity.CraftPlayer");
            if (craftPlayerClass != null) {
                getHandle_Player = safeGetMethod(craftPlayerClass, "getHandle");
            }

            iChatBaseComponent = safeGetClass(
                    getNMSPack() + ".IChatBaseComponent",
                    "net.minecraft.network.chat.IChatBaseComponent"
            );

            chatComponentText = safeGetClass(
                    getNMSPack() + ".ChatComponentText",
                    "net.minecraft.network.chat.ChatComponentText"
            );

            chatMessageType = safeGetClass(
                    getNMSPack() + ".ChatMessageType",
                    "net.minecraft.network.chat.ChatMessageType"
            );

            LogUtil.debug("玩家相关反射初始化完成");
        } catch (Exception e) {
            LogUtil.debug("玩家相关反射初始化部分失败: " + e.getMessage());
        }
    }

    /**
     * 初始化物品相关反射
     */
    private static void initItemRelated() {
        try {
            craftItemStack = safeGetClass(getOBCPack() + ".inventory.CraftItemStack");

            nbttagCompoundClass = safeGetClass(
                    getNMSPack() + ".NBTTagCompound",
                    "net.minecraft.nbt.NBTTagCompound"
            );

            nmsItemStackClass = safeGetClass(
                    getNMSPack() + ".ItemStack",
                    "net.minecraft.world.item.ItemStack"
            );

            if (craftItemStack != null && nmsItemStackClass != null) {
                asNMSCopy = safeGetMethod(craftItemStack, "asNMSCopy", ItemStack.class);
                asBukkitCopy = safeGetMethod(craftItemStack, "asBukkitCopy", nmsItemStackClass);
            }

            if (nmsItemStackClass != null && nbttagCompoundClass != null) {
                save_ItemStack = safeGetMethod(
                        nmsItemStackClass,
                        new String[]{"save", "b"},
                        nbttagCompoundClass
                );

                setTag = safeGetMethod(
                        nmsItemStackClass,
                        new String[]{"setTag", "c"},
                        nbttagCompoundClass
                );

                load_ItemStack = safeGetMethod(
                        nmsItemStackClass,
                        new String[]{"load", "c", "a"},
                        nbttagCompoundClass
                );

                getName_ItemStack = safeGetMethod(
                        nmsItemStackClass,
                        new String[]{"getName", "y", "q", "p", "m"}
                );

                getItem = safeGetMethod(
                        nmsItemStackClass,
                        new String[]{"getItem", "c", "o"}
                );
            }

            nmsItemClass = safeGetClass(
                    getNMSPack() + ".Item",
                    "net.minecraft.world.item.Item"
            );

            if (nmsItemClass != null && nmsItemStackClass != null) {
                getName_Item = safeGetMethod(
                        nmsItemClass,
                        new String[]{"h", "m"},
                        nmsItemStackClass
                );
            }

            LogUtil.debug("物品相关反射初始化完成");
        } catch (Exception e) {
            LogUtil.debug("物品相关反射初始化部分失败: " + e.getMessage());
        }
    }

    /**
     * 初始化服务器相关反射
     */
    private static void initServerRelated() {
        try {
            minecraftServer = safeGetClass(
                    getNMSPack() + ".MinecraftServer",
                    "net.minecraft.server.MinecraftServer"
            );

            if (minecraftServer != null) {
                getServer = safeGetMethod(minecraftServer, "getServer");
            }

            craftServerClass = safeGetClass(getOBCPack() + ".CraftServer");

            LogUtil.debug("服务器相关反射初始化完成");
        } catch (Exception e) {
            LogUtil.debug("服务器相关反射初始化部分失败: " + e.getMessage());
        }
    }

    /**
     * 初始化实体相关反射
     */
    private static void initEntityRelated() {
        try {
            craftEntityClass = safeGetClass(getOBCPack() + ".entity.CraftEntity");

            if (craftEntityClass != null) {
                getHandle_Entity = safeGetMethod(craftEntityClass, "getHandle");
            }

            nmsEntityClass = safeGetClass(
                    getNMSPack() + ".Entity",
                    "net.minecraft.world.entity.Entity"
            );

            if (nmsEntityClass != null && nbttagCompoundClass != null) {
                save_Entity = safeGetMethod(
                        nmsEntityClass,
                        new String[]{"save", "f", "e"},
                        nbttagCompoundClass
                );

                setNbttagCompound = safeGetMethod(
                        nmsEntityClass,
                        new String[]{"load", "f", "e"},
                        nbttagCompoundClass
                );

                getName_Entity = safeGetMethod(
                        nmsEntityClass,
                        new String[]{"getName", "ah", "cq"}
                );

                getDisplayName_Entity = safeGetMethod(
                        nmsEntityClass,
                        new String[]{"getDisplayName", "ah", "C_"}
                );

                getSaveID = safeGetMethod(
                        nmsEntityClass,
                        new String[]{"getSaveID", "bD", "bn"}
                );
            }

            if (craftEntityClass != null && craftServerClass != null && nmsEntityClass != null) {
                getEntity = safeGetMethod(
                        craftEntityClass,
                        "getEntity",
                        craftServerClass,
                        nmsEntityClass
                );
            }

            LogUtil.debug("实体相关反射初始化完成");
        } catch (Exception e) {
            LogUtil.debug("实体相关反射初始化部分失败: " + e.getMessage());
        }
    }

    /**
     * 初始化 NBT 解析相关反射
     */
    private static void initNBTRelated() {
        try {
            mojangsonParser = safeGetClass(
                    getNMSPack() + ".MojangsonParser",
                    "net.minecraft.nbt.MojangsonParser"
            );

            if (mojangsonParser != null) {
                parse = safeGetMethod(
                        mojangsonParser,
                        new String[]{"parse", "a"},
                        String.class
                );
            }

            LogUtil.debug("NBT解析相关反射初始化完成");
        } catch (Exception e) {
            LogUtil.debug("NBT解析相关反射初始化部分失败: " + e.getMessage());
        }
    }

    /**
     * 初始化方块实体相关反射
     */
    private static void initBlockRelated() {
        try {
            craftWorldClass = safeGetClass(getOBCPack() + ".CraftWorld");

            if (craftWorldClass != null) {
                getHandle_Word = safeGetMethod(craftWorldClass, "getHandle");
            }

            nmsWorldClass = safeGetClass(
                    getNMSPack() + ".World",
                    "net.minecraft.world.level.World"
            );

            tileEntityClass = safeGetClass(
                    getNMSPack() + ".TileEntity",
                    "net.minecraft.world.level.block.entity.TileEntity"
            );

            blockPositionClass = safeGetClass(
                    getNMSPack() + ".BlockPosition",
                    "net.minecraft.core.BlockPosition"
            );

            if (nmsWorldClass != null && blockPositionClass != null && tileEntityClass != null) {
                getTileEntity = safeGetMethod(
                        nmsWorldClass,
                        new String[]{"getTileEntity", "c_"},
                        blockPositionClass
                );

                if (getTileEntity == null) {
                    getTileEntity = safeGetMethod(
                            nmsWorldClass,
                            "getTileEntity",
                            int.class, int.class, int.class
                    );
                }

                setTileEntity = safeGetMethod(
                        nmsWorldClass,
                        new String[]{"setTileEntity", "a"},
                        blockPositionClass, tileEntityClass
                );

                if (setTileEntity == null) {
                    setTileEntity = safeGetMethod(
                            nmsWorldClass,
                            "setTileEntity",
                            int.class, int.class, int.class, tileEntityClass
                    );
                }
            }

            if (tileEntityClass != null && nbttagCompoundClass != null) {
                save_Block = safeGetMethod(
                        tileEntityClass,
                        new String[]{"save", "b"},
                        nbttagCompoundClass
                );

                load_Block = safeGetMethod(
                        tileEntityClass,
                        new String[]{"load", "a"},
                        nbttagCompoundClass
                );
            }

            craftBlockClass = safeGetClass(getOBCPack() + ".block.CraftBlock");

            if (craftBlockClass != null) {
                setData = safeGetMethod(craftBlockClass, "setData", byte.class);
            }

            LogUtil.debug("方块实体相关反射初始化完成");
        } catch (Exception e) {
            LogUtil.debug("方块实体相关反射初始化部分失败: " + e.getMessage());
        }
    }

    /**
     * 初始化文本组件相关反射
     */
    private static void initTextComponentRelated() {
        try {
            iChatBaseComponentClass = safeGetClass(
                    getNMSPack() + ".IChatBaseComponent",
                    "net.minecraft.network.chat.IChatBaseComponent"
            );

            if (iChatBaseComponentClass != null) {
                getString_IChatBaseComponent = safeGetMethod(
                        iChatBaseComponentClass,
                        new String[]{"getString", "a"}
                );
            }

            LogUtil.debug("文本组件相关反射初始化完成");
        } catch (Exception e) {
            LogUtil.debug("文本组件相关反射初始化部分失败: " + e.getMessage());
        }
    }

    /**
     * 初始化附魔相关反射
     */
    private static void initEnchantmentRelated() {
        try {
            craftEnchantmentClass = safeGetClass(getOBCPack() + ".enchantments.CraftEnchantment");

            nmsEnchantmentClass = safeGetClass(
                    getNMSPack() + ".Enchantment",
                    "net.minecraft.world.item.enchantment.Enchantment"
            );

            if (craftEnchantmentClass != null && nmsEnchantmentClass != null) {
                getHandle_Enchantment = safeGetMethod(
                        craftEnchantmentClass,
                        "getHandle"
                );

                getName_Enchantment = safeGetMethod(
                        nmsEnchantmentClass,
                        new String[]{"d", "c"},
                        int.class
                );

                if (getHandle_Enchantment == null) {
                    target = safeGetField(craftEnchantmentClass, "target");
                }
            }

            LogUtil.debug("附魔相关反射初始化完成");
        } catch (Exception e) {
            LogUtil.debug("附魔相关反射初始化部分失败: " + e.getMessage());
        }
    }

    /**
     * 设置所有反射方法为可访问
     */
    private static void setAllMethodsAccessible() {
        try {
            int count = 0;
            for (Field field : NMSUtil.class.getFields()) {
                Object value = field.get(null);
                if (value instanceof Method) {
                    ((Method) value).setAccessible(true);
                    count++;
                }
            }
            LogUtil.debug("已设置 " + count + " 个方法为可访问");
        } catch (Exception e) {
            LogUtil.debug("设置方法可访问性失败: " + e.getMessage());
        }
    }

    // ==================== 工具方法 ====================

    /**
     * 安全获取类
     *
     * @param classNames 可能的类全限定名（按优先级排列）
     * @return 找到的类，未找到返回 null
     */
    private static Class<?> safeGetClass(String... classNames) {
        for (String className : classNames) {
            try {
                return Class.forName(className);
            } catch (ClassNotFoundException e) {
                // 继续尝试下一个类名
            }
        }
        return null;
    }

    /**
     * 安全获取方法
     *
     * @param clazz          目标类
     * @param methodNames    可能的方法名（按优先级排列）
     * @param parameterTypes 参数类型
     * @return 找到的方法，未找到返回 null
     */
    private static Method safeGetMethod(Class<?> clazz, String[] methodNames, Class<?>... parameterTypes) {
        if (clazz == null) {
            return null;
        }

        for (String methodName : methodNames) {
            try {
                return clazz.getDeclaredMethod(methodName, parameterTypes);
            } catch (NoSuchMethodException e) {
                // 继续尝试下一个方法名
            }
        }
        return null;
    }

    /**
     * 安全获取方法（单方法名版本）
     *
     * @param clazz          目标类
     * @param methodName     方法名
     * @param parameterTypes 参数类型
     * @return 找到的方法，未找到返回 null
     */
    private static Method safeGetMethod(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        return safeGetMethod(clazz, new String[]{methodName}, parameterTypes);
    }

    /**
     * 安全获取字段
     *
     * @param clazz      目标类
     * @param fieldNames 可能的字段名（按优先级排列）
     * @return 找到的字段，未找到返回 null
     */
    private static Field safeGetField(Class<?> clazz, String... fieldNames) {
        if (clazz == null) {
            return null;
        }

        for (String fieldName : fieldNames) {
            try {
                Field field = clazz.getField(fieldName);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException e) {
                try {
                    Field field = clazz.getDeclaredField(fieldName);
                    field.setAccessible(true);
                    return field;
                } catch (NoSuchFieldException ex) {
                    // 继续尝试下一个字段名
                }
            }
        }
        return null;
    }

    // ==================== 公共方法 ====================

    /**
     * 获取服务器版本字符串
     *
     * @return 版本字符串，如 v1_12_R1
     */
    public static String getVersion() {
        return version;
    }

    /**
     * 获取 NMS 包路径
     *
     * @return NMS 包的完整路径
     */
    public static String getNMSPack() {
        // 1.17+ 版本使用新的包结构
        if (version.contains("v1_17") || version.contains("v1_18") ||
                version.contains("v1_19") || version.contains("v1_20") ||
                version.contains("v1_21")) {
            return "net.minecraft.server";
        } else {
            return "net.minecraft.server." + version;
        }
    }

    /**
     * 获取 OBC（org.bukkit.craftbukkit）包路径
     *
     * @return OBC 包的完整路径
     */
    public static String getOBCPack() {
        return "org.bukkit.craftbukkit." + version;
    }

    /**
     * 检查反射是否初始化成功
     *
     * @return 初始化成功返回 true，否则返回 false
     */
    public static boolean isInitialized() {
        return initialized;
    }

    /**
     * 获取初始化错误信息
     *
     * @return 错误信息，如果初始化成功则返回 null
     */
    public static String getInitError() {
        return initError;
    }

    /**
     * 判断是否为1.13+版本
     *
     * @return 是则返回true
     */
    public static boolean isHighVersion() {
        return version.contains("v1_13") || version.contains("v1_14") ||
                version.contains("v1_15") || version.contains("v1_16") ||
                version.contains("v1_17") || version.contains("v1_18") ||
                version.contains("v1_19") || version.contains("v1_20") ||
                version.contains("v1_21");
    }

    /**
     * 判断是否为1.17+版本（Mojang映射）
     *
     * @return 是则返回true
     */
    public static boolean isMojangMapped() {
        return version.contains("v1_17") || version.contains("v1_18") ||
                version.contains("v1_19") || version.contains("v1_20") ||
                version.contains("v1_21");
    }

    /**
     * 获取主版本号（如1.12返回12，1.20返回20）
     *
     * @return 主版本号，解析失败返回0
     */
    public static int getMajorVersion() {
        try {
            // v1_12_R1 -> 12
            String[] parts = version.split("_");
            if (parts.length >= 2) {
                return Integer.parseInt(parts[1]);
            }
        } catch (NumberFormatException ignored) {
        }
        return 0;
    }

    /**
     * 判断当前版本是否大于等于指定版本
     *
     * @param major 主版本号（如12表示1.12）
     * @return 是则返回true
     */
    public static boolean isVersionAtLeast(int major) {
        return getMajorVersion() >= major;
    }
}