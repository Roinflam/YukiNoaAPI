package pers.tany.yukinoaapi;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;

/**
 * NMS相关操作
 */
public class NMSUtil {
    private static final String version;

    public static Field playerConnection;
    public static Class<?> packetPlayOutChat;
    public static Method send;
    public static Method getHandle_Player;
    public static Class<?> iChatBaseComponent;
    public static Class<?> chatComponentText;
    public static Class<?> chatMessageType;

    public static Class<?> nbttagCompoundClass;
    public static Class<?> craftItemStack;
    public static Method asNMSCopy;
    public static Method save_ItemStack;
    public static Class<?> nmsItemStackClass;

    public static Class<?> minecraftServer;
    public static Method getServer;

    public static Class<?> craftEntityClass;
    public static Class<?> nmsEntityClass;
    public static Method getHandle_Entity;
    public static Method getSaveID;
    public static Method getName_Entity;
    public static Method getDisplayName_Entity;

    public static Class<?> mojangsonParser;
    public static Class<?> nmsItemClass;
    public static Method parse;
    public static Method setTag;
    public static Method load_ItemStack;
    public static Method asBukkitCopy;

    public static Method save_Entity;
    public static Method setNbttagCompound;
    public static Class<?> craftServerClass;
    public static Method getEntity;

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

    public static Class<?> iChatBaseComponentClass;
    public static Method getString_IChatBaseComponent;
    public static Method getName_ItemStack;
    public static Method getItem;
    public static Method getName_Item;

    public static Class<?> craftEnchantmentClass;
    public static Class<?> nmsEnchantmentClass;
    public static Method getHandle_Enchantment;
    public static Method getName_Enchantment;
    public static Field target;

