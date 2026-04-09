package pers.tany.yukinoaapi.util;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import pers.tany.yukinoaapi.Main;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * NMS 跨版本兼容工具类
 * 支持 Minecraft 1.7-1.21+ 版本
 * <p>
 * 兼容 Spigot（带版本后缀的OBC包）和 Paper 1.20.5+（无版本后缀的OBC包）。
 * </p>
 *
 * @author tany
 * @since 1.0.0
 */
public class NMSUtil {

    /**
     * 服务器版本字符串（如 v1_12_R1）
     */
    private static final String version;

    /**
     * 主版本号缓存（如 1.12 返回 12）
     */
    private static final int majorVersion;

    /**
     * OBC 包是否包含版本后缀
     * <p>
     * Spigot / 旧版 Paper: org.bukkit.craftbukkit.v1_xx_Rx（带版本后缀）<br>
     * Paper 1.20.5+: org.bukkit.craftbukkit（无版本后缀）
     * </p>
     */
    private static final boolean obcVersioned;

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
     * <p><b>仅 1.8.x 使用</b> - 1.9+ 使用 Spigot API，1.7 不支持 ActionBar</p>
     */
    public static Class<?> packetPlayOutChat;

    /**
     * 发送数据包方法
     */
    public static Method send;

    /**
     * 获取玩家 NMS 句柄方法
     */
    public static Method getHandle_Player;

    /**
     * 聊天组件接口
     * <p><b>仅 1.8.x 使用</b> - 1.9+ 使用 Spigot API</p>
     */
    public static Class<?> iChatBaseComponent;

    /**
     * 文本聊天组件类
     * <p><b>仅 1.8.x 使用</b> - 1.9+ 使用 Spigot API，1.19+ 该类已移除</p>
     */
    public static Class<?> chatComponentText;

    /**
     * 聊天消息类型枚举
     * <p><b>仅 1.8.x 使用</b> - 1.9+ 使用 Spigot API，1.19+ 该类已重构</p>
     */
    public static Class<?> chatMessageType;

    // ==================== 物品相关 ====================

    /**
     * NBT复合标签类
     * <p>1.20.4-: net.minecraft.nbt.NBTTagCompound（Spigot映射）</p>
     * <p>1.20.5+: net.minecraft.nbt.CompoundTag（Mojang映射）</p>
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
     * 保存物品NBT方法（旧版：1.20.4及以下）
     * <p>方法签名：ItemStack.save(NBTTagCompound) → NBTTagCompound</p>
     * <p><b>1.20.5+ 该方法已移除，使用 {@link #save_ItemStack_v2} 代替</b></p>
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

    // ==================== 1.20.5+ 新版物品相关 ====================

    /**
     * NBT Tag 基类接口
     * <p>1.20.4-: net.minecraft.nbt.NBTBase（Spigot映射）</p>
     * <p>1.20.5+: net.minecraft.nbt.Tag（Mojang映射）</p>
     */
    public static Class<?> tagClass;

    /**
     * HolderLookup.Provider 类（1.20.5+ 注册表上下文）
     * <p>用于序列化/反序列化物品等需要注册表信息的操作</p>
     */
    public static Class<?> holderLookupProviderClass;

    /**
     * 保存物品NBT方法（新版：1.20.5+）
     * <p>方法签名：ItemStack.save(HolderLookup.Provider) → Tag</p>
     * <p>替代旧版 {@link #save_ItemStack}</p>
     */
    public static Method save_ItemStack_v2;

    /**
     * 静态解析物品方法（新版：1.20.5+）
     * <p>方法签名：ItemStack.parse(HolderLookup.Provider, Tag) → Optional&lt;ItemStack&gt;</p>
     * <p>替代旧版 {@link #load_ItemStack}</p>
     */
    public static Method parse_ItemStack_static;

    /**
     * 获取注册表上下文方法（1.20.5+）
     * <p>方法签名：MinecraftServer.registryAccess() → RegistryAccess</p>
     */
    public static Method registryAccess;

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
     * <p>1.20.4-: net.minecraft.nbt.MojangsonParser（Spigot映射）</p>
     * <p>1.20.5+: net.minecraft.nbt.TagParser（Mojang映射）</p>
     */
    public static Class<?> mojangsonParser;

    /**
     * 解析NBT字符串方法
     * <p>1.20.4-: MojangsonParser.parse(String)</p>
     * <p>1.20.5+: TagParser.parseTag(String)</p>
     */
    public static Method parse;

    /**
     * 设置物品NBT方法（旧版：1.20.4及以下）
     * <p><b>1.20.5+ 该方法已移除（DataComponents系统取代了tag复合标签）</b></p>
     */
    public static Method setTag;

