package pers.tany.yukinoaapi;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @description: NMS 跨版本兼容工具类（支持 1.7-1.21，包括 NeoForge）
 */
public class NMSUtil {
    private static final Logger logger = Logger.getLogger("YukiNoaAPI");
    private static final String version;
    private static boolean initialized = false;
    private static String initError = null;

    // ==================== 玩家相关 ====================
    public static Field playerConnection;
    public static Class<?> packetPlayOutChat;
    public static Method send;
    public static Method getHandle_Player;
    public static Class<?> iChatBaseComponent;
    public static Class<?> chatComponentText;
    public static Class<?> chatMessageType;

    // ==================== 物品相关 ====================
    public static Class<?> nbttagCompoundClass;
    public static Class<?> craftItemStack;
    public static Method asNMSCopy;
    public static Method save_ItemStack;
    public static Class<?> nmsItemStackClass;
    public static Method getName_ItemStack;
    public static Method getItem;
    public static Class<?> nmsItemClass;
    public static Method getName_Item;

    // ==================== 服务器相关 ====================
    public static Class<?> minecraftServer;
    public static Method getServer;
    public static Class<?> craftServerClass;

    // ==================== 实体相关 ====================
    public static Class<?> craftEntityClass;
    public static Class<?> nmsEntityClass;
    public static Method getHandle_Entity;
    public static Method getSaveID;
    public static Method getName_Entity;
    public static Method getDisplayName_Entity;
    public static Method save_Entity;
    public static Method setNbttagCompound;
    public static Method getEntity;

    // ==================== NBT 解析相关 ====================
    public static Class<?> mojangsonParser;
    public static Method parse;
    public static Method setTag;
    public static Method load_ItemStack;
    public static Method asBukkitCopy;

    // ==================== 方块实体相关 ====================
    public static Class<?> craftWorldClass;
    public static Method getHandle_Word;
    public static Class<?> nmsWorldClass;
    public static Class<?> tileEntityClass;
    public static Class<?> blockPositionClass;
    public static Method getTileEntity;
    public static Method save_Block;
    public static Method load_Block;
    public static Method setData;
    public static Class<?> craftBlockClass;
    public static Method setTileEntity;

    // ==================== 文本组件相关 ====================
    public static Class<?> iChatBaseComponentClass;
    public static Method getString_IChatBaseComponent;

    // ==================== 附魔相关 ====================
    public static Class<?> craftEnchantmentClass;
    public static Class<?> nmsEnchantmentClass;
    public static Method getHandle_Enchantment;
    public static Method getName_Enchantment;
    public static Field target;

    static {
        version = detectVersion();
        logger.info("[NMSUtil] 检测到服务器版本: " + version);

        try {
            initializeReflection();
            initialized = true;
            logger.info("[NMSUtil] 反射初始化成功");
        } catch (Exception e) {
            initError = e.getMessage();
            logger.log(Level.SEVERE, "[NMSUtil] 反射初始化失败", e);
        }
    }

    /**
     * @description: 检测服务器版本
     * @return String - 版本字符串
     */
    private static String detectVersion() {
        try {
            String packageName = Bukkit.getServer().getClass().getPackage().getName();
            logger.info("[NMSUtil] 服务器完整包名: " + packageName);

            String[] parts = packageName.split("\\.");
            logger.info("[NMSUtil] 包名分段数: " + parts.length);

            // 标准 CraftBukkit 结构: org.bukkit.craftbukkit.v1_xx_Rx
            // parts[0]=org, parts[1]=bukkit, parts[2]=craftbukkit, parts[3]=v1_xx_Rx
            if (parts.length >= 4 && parts[2].equals("craftbukkit")) {
                String detectedVersion = parts[3];
                logger.info("[NMSUtil] 从包名提取版本: " + detectedVersion);
                return detectedVersion;
            }

            // 备用方案：在所有分段中查找版本号
            for (String part : parts) {
                if (part.startsWith("v1_")) {
                    logger.info("[NMSUtil] 在包名中找到版本: " + part);
                    return part;
                }
            }

            // 最后的备用方案
            logger.warning("[NMSUtil] 无法从包名检测版本，使用默认值");
            return "v1_21_R1";

        } catch (Exception e) {
            logger.log(Level.WARNING, "[NMSUtil] 版本检测异常", e);
            return "v1_21_R1";
        }
    }