    static {
        if (Bukkit.getServer().getClass().getPackage().getName().replace(".", ",").split(",").length > 3) {
            version = Bukkit.getServer().getClass().getPackage().getName().replace(".", ",").split(",")[3];
        } else {
            version = "v1_21_R1";
        }

        System.out.println(version);
        try {
            try {
                playerConnection = Class.forName(getNMSPack() + ".EntityPlayer").getField("playerConnection");
                packetPlayOutChat = Class.forName(getNMSPack() + ".PacketPlayOutChat");
                send = Class.forName(getNMSPack() + ".PlayerConnection").getDeclaredMethod("sendPacket", Class.forName(getNMSPack() + ".Packet"));
                getHandle_Player = Class.forName(getOBCPack() + ".entity.CraftPlayer").getDeclaredMethod("getHandle");
                iChatBaseComponent = Class.forName(getNMSPack() + ".IChatBaseComponent");
                chatComponentText = Class.forName(getNMSPack() + ".ChatComponentText");
                chatMessageType = Class.forName(getNMSPack() + ".ChatMessageType");
            } catch (Exception ignored) {

            }

            try {
                System.out.println(getOBCPack() + ".inventory.CraftItemStack");
                System.out.println(Class.forName(getOBCPack() + ".inventory.CraftItemStack").getPackage().getClass());
                craftItemStack = Class.forName(getOBCPack() + ".inventory.CraftItemStack");
                nbttagCompoundClass = Class.forName(getNMSPack() + ".NBTTagCompound");
                nmsItemStackClass = Class.forName(getNMSPack() + ".ItemStack");
            } catch (ClassNotFoundException | SecurityException e) {
                try {
                    nbttagCompoundClass = Class.forName("net.minecraft.nbt.NBTTagCompound");
                    nmsItemStackClass = Class.forName("net.minecraft.world.item.ItemStack");
                } catch (ClassNotFoundException ex) {
                    nbttagCompoundClass = Class.forName("net.minecraft.nbt.NBTTagCompound");
                    nmsItemStackClass = Class.forName("net.minecraft.world.item.ItemStack");
                }
            }
            asNMSCopy = craftItemStack.getDeclaredMethod("asNMSCopy", ItemStack.class);
            try {
                save_ItemStack = nmsItemStackClass.getDeclaredMethod("save", nbttagCompoundClass);
            } catch (NoSuchMethodException | SecurityException e) {
                save_ItemStack = nmsItemStackClass.getDeclaredMethod("b", nbttagCompoundClass);
            }

            try {
                minecraftServer = Class.forName(getNMSPack() + ".MinecraftServer");
            } catch (ClassNotFoundException | SecurityException e) {
                minecraftServer = Class.forName("net.minecraft.server.MinecraftServer");
            }
            getServer = minecraftServer.getDeclaredMethod("getServer");

            craftEntityClass = Class.forName(getOBCPack() + ".entity.CraftEntity");
            getHandle_Entity = craftEntityClass.getDeclaredMethod("getHandle");

            try {
                nmsEntityClass = Class.forName(getNMSPack() + ".Entity");
            } catch (ClassNotFoundException e) {
                nmsEntityClass = Class.forName("net.minecraft.world.entity.Entity");
            }
            try {
                save_Entity = nmsEntityClass.getDeclaredMethod("save", nbttagCompoundClass);
            } catch (Exception e) {
                save_Entity = nmsEntityClass.getDeclaredMethod("e", nbttagCompoundClass);
            }
            try {
                getName_Entity = nmsEntityClass.getDeclaredMethod("getName");
            } catch (NoSuchMethodException e) {
                getName_Entity = nmsEntityClass.getDeclaredMethod("cq");
            }
            try {
                getDisplayName_Entity = nmsEntityClass.getDeclaredMethod("getDisplayName");
            } catch (Exception ignored) {
                try {
                    getDisplayName_Entity = nmsEntityClass.getDeclaredMethod("C_");
                } catch (Exception e) {

                }
            }
            try {
                getSaveID = nmsEntityClass.getDeclaredMethod("getSaveID");
            } catch (Exception ignored) {
                try {
                    getSaveID = nmsEntityClass.getDeclaredMethod("bn");
                } catch (Exception e) {

                }
            }

            try {
                mojangsonParser = Class.forName(getNMSPack() + ".MojangsonParser");
                nmsItemClass = Class.forName(getNMSPack() + ".Item");
            } catch (ClassNotFoundException e) {
                mojangsonParser = Class.forName("net.minecraft.nbt.MojangsonParser");
                nmsItemClass = Class.forName("net.minecraft.world.item.Item");
                System.out.println(mojangsonParser.getName());
                System.out.println(nmsItemClass.getName());
            }
            try {
                parse = mojangsonParser.getDeclaredMethod("parse", String.class);
            } catch (NoSuchMethodException e) {
                parse = mojangsonParser.getDeclaredMethod("a", String.class);
            }
            try {
                setTag = nmsItemStackClass.getDeclaredMethod("setTag", nbttagCompoundClass);
            } catch (NoSuchMethodException e) {
                setTag = nmsItemStackClass.getDeclaredMethod("c", nbttagCompoundClass);
            }
            try {
                load_ItemStack = nmsItemStackClass.getDeclaredMethod("load", nbttagCompoundClass);
            } catch (Exception e) {
                try {
                    load_ItemStack = nmsItemStackClass.getDeclaredMethod("c", nbttagCompoundClass);
                } catch (Exception e1) {
                    load_ItemStack = nmsItemStackClass.getDeclaredMethod("a", nbttagCompoundClass);
                }
            }

            asBukkitCopy = craftItemStack.getDeclaredMethod("asBukkitCopy", nmsItemStackClass);

            try {
                setNbttagCompound = nmsEntityClass.getDeclaredMethod("f", nbttagCompoundClass);
            } catch (Exception e) {
                try {
                    setNbttagCompound = nmsEntityClass.getDeclaredMethod("e", nbttagCompoundClass);
                } catch (Exception e1) {
                    setNbttagCompound = nmsEntityClass.getDeclaredMethod("load", nbttagCompoundClass);
                }
            }
            craftServerClass = Class.forName(getOBCPack() + ".CraftServer");
            getEntity = craftEntityClass.getDeclaredMethod("getEntity", craftServerClass, nmsEntityClass);

            craftWorldClass = Class.forName(getOBCPack() + ".CraftWorld");
            getHandle_Word = craftWorldClass.getDeclaredMethod("getHandle");
            try {
                nmsWorldClass = Class.forName(getNMSPack() + ".World");
                tileEntityClass = Class.forName(getNMSPack() + ".TileEntity");
            } catch (ClassNotFoundException e) {
                nmsWorldClass = Class.forName("net.minecraft.world.level.World");
                tileEntityClass = Class.forName("net.minecraft.world.level.block.entity.TileEntity");
            }
            try {
                try {
                    blockPositionClass = Class.forName(getNMSPack() + ".BlockPosition");
                } catch (ClassNotFoundException e) {
                    blockPositionClass = Class.forName("net.minecraft.core.BlockPosition");
                }
                getTileEntity = nmsWorldClass.getDeclaredMethod("getTileEntity", blockPositionClass);
                setTileEntity = nmsWorldClass.getDeclaredMethod("setTileEntity", blockPositionClass, tileEntityClass);
            } catch (Exception e) {
                try {
                    getTileEntity = nmsWorldClass.getDeclaredMethod("getTileEntity", int.class, int.class, int.class);
                    setTileEntity = nmsWorldClass.getDeclaredMethod("setTileEntity", int.class, int.class, int.class, tileEntityClass);
                } catch (NoSuchMethodException ex) {
                    getTileEntity = nmsWorldClass.getDeclaredMethod("c_", blockPositionClass);
                    setTileEntity = nmsWorldClass.getDeclaredMethod("a", tileEntityClass);
                }
            }

            try {
                save_Block = tileEntityClass.getDeclaredMethod("save", nbttagCompoundClass);
            } catch (Exception e) {
                save_Block = tileEntityClass.getDeclaredMethod("b", nbttagCompoundClass);
            }
            try {
                load_Block = tileEntityClass.getDeclaredMethod("load", nbttagCompoundClass);
            } catch (Exception e) {
                try {
                    load_Block = tileEntityClass.getDeclaredMethod("a", nbttagCompoundClass);
                } catch (Exception e1) {
                    load_Block = tileEntityClass.getDeclaredMethod("load", Class.forName(getNMSPack() + ".IBlockData"), nbttagCompoundClass);
                }
            }
            craftBlockClass = Class.forName(getOBCPack() + ".block.CraftBlock");
            setData = craftBlockClass.getDeclaredMethod("setData", byte.class);

            try {
                getName_ItemStack = nmsItemStackClass.getDeclaredMethod("getName");
            } catch (Exception e1) {
                try {
                    getName_ItemStack = nmsItemStackClass.getDeclaredMethod("y");  // 尝试新的 y() 方法
                } catch (Exception e2) {
                    try {
                        getName_ItemStack = nmsItemStackClass.getDeclaredMethod("q");
                    } catch (Exception e3) {
                        try {
                            getName_ItemStack = nmsItemStackClass.getDeclaredMethod("p");
                        } catch (Exception e4) {
                            try {
                                getName_ItemStack = nmsItemStackClass.getDeclaredMethod("m");
                            } catch (Exception e5) {
                                throw e5;
                            }
                        }
                    }
                }
            }

            try {
                getItem = nmsItemStackClass.getDeclaredMethod("c");
            } catch (NoSuchMethodException | SecurityException e) {
                try {
                    getItem = nmsItemStackClass.getDeclaredMethod("getItem");
                } catch (NoSuchMethodException | SecurityException e1) {
                    getItem = nmsItemStackClass.getDeclaredMethod("o");
                }
            }
            try {
                iChatBaseComponentClass = Class.forName(getNMSPack() + ".IChatBaseComponent");
            } catch (Exception e) {
                iChatBaseComponentClass = Class.forName("net.minecraft.network.chat.IChatBaseComponent");
            }
            try {
                getString_IChatBaseComponent = iChatBaseComponentClass.getDeclaredMethod("getString");
            } catch (NoSuchMethodException | SecurityException e) {

            }

            try {
                getName_Item = nmsItemClass.getDeclaredMethod("h", nmsItemStackClass);
            } catch (NoSuchMethodException | SecurityException e) {
                try {
                    getName_Item = nmsItemClass.getDeclaredMethod("m", nmsItemStackClass);
                } catch (NoSuchMethodException | SecurityException ignored) {

                }
            }
            craftEnchantmentClass = Class.forName(getOBCPack() + ".enchantments.CraftEnchantment");
            try {
                nmsEnchantmentClass = Class.forName(getNMSPack() + ".Enchantment");
            } catch (ClassNotFoundException e) {
                nmsEnchantmentClass = Class.forName("net.minecraft.world.item.enchantment.Enchantment");
            }
            try {
                getHandle_Enchantment = craftEnchantmentClass.getDeclaredMethod("getHandle");
                getName_Enchantment = nmsEnchantmentClass.getDeclaredMethod("d", int.class);
            } catch (Exception e) {
                target = craftEnchantmentClass.getDeclaredField("target");
                getName_Enchantment = nmsEnchantmentClass.getDeclaredMethod("c", int.class);
            }

        } catch (Exception exception) {
            exception.printStackTrace();
        }
        for (Field field : NMSUtil.class.getFields()) {
            try {
                if (field.get(NMSUtil.class) instanceof Method) {
                    Method method = (Method) field.get(NMSUtil.class);
                    method.setAccessible(true);
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 获取这个版本的信息
     *
     * @return 服务器版本
     */
    public static String getVersion() {
        return version;
    }

    /**
     * 获取这个版本的nms包路径
     *
     * @return nms包路径
     */
    
    public static String getNMSPack() {
        if (version.contains("v1_20") || version.contains("v1_21")) {
            return "net.minecraft.server";
        } else {
            return "net.minecraft.server." + version;
        }
    }

    /**
     * 获取这个版本的obc包路径
     *
     * @return obc包路径
     */
    
    public static String getOBCPack() {
        return "org.bukkit.craftbukkit." + version;
    }
}