    /**
     * 从NBT加载物品方法（旧版：1.20.4及以下）
     * <p><b>1.20.5+ 该方法已移除，使用 {@link #parse_ItemStack_static} 代替</b></p>
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
     * <p><b>1.8+ 可用</b> - 1.7 使用 xyz 参数</p>
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
     * <p><b>仅 1.12 及以下使用</b> - 1.13+ 使用 BlockData</p>
     */
    public static Method setData;

    /**
     * CraftBlock类
     */
    public static Class<?> craftBlockClass;

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

    /**
     * 获取语言文本（辅助方法）
     *
     * @param key  语言键
     * @param args 参数
     * @return 语言文本
     */
    private static String getLang(String key, Object... args) {
        if (Main.plugin == null || ((Main) Main.plugin).getLanguageManager() == null) {
            return key;
        }

        return ((Main) Main.plugin).getLanguageManager().getMessage(key, args);
    }

    /*
     * 静态初始化块
     * 检测服务器版本并初始化所有反射对象
     */
    static {
        version = detectVersion();
        majorVersion = parseMajorVersion(version);
        obcVersioned = detectObcVersioned();
        LogUtil.info(getLang("nms.detected-version", version));
        LogUtil.debug("[NMS] OBC包路径: " + getOBCPack() + " (版本后缀: " + obcVersioned + ")");

        try {
            initializeReflection();
            initialized = true;
            LogUtil.success(getLang("nms.init-success"));
        } catch (Exception e) {
            initError = e.getMessage();
            LogUtil.error(getLang("nms.init-fail"), e);
        }
    }

    /**
     * 检测服务器版本
     * <p>
     * 优先从 CraftBukkit 包路径中提取（标准格式: org.bukkit.craftbukkit.v1_xx_Rx），
     * 若包路径中无版本号（Paper 1.20.5+ / Mohist 等），则从 Bukkit.getBukkitVersion() 解析构造。
     * </p>
     *
     * @return 版本字符串（如 v1_20_R1）
     */
    private static String detectVersion() {
        try {
            String packageName = Bukkit.getServer().getClass().getPackage().getName();
            LogUtil.debug(getLang("nms.server-package", packageName));

            String[] parts = packageName.split("\\.");
            LogUtil.debug(getLang("nms.package-segments", parts.length));

            // 标准 CraftBukkit 结构: org.bukkit.craftbukkit.v1_xx_Rx
            if (parts.length >= 4 && parts[2].equals("craftbukkit")) {
                String candidate = parts[3];
                if (candidate.startsWith("v1_")) {
                    LogUtil.debug(getLang("nms.version-from-package", candidate));
                    return candidate;
                }
            }

            // 备用方案：在所有分段中查找版本号
            for (String part : parts) {
                if (part.startsWith("v1_")) {
                    LogUtil.debug(getLang("nms.version-found", part));
                    return part;
                }
            }
        } catch (Exception e) {
            LogUtil.error("[NMS] 从包名检测版本失败: " + e.getMessage());
        }

        // 最终备用方案：从 Bukkit.getBukkitVersion() 解析（如 "1.20.1-R0.1-SNAPSHOT"）
        return detectVersionFromBukkitAPI();
    }

    /**
     * 从 Bukkit API 版本字符串解析并构造 NMS 版本号
     * <p>
     * Bukkit.getBukkitVersion() 返回格式如 "1.21.4-R0.1-SNAPSHOT"，
     * 从中提取主版本号和修订号构造为 "v1_21_R1" 格式。
     * 适用于 Paper 1.20.5+（OBC包路径无版本后缀）和 Mohist 等服务端。
     * </p>
     *
     * @return 构造的版本字符串（如 v1_21_R1），解析失败返回 v1_21_R1
     */
    private static String detectVersionFromBukkitAPI() {
        try {
            // 格式: "1.21.4-R0.1-SNAPSHOT" 或 "1.20-R0.1-SNAPSHOT"
            String bukkitVersion = Bukkit.getBukkitVersion();
            LogUtil.debug("[NMS] Bukkit版本字符串: " + bukkitVersion);

            // 分割出版本部分和修订部分
            String[] dashParts = bukkitVersion.split("-");
            if (dashParts.length < 2) {
                LogUtil.warning("[NMS] 无法解析Bukkit版本: " + bukkitVersion);
                return "v1_21_R1";
            }

            // 解析主版本号（如 "1.21.4" → major=21）
            String mcVersion = dashParts[0]; // "1.21.4" 或 "1.20"
            String[] versionParts = mcVersion.split("\\.");
            if (versionParts.length < 2) {
                LogUtil.warning("[NMS] 无法解析MC版本号: " + mcVersion);
                return "v1_21_R1";
            }

            String major = versionParts[1]; // "21"

            // 解析修订号（如 "R0.1" → revision=1）
            String revisionPart = dashParts[1]; // "R0.1"
            String revision = "1"; // 默认修订号
            if (revisionPart.startsWith("R")) {
                // 取 R 后面的第一个数字作为修订号
                String revNum = revisionPart.substring(1).split("\\.")[0];
                if (!revNum.isEmpty()) {
                    int rev = Integer.parseInt(revNum);
                    revision = String.valueOf(Math.max(rev, 1));
                }
            }

            String constructed = "v1_" + major + "_R" + revision;
            LogUtil.info("[NMS] 从Bukkit API构造版本号: " + constructed + " (源: " + bukkitVersion + ")");
            return constructed;

        } catch (Exception e) {
            LogUtil.error("[NMS] 从Bukkit API检测版本失败，使用默认值 v1_21_R1", e);
            return "v1_21_R1";
        }
    }

