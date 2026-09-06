package pers.tany.yukinoaapi.interfacepart.block;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import pers.tany.yukinoaapi.util.LogUtil;
import pers.tany.yukinoaapi.util.NMSUtil;

import javax.annotation.Nullable;
import java.lang.reflect.Method;

/**
 * 方块操作相关接口
 * 提供方块检测、NBT 读写功能
 *
 * <p>注意：方块实体 NBT 操作 Bukkit 没有提供 API，所有版本均使用 NMS 反射</p>
 *
 * @author tany
 * @since 1.0.0
 */
public final class IBlock {

    /**
     * 工具类,禁止实例化
     *
     * @throws UnsupportedOperationException 总是抛出
     */
    private IBlock() {
        throw new UnsupportedOperationException("IBlock 是工具类,不允许实例化");
    }


    /**
     * 检查方块是否为空
     *
     * @param block 目标方块，可为 null
     * @return 如果方块为 null 或为空气则返回 true
     */
    public static boolean isEmpty(@Nullable Block block) {
        return block == null || block.getType() == Material.AIR;
    }

    /**
     * 获取方块实体的 NBT 数据
     * 仅对有方块实体的方块有效（箱子、告示牌、熔炉等）
     *
     * @param block 目标方块
     * @return NBT 字符串，如果方块没有方块实体或获取失败返回 null
     */
    @Nullable
    public static String getBlockNBT(Block block) {
        if (block == null) {
            return null;
        }

        try {
            Location location = block.getLocation();

            Class<?> nbttagCompoundClass = NMSUtil.nbttagCompoundClass;
            Method getHandle = NMSUtil.getHandle_Word;
            Method getTileEntity = NMSUtil.getTileEntity;
            Method save = NMSUtil.save_Block;
            Class<?> blockPositionClass = NMSUtil.blockPositionClass;

            // 获取 NMS 世界
            Object nmsWorld = getHandle.invoke(location.getWorld());

            // 创建空的 NBT 复合标签
            Object nbttagCompound = nbttagCompoundClass.getDeclaredConstructor().newInstance();

            // 获取方块实体
            Object tileEntity = getTileEntityAt(nmsWorld, location, getTileEntity, blockPositionClass);

            if (tileEntity == null) {
                return null;
            }

            // 保存 NBT
            save.invoke(tileEntity, nbttagCompound);

            return nbttagCompound.toString();

        } catch (NullPointerException e) {
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 设置方块实体的 NBT 数据
     * 仅对有方块实体的方块有效
     *
     * @param block     目标方块
     * @param nbtString NBT 字符串
     * @return 设置成功返回 true
     */
    public static boolean setBlockNBT(Block block, String nbtString) {
        if (block == null || nbtString == null || nbtString.isEmpty()) {
            return false;
        }

        try {
            Location location = block.getLocation();

            Method getHandle = NMSUtil.getHandle_Word;
            Method getTileEntity = NMSUtil.getTileEntity;
            Method load = NMSUtil.load_Block;
            Method parse = NMSUtil.parse;
            Class<?> blockPositionClass = NMSUtil.blockPositionClass;

            // 解析 NBT 字符串
            Object nbttagCompound = parse.invoke(null, nbtString);

            // 获取 NMS 世界
            Object nmsWorld = getHandle.invoke(location.getWorld());

            // 获取方块实体
            Object tileEntity = getTileEntityAt(nmsWorld, location, getTileEntity, blockPositionClass);

            if (tileEntity == null) {
                return false;
            }

            // 加载 NBT 数据
            load.invoke(tileEntity, nbttagCompound);

            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 内部方法：获取指定位置的方块实体
     *
     * @param nmsWorld           NMS 世界对象
     * @param location           位置
     * @param getTileEntity      getTileEntity 方法
     * @param blockPositionClass BlockPosition 类
     * @return 方块实体对象，不存在返回 null
     */
    @Nullable
    public static Object getTileEntityAt(Object nmsWorld, Location location,
                                  Method getTileEntity, Class<?> blockPositionClass) {
        // 优先使用 BlockPosition（1.8+）
        if (blockPositionClass != null) {
            try {
                Object blockPosition = blockPositionClass
                        .getConstructor(int.class, int.class, int.class)
                        .newInstance(location.getBlockX(), location.getBlockY(), location.getBlockZ());
                return getTileEntity.invoke(nmsWorld, blockPosition);
            } catch (Exception ignored) {
                // 继续尝试 xyz 方式
            }
        }

        // 1.7 使用 xyz 参数
        try {
            return getTileEntity.invoke(nmsWorld,
                    location.getBlockX(),
                    location.getBlockY(),
                    location.getBlockZ());
        } catch (Exception e) {
            return null;
        }
    }
}