    /**
     * @description: 初始化所有反射类和方法
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
     * @description: 初始化玩家相关反射
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

        } catch (Exception e) {
            logger.fine("[NMSUtil] 玩家相关反射初始化部分失败: " + e.getMessage());
        }
    }

    /**
     * @description: 初始化物品相关反射
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

        } catch (Exception e) {
            logger.fine("[NMSUtil] 物品相关反射初始化部分失败: " + e.getMessage());
        }
    }

    /**
     * @description: 初始化服务器相关反射
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

        } catch (Exception e) {
            logger.fine("[NMSUtil] 服务器相关反射初始化部分失败: " + e.getMessage());
        }
    }

    /**
     * @description: 初始化实体相关反射
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
                // 保存方法
                save_Entity = safeGetMethod(
                        nmsEntityClass,
                        new String[]{"save", "f", "e"},
                        nbttagCompoundClass
                );

                // 加载方法
                setNbttagCompound = safeGetMethod(
                        nmsEntityClass,
                        new String[]{"load", "f", "e"},
                        nbttagCompoundClass
                );

                // 获取名称方法（返回 IChatBaseComponent）
                getName_Entity = safeGetMethod(
                        nmsEntityClass,
                        new String[]{"getName", "ah", "cq"}
                );

                // 获取显示名称（返回 IChatBaseComponent）
                getDisplayName_Entity = safeGetMethod(
                        nmsEntityClass,
                        new String[]{"getDisplayName", "ah", "C_"}
                );

                // 获取保存 ID（1.21+ 使用 bD 方法）
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

        } catch (Exception e) {
            logger.fine("[NMSUtil] 实体相关反射初始化部分失败: " + e.getMessage());
        }
    }

    /**
     * @description: 初始化 NBT 解析相关反射
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

        } catch (Exception e) {
            logger.fine("[NMSUtil] NBT 解析相关反射初始化部分失败: " + e.getMessage());
        }
    }

    /**
     * @description: 初始化方块实体相关反射
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

        } catch (Exception e) {
            logger.fine("[NMSUtil] 方块实体相关反射初始化部分失败: " + e.getMessage());
        }
    }

    /**
     * @description: 初始化文本组件相关反射
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

        } catch (Exception e) {
            logger.fine("[NMSUtil] 文本组件相关反射初始化部分失败: " + e.getMessage());
        }
    }

    /**
     * @description: 初始化附魔相关反射
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

        } catch (Exception e) {
            logger.fine("[NMSUtil] 附魔相关反射初始化部分失败: " + e.getMessage());
        }
    }

    /**
     * @description: 设置所有反射方法为可访问
     */
    private static void setAllMethodsAccessible() {
        try {
            for (Field field : NMSUtil.class.getFields()) {
                Object value = field.get(null);
                if (value instanceof Method) {
                    ((Method) value).setAccessible(true);
                }
            }
        } catch (Exception e) {
            logger.fine("[NMSUtil] 设置方法可访问性失败: " + e.getMessage());
        }
    }

    // ==================== 工具方法 ====================

    /**
     * @description: 安全获取类
     * @param classNames - 可能的类全限定名
     * @return Class<?> - 找到的类，未找到返回 null
     */
    private static Class<?> safeGetClass(String... classNames) {
        for (String className : classNames) {
            try {
                return Class.forName(className);
            } catch (ClassNotFoundException e) {
                // 继续尝试
            }
        }
        return null;
    }

    /**
     * @description: 安全获取方法
     * @param clazz - 目标类
     * @param methodNames - 可能的方法名
     * @param parameterTypes - 参数类型
     * @return Method - 找到的方法，未找到返回 null
     */
    private static Method safeGetMethod(Class<?> clazz, String[] methodNames, Class<?>... parameterTypes) {
        if (clazz == null) {
            return null;
        }

        for (String methodName : methodNames) {
            try {
                return clazz.getDeclaredMethod(methodName, parameterTypes);
            } catch (NoSuchMethodException e) {
                // 继续尝试
            }
        }
        return null;
    }

    /**
     * @description: 安全获取方法（单方法名）
     */
    private static Method safeGetMethod(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        return safeGetMethod(clazz, new String[]{methodName}, parameterTypes);
    }

    /**
     * @description: 安全获取字段
     * @param clazz - 目标类
     * @param fieldNames - 可能的字段名
     * @return Field - 找到的字段，未找到返回 null
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
                    // 继续尝试
                }
            }
        }
        return null;
    }

    public static String getVersion() {
        return version;
    }

    public static String getNMSPack() {
        if (version.contains("v1_20") || version.contains("v1_21")) {
            return "net.minecraft.server";
        } else {
            return "net.minecraft.server." + version;
        }
    }

    public static String getOBCPack() {
        return "org.bukkit.craftbukkit." + version;
    }

    public static boolean isInitialized() {
        return initialized;
    }

    public static String getInitError() {
        return initError;
    }
}