    /**
     * 检测 OBC 包路径是否包含版本后缀
     * <p>
     * Paper 1.20.5+ 移除了 OBC 包的版本后缀，包路径从
     * org.bukkit.craftbukkit.v1_xx_Rx 变为 org.bukkit.craftbukkit。
     * Spigot / 旧版 Paper 仍保留版本后缀。
     * 通过尝试加载 CraftServer 类来运行时判断。
     * </p>
     *
     * @return 如果 OBC 包含版本后缀返回 true，否则返回 false
     */
    private static boolean detectObcVersioned() {
        // 优先尝试带版本后缀（Spigot / 旧版 Paper）
        try {
            Class.forName("org.bukkit.craftbukkit." + version + ".CraftServer");
            return true;
        } catch (ClassNotFoundException ignored) {
        }

        // 尝试不带版本后缀（Paper 1.20.5+ / Mohist 等）
        try {
            Class.forName("org.bukkit.craftbukkit.CraftServer");
            LogUtil.info("[NMS] 检测到无版本后缀的OBC包（Paper 1.20.5+ 或类似服务端）");
            return false;
        } catch (ClassNotFoundException ignored) {
        }

        // 都找不到，默认使用带版本后缀
        LogUtil.warning("[NMS] 无法确定OBC包结构，默认使用版本后缀");
        return true;
    }

    /**
     * 解析主版本号
     *
     * @param versionStr 版本字符串（如 v1_12_R1）
     * @return 主版本号（如 12），解析失败返回 0
     */
    private static int parseMajorVersion(String versionStr) {
        try {
            // v1_12_R1 -> 12
            String[] parts = versionStr.split("_");
            if (parts.length >= 2) {
                return Integer.parseInt(parts[1]);
            }
        } catch (NumberFormatException ignored) {
        }
        return 0;
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
        // 1.20.5+ 新版本初始化（必须在 Server、Item、NBT 之后执行，依赖这些模块的类）
        initNewVersionRelated();
        setAllMethodsAccessible();
    }

    /**
     * 初始化玩家相关反射
     */
    private static void initPlayerRelated() {
        LogUtil.debug(getLang("nms.module-init-start", "Player"));

        try {
            // playerConnection 字段
            playerConnection = safeGetField(
                    safeGetClass(getNMSPack() + ".EntityPlayer", "net.minecraft.server.level.EntityPlayer"),
                    "playerConnection", "c"
            );
            logFieldStatus("playerConnection", playerConnection);

            // PacketPlayOutChat 类（仅 1.8 使用，1.9+ 用 Spigot API，1.19+ 已移除）
            packetPlayOutChat = safeGetClass(
                    getNMSPack() + ".PacketPlayOutChat",
                    "net.minecraft.network.protocol.game.PacketPlayOutChat"
            );
            logClassStatus("PacketPlayOutChat", packetPlayOutChat);

            // PlayerConnection 类
            Class<?> playerConnectionClass = safeGetClass(
                    getNMSPack() + ".PlayerConnection",
                    "net.minecraft.server.network.PlayerConnection"
            );
            logClassStatus("PlayerConnection", playerConnectionClass);

            // Packet 类
            Class<?> packetClass = safeGetClass(
                    getNMSPack() + ".Packet",
                    "net.minecraft.network.protocol.Packet"
            );
            logClassStatus("Packet", packetClass);

            // sendPacket 方法
            if (playerConnectionClass != null && packetClass != null) {
                send = safeGetMethod(
                        playerConnectionClass,
                        new String[]{"sendPacket", "a", "b"},
                        packetClass
                );
                logMethodStatus("sendPacket", send);
            }

            // CraftPlayer 类
            Class<?> craftPlayerClass = safeGetClass(getOBCPack() + ".entity.CraftPlayer");
            logClassStatus("CraftPlayer", craftPlayerClass);

            if (craftPlayerClass != null) {
                getHandle_Player = safeGetMethod(craftPlayerClass, "getHandle");
                logMethodStatus("CraftPlayer.getHandle", getHandle_Player);
            }

            // IChatBaseComponent 接口（仅 1.8 使用）
            iChatBaseComponent = safeGetClass(
                    getNMSPack() + ".IChatBaseComponent",
                    "net.minecraft.network.chat.IChatBaseComponent"
            );
            logClassStatus("IChatBaseComponent", iChatBaseComponent);

            // ChatComponentText 类（仅 1.8 使用，1.19+ 已移除）
            chatComponentText = safeGetClass(
                    getNMSPack() + ".ChatComponentText",
                    "net.minecraft.network.chat.ChatComponentText"
            );
            logClassStatus("ChatComponentText", chatComponentText);

            // ChatMessageType 枚举（仅 1.8 使用，1.19+ 已重构）
            chatMessageType = safeGetClass(
                    getNMSPack() + ".ChatMessageType",
                    "net.minecraft.network.chat.ChatMessageType"
            );
            logClassStatus("ChatMessageType", chatMessageType);

            LogUtil.debug(getLang("nms.module-init-complete", "Player"));
        } catch (Exception e) {
            LogUtil.error(getLang("nms.module-init-fail", "Player", e.getMessage()));
        }
    }

