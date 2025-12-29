// 文件：ILocation.java
// 路径：src/pers/tany/yukinoaapi/interfacepart/location/ILocation.java
package pers.tany.yukinoaapi.interfacepart.location;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * Location相关操作接口
 * 提供坐标计算、范围判断、实体/方块获取等功能
 */
public interface ILocation {

    /**
     * 获取这两个坐标的距离(同世界)
     *
     * @param one 其中一个坐标
     * @param two 另一个坐标
     * @return 距离
     */
    static double getFar(Location one, Location two) {
        return getFar(one, two, true);
    }

    /**
     * 获取这两个坐标的距离(同世界)
     *
     * @param one    其中一个坐标
     * @param two    另一个坐标
     * @param height 是否计算高度
     * @return 距离
     */
    static double getFar(Location one, Location two, boolean height) {
        if (!one.getWorld().equals(two.getWorld())) {
            throw new IllegalArgumentException("必须为同一个世界");
        }
        double dx = Math.abs(one.getX() - two.getX());
        double dy = height ? Math.abs(one.getY() - two.getY()) : 0;
        double dz = Math.abs(one.getZ() - two.getZ());
        return dx + dy + dz;
    }

    /**
     * 获取这两个坐标的格子距离(同世界)
     *
     * @param one 其中一个坐标
     * @param two 另一个坐标
     * @return 距离
     */
    static int getFarGrid(Location one, Location two) {
        return getFarGrid(one, two, true);
    }

    /**
     * 获取这两个坐标的格子距离(同世界)
     *
     * @param one    其中一个坐标
     * @param two    另一个坐标
     * @param height 是否计算高度
     * @return 距离
     */
    static int getFarGrid(Location one, Location two, boolean height) {
        if (!one.getWorld().equals(two.getWorld())) {
            throw new IllegalArgumentException("必须为同一个世界");
        }
        int dx = Math.abs(one.getBlockX() - two.getBlockX());
        int dy = height ? Math.abs(one.getBlockY() - two.getBlockY()) : 0;
        int dz = Math.abs(one.getBlockZ() - two.getBlockZ());
        return dx + dy + dz;
    }

    /**
     * 判断坐标是否在范围内
     *
     * @param corner1  一点
     * @param corner2  另外一点
     * @param location 坐标
     * @return 在则true，否则为false
     */
    static boolean isWithinRange(Location corner1, Location corner2, Location location) {
        return isWithinRange(corner1, corner2, location, true);
    }

    /**
     * 判断坐标是否在范围内
     *
     * @param corner1  一点
     * @param corner2  另外一点
     * @param location 坐标
     * @param height   是否计算高度（false为无视Y轴）
     * @return 在则true，否则为false
     */
    static boolean isWithinRange(Location corner1, Location corner2, Location location, boolean height) {
        // 世界检查
        if (!corner1.getWorld().equals(corner2.getWorld())) {
            throw new IllegalArgumentException("必须为同一个世界");
        }
        if (!corner1.getWorld().equals(location.getWorld())) {
            return false;
        }

        // 计算边界
        double minX = Math.min(corner1.getX(), corner2.getX());
        double maxX = Math.max(corner1.getX(), corner2.getX());
        double minZ = Math.min(corner1.getZ(), corner2.getZ());
        double maxZ = Math.max(corner1.getZ(), corner2.getZ());

        // X和Z范围检查
        double x = location.getX();
        double z = location.getZ();
        if (x < minX || x > maxX || z < minZ || z > maxZ) {
            return false;
        }

        // Y范围检查（如果需要）
        if (height) {
            double minY = Math.min(corner1.getY(), corner2.getY());
            double maxY = Math.max(corner1.getY(), corner2.getY());
            double y = location.getY();
            return y >= minY && y <= maxY;
        }

        return true;
    }

    /**
     * 获取这个范围内的所有实体
     *
     * @param corner1 一点
     * @param corner2 另外一点
     * @return 这范围区域内的所有实体
     */
    @Nonnull
    static List<Entity> getLocationEntitys(Location corner1, Location corner2) {
        return getLocationEntitys(corner1, corner2, true);
    }

    /**
     * 获取这个范围内的所有实体
     *
     * @param corner1 一点
     * @param corner2 另外一点
     * @param height  是否计算高度（false为无视）
     * @return 这范围区域内的所有实体
     */
    @Nonnull
    static List<Entity> getLocationEntitys(Location corner1, Location corner2, boolean height) {
        if (!corner1.getWorld().equals(corner2.getWorld())) {
            throw new IllegalArgumentException("必须为同一个世界");
        }

        List<Entity> entities = new ArrayList<>();
        for (Entity entity : corner1.getWorld().getEntities()) {
            if (isWithinRange(corner1, corner2, entity.getLocation(), height)) {
                entities.add(entity);
            }
        }
        return entities;
    }

    /**
     * 获取这个范围内的所有生物
     *
     * @param corner1 一点
     * @param corner2 另外一点
     * @return 这范围区域内的所有生物
     */
    @Nonnull
    static List<LivingEntity> getLocationLivingEntitys(Location corner1, Location corner2) {
        return getLocationLivingEntitys(corner1, corner2, true);
    }

