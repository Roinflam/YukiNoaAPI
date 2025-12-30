package pers.tany.yukinoaapi.interfacepart.block;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import pers.tany.yukinoaapi.util.NMSUtil;

import javax.annotation.Nullable;
import java.lang.reflect.Method;

public interface IBlock {
    /**
     * 检查方块是否为空
     *
     * @param block 涉及方块
     * @return 如果空则为true，否则为false
     */
    static boolean isEmpty(@Nullable Block block) {
        return block == null || block.getType().equals(Material.AIR);
    }

    /**
     * 获取方块的NBT
     *
     * @param block 涉及方块
     * @return 物品的NBT
     */
    @Nullable
    static String getBlockNBT(Block block) {
        try {
            Location location = block.getLocation();
            Class<?> nbttagCompoundClass = NMSUtil.nbttagCompoundClass;
            Class<?> nmsWorldClass = NMSUtil.nmsWorldClass;
            Class<?> tileEntityClass = NMSUtil.tileEntityClass;
            Class<?> blockPositionClass = NMSUtil.blockPositionClass;
            Method getTileEntity = NMSUtil.getTileEntity;
            Method getHandle_Word = NMSUtil.getHandle_Word;
            Method save = NMSUtil.save_Block;

            Object nmsWorld = getHandle_Word.invoke(location.getWorld());
            Object nbttagCompound = nbttagCompoundClass.newInstance();
            @Nullable Object tileEntity = null;
            try {
                Object blockPosition = blockPositionClass.getConstructor(int.class, int.class, int.class).newInstance(location.getBlockX(), location.getBlockY(), location.getBlockZ());
                tileEntity = getTileEntity.invoke(nmsWorld, blockPosition);
            } catch (Exception e) {
                tileEntity = getTileEntity.invoke(nmsWorld, location.getBlockX(), location.getBlockY(), location.getBlockZ());
            }
            save.invoke(tileEntity, nbttagCompound);

            return nbttagCompound.toString();
//            WorldServer nmsWorld = ((CraftWorld) location.getWorld()).getHandle();
//            NBTTagCompound nbttagCompound = new NBTTagCompound();
//            BlockPosition blockPosition = new BlockPosition(location.getBlockX(), location.getBlockY(), location.getBlockZ());
//            TileEntity tileEntity = nmsWorld.getTileEntity(blockPosition);
//            tileEntity.save(nbttagCompound);
//            return nbttagCompound.toString();
        } catch (NullPointerException e) {
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