    /**
     * 初始化物品相关反射
     */
    private static void initItemRelated() {
        LogUtil.debug(getLang("nms.module-init-start", "Item"));

        try {
            // CraftItemStack 类
            craftItemStack = safeGetClass(getOBCPack() + ".inventory.CraftItemStack");
            logClassStatus("CraftItemStack", craftItemStack);

            // NBTTagCompound 类（兼容新旧映射名）
            nbttagCompoundClass = safeGetClass(
                    getNMSPack() + ".NBTTagCompound",
                    "net.minecraft.nbt.NBTTagCompound",
                    "net.minecraft.nbt.CompoundTag"          // 1.20.5+ Mojang映射
            );
            logClassStatus("NBTTagCompound/CompoundTag", nbttagCompoundClass);

            // NMS ItemStack 类
            nmsItemStackClass = safeGetClass(
                    getNMSPack() + ".ItemStack",
                    "net.minecraft.world.item.ItemStack"
            );
            logClassStatus("NMS ItemStack", nmsItemStackClass);

            if (craftItemStack != null && nmsItemStackClass != null) {
                // asNMSCopy 方法（全版本通用，CraftItemStack 上的静态方法）
                asNMSCopy = safeGetMethod(craftItemStack, "asNMSCopy", ItemStack.class);
                logMethodStatus("asNMSCopy", asNMSCopy);

                // asBukkitCopy 方法（全版本通用）
                asBukkitCopy = safeGetMethod(craftItemStack, "asBukkitCopy", nmsItemStackClass);
                logMethodStatus("asBukkitCopy", asBukkitCopy);
            }

            if (nmsItemStackClass != null && nbttagCompoundClass != null) {
                // save 方法（旧版：save(NBTTagCompound)，1.20.5+ 该签名已移除）
                save_ItemStack = safeGetMethod(
                        nmsItemStackClass,
                        new String[]{"save", "b"},
                        nbttagCompoundClass
                );
                logMethodStatus("ItemStack.save(NBTTagCompound) [旧版]", save_ItemStack);

                // setTag 方法（旧版：setTag(NBTTagCompound)，1.20.5+ 已移除）
                setTag = safeGetMethod(
                        nmsItemStackClass,
                        new String[]{"setTag", "c"},
                        nbttagCompoundClass
                );
                logMethodStatus("ItemStack.setTag [旧版]", setTag);

                // load 方法（旧版：load(NBTTagCompound)，1.20.5+ 已移除）
                load_ItemStack = safeGetMethod(
                        nmsItemStackClass,
                        new String[]{"load", "c", "a"},
                        nbttagCompoundClass
                );
                logMethodStatus("ItemStack.load [旧版]", load_ItemStack);

                // getName 方法
                getName_ItemStack = safeGetMethod(
                        nmsItemStackClass,
                        new String[]{"getName", "y", "q", "p", "m"}
                );
                logMethodStatus("ItemStack.getName", getName_ItemStack);

                // getItem 方法（h = 1.21 Mojang映射）
                getItem = safeGetMethod(
                        nmsItemStackClass,
                        new String[]{"getItem", "d", "c", "o", "h"}
                );
                logMethodStatus("ItemStack.getItem", getItem);
            }

            // NMS Item 类
            nmsItemClass = safeGetClass(
                    getNMSPack() + ".Item",
                    "net.minecraft.world.item.Item"
            );
            logClassStatus("NMS Item", nmsItemClass);

            if (nmsItemClass != null && nmsItemStackClass != null) {
                // Item.getName 方法
                getName_Item = safeGetMethod(
                        nmsItemClass,
                        new String[]{"h", "m"},
                        nmsItemStackClass
                );
                logMethodStatus("Item.getName", getName_Item);
            }

            LogUtil.debug(getLang("nms.module-init-complete", "Item"));
        } catch (Exception e) {
            LogUtil.error(getLang("nms.module-init-fail", "Item", e.getMessage()));
        }
    }