    /**
     * 获取这个范围内的所有生物
     *
     * @param corner1 一点
     * @param corner2 另外一点
     * @param height  是否计算高度（false为无视）
     * @return 这范围区域内的所有生物
     */
    @Nonnull
    static List<LivingEntity> getLocationLivingEntitys(Location corner1, Location corner2, boolean height) {
        if (!corner1.getWorld().equals(corner2.getWorld())) {
            throw new IllegalArgumentException("必须为同一个世界");
        }

        List<LivingEntity> livingEntities = new ArrayList<>();
        for (LivingEntity livingEntity : corner1.getWorld().getLivingEntities()) {
            if (isWithinRange(corner1, corner2, livingEntity.getLocation(), height)) {
                livingEntities.add(livingEntity);
            }
        }
        return livingEntities;
    }

    /**
     * 获取这个范围内的所有方块
     *
     * @param corner1 一点
     * @param corner2 另外一点
     * @return 这范围区域内的所有方块
     */
    @Nonnull
    static List<Block> getLocationBlocks(Location corner1, Location corner2) {
        return getLocationBlocks(corner1, corner2, true);
    }

    /**
     * 获取这个范围内的所有方块
     *
     * @param corner1 一点
     * @param corner2 另外一点
     * @param height  是否计算高度（false则遍历0-255）
     * @return 这范围区域内的所有方块
     */
    @Nonnull
    static List<Block> getLocationBlocks(Location corner1, Location corner2, boolean height) {
        if (!corner1.getWorld().equals(corner2.getWorld())) {
            throw new IllegalArgumentException("必须为同一个世界");
        }

        World world = corner1.getWorld();
        List<Block> blocks = new Vector<>();

        int minX = Math.min(corner1.getBlockX(), corner2.getBlockX());
        int maxX = Math.max(corner1.getBlockX(), corner2.getBlockX());
        int minY = height ? Math.min(corner1.getBlockY(), corner2.getBlockY()) : 0;
        int maxY = height ? Math.max(corner1.getBlockY(), corner2.getBlockY()) : 255;
        int minZ = Math.min(corner1.getBlockZ(), corner2.getBlockZ());
        int maxZ = Math.max(corner1.getBlockZ(), corner2.getBlockZ());

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = minY; y <= maxY; y++) {
                    blocks.add(world.getBlockAt(x, y, z));
                }
            }
        }
        return blocks;
    }

    /**
     * 获取这个范围内所有的Location实例
     *
     * @param corner1 一点
     * @param corner2 另外一点
     * @return location集合
     */
    @Nonnull
    static List<Location> getRanges(Location corner1, Location corner2) {
        return getRanges(corner1, corner2, true);
    }

    /**
     * 获取这个范围内所有的Location实例
     *
     * @param corner1 一点
     * @param corner2 另外一点
     * @param height  是否细分高度，为false则0-255高度
     * @return location集合
     */
    @Nonnull
    static List<Location> getRanges(Location corner1, Location corner2, boolean height) {
        if (!corner1.getWorld().equals(corner2.getWorld())) {
            throw new IllegalArgumentException("必须为同一个世界");
        }

        World world = corner1.getWorld();
        List<Location> locations = new ArrayList<>();

        int minX = Math.min(corner1.getBlockX(), corner2.getBlockX());
        int maxX = Math.max(corner1.getBlockX(), corner2.getBlockX());
        int minY = height ? Math.min(corner1.getBlockY(), corner2.getBlockY()) : 0;
        int maxY = height ? Math.max(corner1.getBlockY(), corner2.getBlockY()) : 255;
        int minZ = Math.min(corner1.getBlockZ(), corner2.getBlockZ());
        int maxZ = Math.max(corner1.getBlockZ(), corner2.getBlockZ());

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = minY; y <= maxY; y++) {
                    locations.add(new Location(world, x, y, z));
                }
            }
        }
        return locations;
    }

    /**
     * 获取距离这个坐标最近的玩家
     *
     * @param location 涉及的坐标
     * @return 最近的玩家，如果没有人在这个世界则为null
     */
    @Nullable
    static Player getShortest(Location location) {
        return getShortest(location, true);
    }

    /**
     * 获取距离这个坐标最近的玩家
     *
     * @param location 涉及的坐标
     * @param height   是否计算高度
     * @return 最近的玩家，如果没有人在这个世界则为null
     */
    @Nullable
    static Player getShortest(Location location, boolean height) {
        World world = location.getWorld();
        Player nearest = null;
        double minDistance = Double.MAX_VALUE;

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getLocation().getWorld().equals(world)) {
                double distance = getFar(location, player.getLocation(), height);
                if (distance < minDistance) {
                    nearest = player;
                    minDistance = distance;
                }
            }
        }
        return nearest;
    }

    /**
     * 获取距离这个坐标最远的玩家
     *
     * @param location 涉及的坐标
     * @return 最远的玩家，如果没有人在这个世界则为null
     */
    @Nullable
    static Player getLongest(Location location) {
        return getLongest(location, true);
    }

    /**
     * 获取距离这个坐标最远的玩家
     *
     * @param location 涉及的坐标
     * @param height   是否计算高度
     * @return 最远的玩家，如果没有人在这个世界则为null
     */
    @Nullable
    static Player getLongest(Location location, boolean height) {
        World world = location.getWorld();
        Player farthest = null;
        double maxDistance = -1;

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getLocation().getWorld().equals(world)) {
                double distance = getFar(location, player.getLocation(), height);
                if (distance > maxDistance) {
                    farthest = player;
                    maxDistance = distance;
                }
            }
        }
        return farthest;
    }

    /**
     * 让实体看向某个地点
     *
     * @param entity   涉及的实体
     * @param location 看向的地点
     */
    static void seeLocation(Entity entity, Location location) {
        Location entityLoc = entity.getLocation();
        entityLoc.setDirection(IVector.getVector(entityLoc, location));
        entity.teleport(entityLoc);
    }
}