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
import pers.tany.yukinoaapi.util.LogUtil;

import javax.annotation.Nullable;
import java.lang.reflect.Method;

/**
 * 序列化/反序列化工具接口
 * 支持物品、物品栏、实体、方块、坐标的序列化
 *
 * <p>版本兼容性：</p>
 * <ul>
 *     <li>1.7.x - 1.12.x：方块使用 data 值</li>
 *     <li>1.13+：方块使用 BlockData</li>
 * </ul>
 *
 * @author tany
 * @since 1.0.0
 */
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
                if (itemStack == null) {
                    String[] s = IEncrypt.decrypt(data, "serializer").split("%SerializerItem%");
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
                Method asNMSCopy = NMSUtil.asNMSCopy;
                Method parse = NMSUtil.parse;
                Method load = NMSUtil.load_ItemStack;
                Method asBukkitCopy = NMSUtil.asBukkitCopy;

                Object nbttagCompound = parse.invoke(null, s[3]);
                Object nmsItem = asNMSCopy.invoke(null, new ItemBuilder(Material.valueOf(s[0]), Integer.parseInt(s[1]), Short.parseShort(s[2])).getItemStack());
                load.invoke(nmsItem, nbttagCompound);
                ItemStack itemStack = (ItemStack) asBukkitCopy.invoke(null, nmsItem);
                if (itemStack == null) {
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
        StringBuilder str = new StringBuilder(inventory.getSize() + "%SerializerSize%");
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack itemStack = inventory.getItem(i);
            if (!IItem.isEmpty(itemStack)) {
                str.append(i).append(":").append(serializerItemStack(itemStack, false)).append("%SerializerInventory%");
            }
        }
        return str.toString();
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
     * @param data     实体序列化后的结果
     * @return 生成后的实体
     */
    @Nullable
    static LivingEntity deserializerEntity(Location location, String data) {
        try {
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
     * 1.13+ 使用 BlockData，1.12 及以下使用 data 值
     *
     * @param block 涉及的方块
     * @return 序列化后的结果
     */
    static String serializerBlock(Block block) {
        @Nullable String nbt = IBlock.getBlockNBT(block);
        String blockData;

        // 1.13+ 使用 BlockData，1.12 及以下使用 data 值
        if (NMSUtil.isVersionAtLeast(13)) {
            blockData = block.getBlockData().getAsString();
        } else {
            blockData = String.valueOf(block.getData());
        }

        if (nbt != null) {
            return IEncrypt.encrypt(block.getType().toString() + "%SerializerBlock%" + blockData + "%SerializerBlock%" + nbt, "serializer");
        } else {
            return IEncrypt.encrypt(block.getType().toString() + "%SerializerBlock%" + blockData, "serializer");
        }
    }

    /**
     * 反序列化这个方块
     * 1.13+ 使用 BlockData，1.12 及以下使用 data 值
     *
     * @param location 生成方块的坐标
     * @param data     方块序列化后的结果
     * @return 生成后的方块
     */
    @Nullable
    static Block deserializerBlock(Location location, String data) {
        try {
            String[] str = IEncrypt.decrypt(data, "serializer").split("%SerializerBlock%");
            Block block = location.getBlock();

            // 1. 设置方块类型
            block.setType(Material.valueOf(str[0]));

            // 2. 设置方块状态
            if (NMSUtil.isVersionAtLeast(13)) {
                // 1.13+ 使用 BlockData
                try {
                    block.setBlockData(Bukkit.createBlockData(str[1]));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                // 1.12 及以下使用 data 值
                try {
                    Method setData = NMSUtil.setData;
                    if (setData != null) {
                        setData.invoke(block, Byte.parseByte(str[1]));
                    }
                } catch (Exception e) {
                    // data 设置失败，忽略
                }
            }

            // 3. 设置方块实体 NBT（如果有）
            if (str.length > 2) {
                loadBlockNBT(location, str[2]);
            }

            return block;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 加载方块实体的 NBT 数据
     *
     * @param location  方块位置
     * @param nbtString NBT 字符串
     */
    static void loadBlockNBT(Location location, String nbtString) {
        try {
            Class<?> blockPositionClass = NMSUtil.blockPositionClass;
            Method parse = NMSUtil.parse;
            Method getTileEntity = NMSUtil.getTileEntity;
            Method getHandle = NMSUtil.getHandle_Word;
            Method load = NMSUtil.load_Block;

            // 检查必要组件
            if (parse == null || getTileEntity == null || getHandle == null || load == null) {
                return;
            }

            // 获取 NMS 世界
            Object nmsWorld = getHandle.invoke(location.getWorld());

            // 解析 NBT 字符串
            Object nbttagCompound = parse.invoke(null, nbtString);

            // 获取方块实体
            Object tileEntity = null;

            // 优先使用 BlockPosition（1.8+）
            if (blockPositionClass != null) {
                try {
                    Object blockPosition = blockPositionClass
                            .getConstructor(int.class, int.class, int.class)
                            .newInstance(location.getBlockX(), location.getBlockY(), location.getBlockZ());
                    tileEntity = getTileEntity.invoke(nmsWorld, blockPosition);
                } catch (Exception ignored) {
                    // 继续尝试 xyz 方式
                }
            }

            // 1.7 备用：使用 xyz 参数
            if (tileEntity == null) {
                try {
                    tileEntity = getTileEntity.invoke(nmsWorld,
                            location.getBlockX(),
                            location.getBlockY(),
                            location.getBlockZ());
                } catch (Exception ignored) {
                }
            }

            if (tileEntity == null) {
                return;
            }

            // 加载 NBT 数据
            try {
                load.invoke(tileEntity, nbttagCompound);
            } catch (Exception e) {
                // 某些版本可能需要额外参数，尝试带 BlockState 参数
                try {
                    load.invoke(tileEntity, null, nbttagCompound);
                } catch (Exception ignored) {
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
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