    /**
     * 初始化服务器相关反射
     */
    private static void initServerRelated() {
        LogUtil.debug(getLang("nms.module-init-start", "Server"));

        try {
            // MinecraftServer 类
            minecraftServer = safeGetClass(
                    getNMSPack() + ".MinecraftServer",
                    "net.minecraft.server.MinecraftServer"
            );
            logClassStatus("MinecraftServer", minecraftServer);

            if (minecraftServer != null) {
                // getServer 方法
                getServer = safeGetMethod(minecraftServer, "getServer");
                logMethodStatus("MinecraftServer.getServer", getServer);
            }

            // CraftServer 类
            craftServerClass = safeGetClass(getOBCPack() + ".CraftServer");
            logClassStatus("CraftServer", craftServerClass);

            LogUtil.debug(getLang("nms.module-init-complete", "Server"));
        } catch (Exception e) {
            LogUtil.error(getLang("nms.module-init-fail", "Server", e.getMessage()));
        }
    }

    /**
     * 初始化实体相关反射
     */
    private static void initEntityRelated() {
        LogUtil.debug(getLang("nms.module-init-start", "Entity"));

        try {
            // CraftEntity 类
            craftEntityClass = safeGetClass(getOBCPack() + ".entity.CraftEntity");
            logClassStatus("CraftEntity", craftEntityClass);

            if (craftEntityClass != null) {
                // getHandle 方法
                getHandle_Entity = safeGetMethod(craftEntityClass, "getHandle");
                logMethodStatus("CraftEntity.getHandle", getHandle_Entity);
            }

            // NMS Entity 类
            nmsEntityClass = safeGetClass(
                    getNMSPack() + ".Entity",
                    "net.minecraft.world.entity.Entity"
            );
            logClassStatus("NMS Entity", nmsEntityClass);

            if (nmsEntityClass != null && nbttagCompoundClass != null) {
                // save 方法
                save_Entity = safeGetMethod(
                        nmsEntityClass,
                        new String[]{"save", "f", "e"},
                        nbttagCompoundClass
                );
                logMethodStatus("Entity.save", save_Entity);

                // load 方法
                setNbttagCompound = safeGetMethod(
                        nmsEntityClass,
                        new String[]{"load", "f", "e"},
                        nbttagCompoundClass
                );
                logMethodStatus("Entity.load", setNbttagCompound);

                // getName 方法
                getName_Entity = safeGetMethod(
                        nmsEntityClass,
                        new String[]{"getName", "ah", "cq"}
                );
                logMethodStatus("Entity.getName", getName_Entity);

                // getDisplayName 方法
                getDisplayName_Entity = safeGetMethod(
                        nmsEntityClass,
                        new String[]{"getDisplayName", "ah", "C_"}
                );
                logMethodStatus("Entity.getDisplayName", getDisplayName_Entity);

                // getSaveID 方法
                getSaveID = safeGetMethod(
                        nmsEntityClass,
                        new String[]{"getSaveID", "bD", "bn"}
                );
                logMethodStatus("Entity.getSaveID", getSaveID);
            }

            if (craftEntityClass != null && craftServerClass != null && nmsEntityClass != null) {
                // getEntity 方法
                getEntity = safeGetMethod(
                        craftEntityClass,
                        "getEntity",
                        craftServerClass,
                        nmsEntityClass
                );
                logMethodStatus("CraftEntity.getEntity", getEntity);
            }

            LogUtil.debug(getLang("nms.module-init-complete", "Entity"));
        } catch (Exception e) {
            LogUtil.error(getLang("nms.module-init-fail", "Entity", e.getMessage()));
        }
    }

    /**
     * 初始化 NBT 解析相关反射
     */
    private static void initNBTRelated() {
        LogUtil.debug(getLang("nms.module-init-start", "NBT"));

        try {
            // MojangsonParser / TagParser 类（兼容新旧映射名）
            mojangsonParser = safeGetClass(
                    getNMSPack() + ".MojangsonParser",
                    "net.minecraft.nbt.MojangsonParser",
                    "net.minecraft.nbt.TagParser"            // 1.20.5+ Mojang映射
            );
            logClassStatus("MojangsonParser/TagParser", mojangsonParser);

            if (mojangsonParser != null) {
                // parse / parseTag 方法（兼容新旧方法名）
                parse = safeGetMethod(
                        mojangsonParser,
                        new String[]{"parse", "parseTag", "a"},
                        String.class
                );
                logMethodStatus("MojangsonParser.parse / TagParser.parseTag", parse);
            }

            LogUtil.debug(getLang("nms.module-init-complete", "NBT"));
        } catch (Exception e) {
            LogUtil.error(getLang("nms.module-init-fail", "NBT", e.getMessage()));
        }
    }

