package pers.tany.yukinoaapi.interfacepart.serializer;

import com.comphenix.protocol.utility.StreamSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import pers.tany.yukinoaapi.Main;
import pers.tany.yukinoaapi.util.NMSUtil;
import pers.tany.yukinoaapi.interfacepart.block.IBlock;
import pers.tany.yukinoaapi.interfacepart.encrypt.IEncrypt;
import pers.tany.yukinoaapi.interfacepart.entity.IEntity;
import pers.tany.yukinoaapi.interfacepart.item.IItem;
import pers.tany.yukinoaapi.realizationpart.builder.ItemBuilder;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public interface ISerializer {
    /**
     * 序列化物品为字符串
     *
     * @param itemStack 序列化的物品
     * @return 序列化后的结果
     */
    @Nullable
    static String serializerItemStack(ItemStack itemStack) {
        return serializerItemStack(itemStack, false);
    }

    /**
     * 序列化物品为字符串
     *
     * @param itemStack 序列化的物品
     * @param alone     是否把物品堆叠数设置为1个
     * @return 序列化后的结果
     */
    @Nullable
    static String serializerItemStack(ItemStack itemStack, boolean alone) {
        if (Main.config.getBoolean("ProtocolLibItem")) {
            final StreamSerializer serializer = new StreamSerializer();
            ItemStack i = itemStack.clone();
            i.setAmount(alone ? 1 : i.getAmount());
            try {
                return serializer.serializeItemStack(i);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            try {
                ItemStack i = itemStack.clone();
                i.setAmount(alone ? 1 : i.getAmount());
                return IEncrypt.encrypt(i.getType().toString() + "%SerializerItem%" + i.getAmount() + "%SerializerItem%" + i.getDurability() + "%SerializerItem%" + IItem.getItemNBT(i), "serializer");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * 反序列化物品
     *
     * @param data 物品序列化后的结果
     * @return 物品
     */
    @Nullable
    static ItemStack deserializeItemStack(String data) {
        if (Main.config.getBoolean("ProtocolLibItem")) {
            StreamSerializer serializer = new StreamSerializer();
            try {
                ItemStack itemStack = serializer.deserializeItemStack(data);
                if(itemStack == null){
                    String[] s = IEncrypt.decrypt(data, "serializer").split("%SerializerItem%");
                    Class<?> nbttagCompoundClass = NMSUtil.nbttagCompoundClass;
                    Class<?> nmsItemStackClass = NMSUtil.nmsItemStackClass;
                    Class<?> nmsItemClass = NMSUtil.nmsItemClass;
                    Method asNMSCopy = NMSUtil.asNMSCopy;
                    Method parse = NMSUtil.parse;
                    Method load = NMSUtil.load_ItemStack;
                    Method asBukkitCopy = NMSUtil.asBukkitCopy;

                    Object nbttagCompound = parse.invoke(null, s[3]);
                    Object nmsItem = asNMSCopy.invoke(null, new ItemBuilder(Material.valueOf(s[0]), Integer.parseInt(s[1]), Short.parseShort(s[2])).getItemStack());
                    load.invoke(nmsItem, nbttagCompound);
                    return (ItemStack) asBukkitCopy.invoke(null, nmsItem);
                }
                return itemStack;
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            try {
                String[] s = IEncrypt.decrypt(data, "serializer").split("%SerializerItem%");
                Class<?> nbttagCompoundClass = NMSUtil.nbttagCompoundClass;
                Class<?> nmsItemStackClass = NMSUtil.nmsItemStackClass;
                Class<?> nmsItemClass = NMSUtil.nmsItemClass;
                Method asNMSCopy = NMSUtil.asNMSCopy;
                Method parse = NMSUtil.parse;
                Method load = NMSUtil.load_ItemStack;
                Method asBukkitCopy = NMSUtil.asBukkitCopy;

                Object nbttagCompound = parse.invoke(null, s[3]);
                Object nmsItem = asNMSCopy.invoke(null, new ItemBuilder(Material.valueOf(s[0]), Integer.parseInt(s[1]), Short.parseShort(s[2])).getItemStack());
                load.invoke(nmsItem, nbttagCompound);
                ItemStack itemStack = (ItemStack) asBukkitCopy.invoke(null, nmsItem);
                if(itemStack == null){
                    StreamSerializer serializer = new StreamSerializer();
                    return serializer.deserializeItemStack(data);
                }
                return itemStack;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * 序列化这个物品栏所有物品
     *
     * @param inventory 涉及的物品栏
     * @return 序列化后的结果
     */
    
    static String serializerInventory(Inventory inventory) {
        String str = inventory.getSize() + "%SerializerSize%";
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack itemStack = inventory.getItem(i);
            if (!IItem.isEmpty(itemStack)) {
                str += i + ":" + serializerItemStack(itemStack, false) + "%SerializerInventory%";
            }
        }
        return str;
    }

    /**
     * 反序列化这个物品栏的物品
     *
     * @param data 物品栏序列化后的结果
     * @return 物品栏
     */
    static Inventory deserializerInventory(String data) {
        Inventory inventory = Bukkit.createInventory(null, Integer.parseInt(data.split("%SerializerSize%")[0]));
        for (String str : data.split("%SerializerSize%")[1].split("%SerializerInventory%")) {
            String[] s = str.split(":");
            int index = Integer.parseInt(s[0]);
            @Nullable ItemStack itemStack = deserializeItemStack(s[1]);
            inventory.setItem(index, itemStack);
        }
        return inventory;
    }

    /**
     * 反序列化这个物品栏的物品
     *
     * @param inventory 需要设置物品的物品栏
     * @param data      物品栏序列化后的结果
     */
    static void deserializerInventory(Inventory inventory, String data) {
        inventory.clear();
        if (data.contains("%SerializerInventory%")) {
            for (String str : data.split("%SerializerSize%")[1].split("%SerializerInventory%")) {
                String[] s = str.split(":");
                int index = Integer.parseInt(s[0]);
                @Nullable ItemStack itemStack = deserializeItemStack(s[1]);
                inventory.setItem(index, itemStack);
            }
        }
    }

    /**
     * 序列化这个实体
     *
     * @param entity 涉及的实体
     * @return 序列化后的结果
     */
    
    static String serializerEntity(Entity entity) {
        return IEncrypt.encrypt(entity.getType().toString() + "%SerializerEntity%" + IEntity.getEntityNBT(entity), "serializer");
    }

    /**
     * 反序列化这个实体
     *
     * @param location 生成实体的坐标
     * @param data     物品栏序列化后的结果
     * @return 生成后的实体
     */
    @Nullable
    static LivingEntity deserializerEntity(Location location, String data) {
        try {
            Class<?> nmsEntityClass = NMSUtil.nmsEntityClass;
            Class<?> nbttagCompoundClass = NMSUtil.nbttagCompoundClass;
            Method getHandle = NMSUtil.getHandle_Entity;
            Method parse = NMSUtil.parse;
            Method setNbttagCompound = NMSUtil.setNbttagCompound;

            String[] str = IEncrypt.decrypt(data, "serializer").split("%SerializerEntity%");
            LivingEntity livingEntity = (LivingEntity) location.getWorld().spawnEntity(location, EntityType.valueOf(str[0]));

            Object nmsEntity = getHandle.invoke(livingEntity);

            Object nbttagCompound = parse.invoke(null, str[1]);
            setNbttagCompound.invoke(nmsEntity, nbttagCompound);

            livingEntity.teleport(location);
            return livingEntity;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 序列化这个方块
     *
     * @param block 涉及的方块
     * @return 序列化后的结果
     */
    
    static String serializerBlock(Block block) {
        @Nullable String nbt = IBlock.getBlockNBT(block);
        if (nbt != null) {
            return IEncrypt.encrypt(block.getType().toString() + "%SerializerBlock%" + block.getData() + "%SerializerBlock%" + nbt, "serializer");
        } else {
            return IEncrypt.encrypt(block.getType().toString() + "%SerializerBlock%" + block.getData(), "serializer");
        }
    }

    /**
     * 反序列化这个方块
     *
     * @param location 生成方块的坐标
     * @param data     物品栏序列化后的结果
     * @return 生成后的实体
     */
    @Nullable
    static Block deserializerBlock(Location location, String data) {
        try {
            Method setData = NMSUtil.setData;
            String[] str = IEncrypt.decrypt(data, "serializer").split("%SerializerBlock%");
            Block block = location.getBlock();

            block.setType(Material.valueOf(str[0]));
            setData.invoke(block, Byte.parseByte(str[1]));
            if (str.length > 2) {
                Class<?> nmsWorldClass = NMSUtil.nmsWorldClass;
                Class<?> tileEntityClass = NMSUtil.tileEntityClass;
                Class<?> blockPositionClass = NMSUtil.blockPositionClass;
                Method parse = NMSUtil.parse;
                Method getTileEntity = NMSUtil.getTileEntity;
                Method getHandle_Word = NMSUtil.getHandle_Word;
                Method load = NMSUtil.load_Block;
                Method setTileEntity = NMSUtil.setTileEntity;

                Object nmsWorld = getHandle_Word.invoke(location.getWorld());
                Object nbttagCompound = parse.invoke(null, str[2]);
                @Nullable Object blockPosition = null;
                @Nullable Object tileEntity = null;

                try {
                    blockPosition = blockPositionClass.getConstructor(int.class, int.class, int.class).newInstance(location.getBlockX(), location.getBlockY(), location.getBlockZ());
                    tileEntity = getTileEntity.invoke(nmsWorld, blockPosition);
                } catch (Exception e) {
                    tileEntity = getTileEntity.invoke(nmsWorld, location.getBlockX(), location.getBlockY(), location.getBlockZ());
                }

                try {
                    load.invoke(tileEntity, nbttagCompound);
                } catch (Exception e) {
                    load.invoke(tileEntity, null, nbttagCompound);
                }
                try {
                    setTileEntity.invoke(nmsWorld, blockPosition, tileEntity);
                } catch (Exception e) {
                    try {
                        setTileEntity.invoke(nmsWorld, location.getBlockX(), location.getBlockY(), location.getBlockZ(), tileEntity);
                    } catch (IllegalAccessException | InvocationTargetException | IllegalArgumentException ex) {
                        setTileEntity.invoke(nmsWorld, tileEntity);
                    }
                }
//                NBTTagCompound nbttagCompound = (NBTTagCompound) MojangsonParser.parse(str[2]);
//                World world = ((CraftWorld) location.getWorld()).getHandle();
//                TileEntity tileEntity = world.getTileEntity(location.getBlockX(), location.getBlockY(), location.getBlockZ());
//                tileEntity.a(nbttagCompound);
//                world.setTileEntity(location.getBlockX(), location.getBlockY(), location.getBlockZ(), tileEntity);
            }
            return block;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 序列化这个坐标
     *
     * @param location 涉及的坐标
     * @return 序列化后的结果
     */
    
    static String serializerLocation(Location location) {
        return IEncrypt.encrypt(location.getWorld().getName() + ":" + location.getX() + ":" + location.getY() + ":" + location.getZ() + ":" + location.getYaw() + ":" + location.getPitch(), "serializer");
    }

    /**
     * 反序列化这个坐标
     *
     * @param data 坐标序列化后的结果
     * @return 坐标
     */
    
    static Location deserializerLocation(String data) {
        String[] strs = IEncrypt.decrypt(data, "serializer").split(":");
        return new Location(Bukkit.getWorld(strs[0]), Double.parseDouble(strs[1]), Double.parseDouble(strs[2]), Double.parseDouble(strs[3]), Float.parseFloat(strs[4]), Float.parseFloat(strs[5]));
    }
}