    /**
     * 初始化方块实体相关反射
     */
    private static void initBlockRelated() {
        LogUtil.debug(getLang("nms.module-init-start", "Block"));

        try {
            // CraftWorld 类
            craftWorldClass = safeGetClass(getOBCPack() + ".CraftWorld");
            logClassStatus("CraftWorld", craftWorldClass);

            if (craftWorldClass != null) {
                // getHandle 方法
                getHandle_Word = safeGetMethod(craftWorldClass, "getHandle");
                logMethodStatus("CraftWorld.getHandle", getHandle_Word);
            }

            // NMS World 类
            nmsWorldClass = safeGetClass(
                    getNMSPack() + ".World",
                    "net.minecraft.world.level.World"
            );
            logClassStatus("NMS World", nmsWorldClass);

            // TileEntity 类
            tileEntityClass = safeGetClass(
                    getNMSPack() + ".TileEntity",
                    "net.minecraft.world.level.block.entity.TileEntity"
            );
            logClassStatus("TileEntity", tileEntityClass);

            // BlockPosition 类（1.8+ 可用）
            blockPositionClass = safeGetClass(
                    getNMSPack() + ".BlockPosition",
                    "net.minecraft.core.BlockPosition"
            );
            logClassStatus("BlockPosition", blockPositionClass);

            if (nmsWorldClass != null && tileEntityClass != null) {
                // getTileEntity 方法（优先使用 BlockPosition 参数）
                if (blockPositionClass != null) {
                    getTileEntity = safeGetMethod(
                            nmsWorldClass,
                            new String[]{"getTileEntity", "c_"},
                            blockPositionClass
                    );
                    logMethodStatus("World.getTileEntity", getTileEntity);
                }

                // 1.7 备用：使用 xyz 参数
                if (getTileEntity == null) {
                    getTileEntity = safeGetMethod(
                            nmsWorldClass,
                            "getTileEntity",
                            int.class, int.class, int.class
                    );
                    logMethodStatus("World.getTileEntity(xyz)", getTileEntity);
                }
            }

            if (tileEntityClass != null && nbttagCompoundClass != null) {
                // TileEntity.save 方法
                save_Block = safeGetMethod(
                        tileEntityClass,
                        new String[]{"save", "b"},
                        nbttagCompoundClass
                );
                logMethodStatus("TileEntity.save", save_Block);

                // TileEntity.load 方法
                load_Block = safeGetMethod(
                        tileEntityClass,
                        new String[]{"load", "a"},
                        nbttagCompoundClass
                );
                logMethodStatus("TileEntity.load", load_Block);
            }

            // CraftBlock 类
            craftBlockClass = safeGetClass(getOBCPack() + ".block.CraftBlock");
            logClassStatus("CraftBlock", craftBlockClass);

            if (craftBlockClass != null) {
                // setData 方法（仅 1.12 及以下）
                setData = safeGetMethod(craftBlockClass, "setData", byte.class);
                logMethodStatus("CraftBlock.setData", setData);
            }

            LogUtil.debug(getLang("nms.module-init-complete", "Block"));
        } catch (Exception e) {
            LogUtil.error(getLang("nms.module-init-fail", "Block", e.getMessage()));
        }
    }

    /**
     * 初始化文本组件相关反射
     */
    private static void initTextComponentRelated() {
        LogUtil.debug(getLang("nms.module-init-start", "TextComponent"));

        try {
            // IChatBaseComponent 类（兼容新旧映射名）
            iChatBaseComponentClass = safeGetClass(
                    getNMSPack() + ".IChatBaseComponent",
                    "net.minecraft.network.chat.IChatBaseComponent",
                    "net.minecraft.network.chat.Component"   // 1.20.5+ Mojang映射
            );
            logClassStatus("IChatBaseComponent/Component", iChatBaseComponentClass);

            if (iChatBaseComponentClass != null) {
                // getString 方法
                getString_IChatBaseComponent = safeGetMethod(
                        iChatBaseComponentClass,
                        new String[]{"getString", "a"}
                );
                logMethodStatus("IChatBaseComponent.getString", getString_IChatBaseComponent);
            }

            LogUtil.debug(getLang("nms.module-init-complete", "TextComponent"));
        } catch (Exception e) {
            LogUtil.error(getLang("nms.module-init-fail", "TextComponent", e.getMessage()));
        }
    }

    /**
     * 初始化附魔相关反射
     */
    private static void initEnchantmentRelated() {
        LogUtil.debug(getLang("nms.module-init-start", "Enchantment"));

        try {
            // CraftEnchantment 类
            craftEnchantmentClass = safeGetClass(getOBCPack() + ".enchantments.CraftEnchantment");
            logClassStatus("CraftEnchantment", craftEnchantmentClass);

            // NMS Enchantment 类
            nmsEnchantmentClass = safeGetClass(
                    getNMSPack() + ".Enchantment",
                    "net.minecraft.world.item.enchantment.Enchantment"
            );
            logClassStatus("NMS Enchantment", nmsEnchantmentClass);

            if (craftEnchantmentClass != null && nmsEnchantmentClass != null) {
                // getHandle 方法
                getHandle_Enchantment = safeGetMethod(
                        craftEnchantmentClass,
                        "getHandle"
                );
                logMethodStatus("CraftEnchantment.getHandle", getHandle_Enchantment);

                // getName 方法
                getName_Enchantment = safeGetMethod(
                        nmsEnchantmentClass,
                        new String[]{"d", "c"},
                        int.class
                );
                logMethodStatus("Enchantment.getName", getName_Enchantment);

                if (getHandle_Enchantment == null) {
                    // target 字段
                    target = safeGetField(craftEnchantmentClass, "target");
                    logFieldStatus("CraftEnchantment.target", target);
                }
            }

            LogUtil.debug(getLang("nms.module-init-complete", "Enchantment"));
        } catch (Exception e) {
            LogUtil.error(getLang("nms.module-init-fail", "Enchantment", e.getMessage()));
        }
    }

    /**
     * 初始化 1.20.5+ 新版本相关反射
     * <p>
     * 1.20.5 引入了 DataComponents 系统，替代了旧版的 NBTTagCompound 标签体系。
     * 物品序列化/反序列化方法签名发生了破坏性变更：
     * <ul>
     *   <li>save(NBTTagCompound) → save(HolderLookup.Provider) → Tag</li>
     *   <li>load(NBTTagCompound) → ItemStack.parse(HolderLookup.Provider, Tag) → Optional</li>
     *   <li>setTag(NBTTagCompound) → 已移除（DataComponents取代）</li>
     * </ul>
     * 本方法尝试查找新版方法，找到则说明运行在 1.20.5+ 上，找不到则说明是旧版。
     * 通过运行时方法探测而非硬编码版本号判断，兼容性更强。
     * </p>
     */
    private static void initNewVersionRelated() {
        LogUtil.debug(getLang("nms.module-init-start", "NewVersion(1.20.5+)"));

        try {
            // ---- Tag 基类接口 ----
            // 旧版Spigot映射: NBTBase，新版Mojang映射: Tag
            tagClass = safeGetClass(
                    "net.minecraft.nbt.Tag",
                    getNMSPack() + ".NBTBase",
                    "net.minecraft.nbt.NBTBase"
            );
            logClassStatus("Tag/NBTBase", tagClass);

            // ---- HolderLookup.Provider 类 ----
            // 1.20.5+ 新增的注册表上下文接口，用于物品序列化等操作
            holderLookupProviderClass = safeGetClass(
                    "net.minecraft.core.HolderLookup$Provider"
            );
            logClassStatus("HolderLookup.Provider", holderLookupProviderClass);

            // ---- MinecraftServer.registryAccess() 方法 ----
            // 返回 RegistryAccess（实现了 HolderLookup.Provider），用于获取注册表上下文
            if (minecraftServer != null) {
                registryAccess = safeGetMethod(
                        minecraftServer,
                        new String[]{"registryAccess", "bc", "aA", "aB"}
                );
                logMethodStatus("MinecraftServer.registryAccess", registryAccess);
            }

            // ---- 新版 ItemStack.save(HolderLookup.Provider) → Tag ----
            // 替代旧版 save(NBTTagCompound)，将物品序列化为包含完整数据的 Tag
            if (nmsItemStackClass != null && holderLookupProviderClass != null) {
                save_ItemStack_v2 = safeGetMethod(
                        nmsItemStackClass,
                        new String[]{"save", "a"},
                        holderLookupProviderClass
                );
                logMethodStatus("ItemStack.save(Provider) [1.20.5+]", save_ItemStack_v2);
            }

            // ---- 新版 ItemStack.parse(HolderLookup.Provider, Tag) → Optional<ItemStack> ----
            // 这是静态方法，替代旧版的实例方法 load(NBTTagCompound)
            if (nmsItemStackClass != null && holderLookupProviderClass != null && tagClass != null) {
                parse_ItemStack_static = safeGetMethod(
                        nmsItemStackClass,
                        new String[]{"parse", "a"},
                        holderLookupProviderClass, tagClass
                );
                logMethodStatus("ItemStack.parse(Provider, Tag) [1.20.5+ 静态]", parse_ItemStack_static);
            }

            // 输出新版本适配状态总结
            if (save_ItemStack_v2 != null && parse_ItemStack_static != null) {
                LogUtil.info("[NMS] 1.20.5+ 新版物品API已就绪 (DataComponents)");
            } else if (save_ItemStack != null) {
                LogUtil.info("[NMS] 使用旧版物品API (NBTTagCompound)");
            } else {
                LogUtil.warning("[NMS] 警告：新旧版物品API均未找到，物品NBT功能不可用！");
            }

            LogUtil.debug(getLang("nms.module-init-complete", "NewVersion(1.20.5+)"));
        } catch (Exception e) {
            LogUtil.error(getLang("nms.module-init-fail", "NewVersion(1.20.5+)", e.getMessage()));
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
            LogUtil.debug(getLang("nms.methods-accessible", count));
        } catch (Exception e) {
            LogUtil.debug(getLang("nms.methods-accessible-fail", e.getMessage()));
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

    /**
     * 记录类加载状态
     */
    private static void logClassStatus(String name, Class<?> clazz) {
        if (clazz != null) {
            LogUtil.debug(getLang("nms.class-found", name, clazz.getName()));
        } else {
            LogUtil.warning(getLang("nms.class-not-found", name));
        }
    }

    /**
     * 记录方法查找状态
     */
    private static void logMethodStatus(String name, Method method) {
        if (method != null) {
            LogUtil.debug(getLang("nms.method-found", name));
        } else {
            LogUtil.warning(getLang("nms.method-not-found", name));
        }
    }

    /**
     * 记录字段查找状态
     */
    private static void logFieldStatus(String name, Field field) {
        if (field != null) {
            LogUtil.debug(getLang("nms.field-found", name));
        } else {
            LogUtil.warning(getLang("nms.field-not-found", name));
        }
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
        if (isVersionAtLeast(17)) {
            return "net.minecraft.server";
        } else {
            return "net.minecraft.server." + version;
        }
    }

    /**
     * 获取 OBC（org.bukkit.craftbukkit）包路径
     * <p>
     * Spigot / 旧版 Paper: org.bukkit.craftbukkit.v1_xx_Rx<br>
     * Paper 1.20.5+: org.bukkit.craftbukkit（无版本后缀）
     * </p>
     *
     * @return OBC 包的完整路径
     */
    public static String getOBCPack() {
        if (obcVersioned) {
            return "org.bukkit.craftbukkit." + version;
        } else {
            return "org.bukkit.craftbukkit";
        }
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
     * 判断是否为 1.13+ 版本（扁平化更新）
     *
     * @return 是则返回 true
     */
    public static boolean isHighVersion() {
        return isVersionAtLeast(13);
    }

    /**
     * 判断是否为 1.17+ 版本（Mojang 映射）
     *
     * @return 是则返回 true
     */
    public static boolean isMojangMapped() {
        return isVersionAtLeast(17);
    }

    /**
     * 判断是否使用 1.20.5+ 新版物品API（DataComponents系统）
     * <p>
     * 通过运行时检测新版 save 方法是否存在来判断，
     * 比硬编码版本号更可靠（能正确处理 1.20.5/1.20.6 等中间版本）。
     * </p>
     *
     * @return 是则返回 true
     */
    public static boolean isNewItemAPI() {
        return save_ItemStack_v2 != null;
    }

    /**
     * 获取注册表查找上下文（1.20.5+）
     * <p>
     * 通过 MinecraftServer.registryAccess() 获取 HolderLookup.Provider，
     * 用于物品的序列化/反序列化操作。
     * </p>
     *
     * @return HolderLookup.Provider 实例，获取失败返回 null
     */
    public static Object getRegistryLookup() {
        try {
            if (registryAccess == null) {
                return null;
            }

            Object server = null;

            // 方案1：使用 MinecraftServer.getServer() 静态方法
            if (getServer != null) {
                server = getServer.invoke(null);
            }

            // 方案2：通过 CraftServer.getServer() 获取
            if (server == null) {
                Object craftServer = Bukkit.getServer();
                Method getServerMethod = craftServer.getClass().getMethod("getServer");
                getServerMethod.setAccessible(true);
                server = getServerMethod.invoke(craftServer);
            }

            if (server != null) {
                return registryAccess.invoke(server);
            }
        } catch (Exception e) {
            LogUtil.error("[NMS] 获取注册表上下文失败", e);
        }
        return null;
    }

    /**
     * 获取主版本号（如 1.12 返回 12，1.20 返回 20）
     *
     * @return 主版本号，解析失败返回 0
     */
    public static int getMajorVersion() {
        return majorVersion;
    }

    /**
     * 判断当前版本是否大于等于指定版本
     *
     * @param major 主版本号（如 12 表示 1.12）
     * @return 是则返回 true
     */
    public static boolean isVersionAtLeast(int major) {
        return majorVersion >= major;
    }

    /**
     * 判断当前版本是否在指定范围内（包含边界）
     *
     * @param minMajor 最小主版本号（如 8 表示 1.8）
     * @param maxMajor 最大主版本号（如 12 表示 1.12）
     * @return 是则返回 true
     */
    public static boolean isVersionBetween(int minMajor, int maxMajor) {
        return majorVersion >= minMajor && majorVersion <= maxMajor;
    }
}