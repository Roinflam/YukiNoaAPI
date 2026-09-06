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
import java.util.Collection;
import java.util.List;

/**
 * Location相关操作接口
 * 提供坐标计算、范围判断、实体/方块获取等功能
 * 优化算法性能，减少不必要的对象创建
 */
public final class ILocation {

    /**
     * 工具类,禁止实例化
     *
     * @throws UnsupportedOperationException 总是抛出
     */
    private ILocation() {
        throw new UnsupportedOperationException("ILocation 是工具类,不允许实例化");
    }


    /**
     * 获取这两个坐标的距离(同世界)
     * 使用曼哈顿距离计算
     *
     * @param one 其中一个坐标
     * @param two 另一个坐标
     * @return 距离
     */
    public static double getFar(Location one, Location two) {
        return getFar(one, two, true);
    }

    /**
     * 获取这两个坐标的距离(同世界)
     * 使用曼哈顿距离计算
     *
     * @param one    其中一个坐标
     * @param two    另一个坐标
     * @param height 是否计算高度
     * @return 距离
     */
    public static double getFar(Location one, Location two, boolean height) {
        if (!one.getWorld().equals(two.getWorld())) {
            throw new IllegalArgumentException("必须为同一个世界");
        }

        double dx = Math.abs(one.getX() - two.getX());
        double dz = Math.abs(one.getZ() - two.getZ());

        if (height) {
            double dy = Math.abs(one.getY() - two.getY());
            return dx + dy + dz;
        }
        return dx + dz;
    }

    /**
     * 获取这两个坐标的欧几里得距离平方
     * 比 distance() 更快，因为不需要开方
     * 适用于距离比较场景
     *
     * @param one 其中一个坐标
     * @param two 另一个坐标
     * @return 距离的平方
     */
    public static double getDistanceSquared(Location one, Location two) {
        if (!one.getWorld().equals(two.getWorld())) {
            throw new IllegalArgumentException("必须为同一个世界");
        }
        return one.distanceSquared(two);
    }

    /**
     * 获取这两个坐标的格子距离(同世界)
     *
     * @param one 其中一个坐标
     * @param two 另一个坐标
     * @return 距离
     */
    public static int getFarGrid(Location one, Location two) {
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
    public static int getFarGrid(Location one, Location two, boolean height) {
        if (!one.getWorld().equals(two.getWorld())) {
            throw new IllegalArgumentException("必须为同一个世界");
        }

        int dx = Math.abs(one.getBlockX() - two.getBlockX());
        int dz = Math.abs(one.getBlockZ() - two.getBlockZ());

        if (height) {
            int dy = Math.abs(one.getBlockY() - two.getBlockY());
            return dx + dy + dz;
        }
        return dx + dz;
    }

    /**
     * 判断坐标是否在范围内
     * 使用预计算边界值优化性能
     *
     * @param corner1  一点
     * @param corner2  另外一点
     * @param location 坐标
     * @return 在则true，否则为false
     */
    public static boolean isWithinRange(Location corner1, Location corner2, Location location) {
        return isWithinRange(corner1, corner2, location, true);
    }

    /**
     * 判断坐标是否在范围内
     * 使用预计算边界值优化性能
     *
     * @param corner1  一点
     * @param corner2  另外一点
     * @param location 坐标
     * @param height   是否计算高度（false为无视Y轴）
     * @return 在则true，否则为false
     */
    public static boolean isWithinRange(Location corner1, Location corner2, Location location, boolean height) {
        if (!corner1.getWorld().equals(corner2.getWorld())) {
            throw new IllegalArgumentException("必须为同一个世界");
        }
        if (!corner1.getWorld().equals(location.getWorld())) {
            return false;
        }

        double x = location.getX();
        double z = location.getZ();

        double x1 = corner1.getX();
        double x2 = corner2.getX();
        double z1 = corner1.getZ();
        double z2 = corner2.getZ();

        double minX = Math.min(x1, x2);
        double maxX = Math.max(x1, x2);
        double minZ = Math.min(z1, z2);
        double maxZ = Math.max(z1, z2);

        if (x < minX || x > maxX || z < minZ || z > maxZ) {
            return false;
        }

        if (height) {
            double y = location.getY();
            double y1 = corner1.getY();
            double y2 = corner2.getY();
            double minY = Math.min(y1, y2);
            double maxY = Math.max(y1, y2);
            return y >= minY && y <= maxY;
        }

        return true;
    }

    /**
     * 获取这个范围内的所有实体
     * 使用 Bukkit 原生 API 优化性能
     *
     * @param corner1 一点
     * @param corner2 另外一点
     * @return 这范围区域内的所有实体
     */
    @Nonnull
    public static List<Entity> getLocationEntitys(Location corner1, Location corner2) {
        return getLocationEntitys(corner1, corner2, true);
    }

    /**
     * 获取这个范围内的所有实体
     * 使用 getNearbyEntities 优化性能
     *
     * @param corner1 一点
     * @param corner2 另外一点
     * @param height  是否计算高度（false为无视）
     * @return 这范围区域内的所有实体
     */
    @Nonnull
    public static List<Entity> getLocationEntitys(Location corner1, Location corner2, boolean height) {
        if (!corner1.getWorld().equals(corner2.getWorld())) {
            throw new IllegalArgumentException("必须为同一个世界");
        }

        double dx = Math.abs(corner1.getX() - corner2.getX()) / 2;
        double dy = height ? Math.abs(corner1.getY() - corner2.getY()) / 2 : 128;
        double dz = Math.abs(corner1.getZ() - corner2.getZ()) / 2;

        double centerX = (corner1.getX() + corner2.getX()) / 2;
        double centerY = height ? (corner1.getY() + corner2.getY()) / 2 : corner1.getY();
        double centerZ = (corner1.getZ() + corner2.getZ()) / 2;

        Location center = new Location(corner1.getWorld(), centerX, centerY, centerZ);
        Collection<Entity> nearby = center.getWorld().getNearbyEntities(center, dx, dy, dz);

        List<Entity> result = new ArrayList<>(nearby.size());
        for (Entity entity : nearby) {
            if (isWithinRange(corner1, corner2, entity.getLocation(), height)) {
                result.add(entity);
            }
        }
        return result;
    }

    /**
     * 获取这个范围内的所有生物
     *
     * @param corner1 一点
     * @param corner2 另外一点
     * @return 这范围区域内的所有生物
     */
    @Nonnull
    public static List<LivingEntity> getLocationLivingEntitys(Location corner1, Location corner2) {
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
    public static List<LivingEntity> getLocationLivingEntitys(Location corner1, Location corner2, boolean height) {
        List<Entity> entities = getLocationEntitys(corner1, corner2, height);
        List<LivingEntity> livingEntities = new ArrayList<>(entities.size());

        for (Entity entity : entities) {
            if (entity instanceof LivingEntity) {
                livingEntities.add((LivingEntity) entity);
            }
        }
        return livingEntities;
    }

    /**
     * 获取这个范围内的所有方块
     * 预计算边界值，减少循环内计算
     *
     * @param corner1 一点
     * @param corner2 另外一点
     * @return 这范围区域内的所有方块
     */
    @Nonnull
    public static List<Block> getLocationBlocks(Location corner1, Location corner2) {
        return getLocationBlocks(corner1, corner2, true);
    }

    /**
     * 获取这个范围内的所有方块
     * 预计算边界值，减少循环内计算
     *
     * @param corner1 一点
     * @param corner2 另外一点
     * @param height  是否计算高度（false则遍历0-255）
     * @return 这范围区域内的所有方块
     */
    @Nonnull
    public static List<Block> getLocationBlocks(Location corner1, Location corner2, boolean height) {
        if (!corner1.getWorld().equals(corner2.getWorld())) {
            throw new IllegalArgumentException("必须为同一个世界");
        }

        World world = corner1.getWorld();

        int x1 = corner1.getBlockX();
        int x2 = corner2.getBlockX();
        int z1 = corner1.getBlockZ();
        int z2 = corner2.getBlockZ();

        int minX = Math.min(x1, x2);
        int maxX = Math.max(x1, x2);
        int minZ = Math.min(z1, z2);
        int maxZ = Math.max(z1, z2);

        int minY, maxY;
        if (height) {
            int y1 = corner1.getBlockY();
            int y2 = corner2.getBlockY();
            minY = Math.min(y1, y2);
            maxY = Math.max(y1, y2);
        } else {
            minY = 0;
            maxY = 255;
        }

        int capacity = (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
        List<Block> blocks = new ArrayList<>(capacity);

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
    public static List<Location> getRanges(Location corner1, Location corner2) {
        return getRanges(corner1, corner2, true);
    }

    /**
     * 获取这个范围内所有的Location实例
     * 预分配容量，减少扩容开销
     *
     * @param corner1 一点
     * @param corner2 另外一点
     * @param height  是否细分高度，为false则0-255高度
     * @return location集合
     */
    @Nonnull
    public static List<Location> getRanges(Location corner1, Location corner2, boolean height) {
        if (!corner1.getWorld().equals(corner2.getWorld())) {
            throw new IllegalArgumentException("必须为同一个世界");
        }

        World world = corner1.getWorld();

        int x1 = corner1.getBlockX();
        int x2 = corner2.getBlockX();
        int z1 = corner1.getBlockZ();
        int z2 = corner2.getBlockZ();

        int minX = Math.min(x1, x2);
        int maxX = Math.max(x1, x2);
        int minZ = Math.min(z1, z2);
        int maxZ = Math.max(z1, z2);

        int minY, maxY;
        if (height) {
            int y1 = corner1.getBlockY();
            int y2 = corner2.getBlockY();
            minY = Math.min(y1, y2);
            maxY = Math.max(y1, y2);
        } else {
            minY = 0;
            maxY = 255;
        }

        int capacity = (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
        List<Location> locations = new ArrayList<>(capacity);

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
     * 使用距离平方比较，避免开方运算
     *
     * @param location 涉及的坐标
     * @return 最近的玩家，如果没有人在这个世界则为null
     */
    @Nullable
    public static Player getShortest(Location location) {
        return getShortest(location, true);
    }

    /**
     * 获取距离这个坐标最近的玩家
     * 使用距离平方比较，避免开方运算
     *
     * @param location 涉及的坐标
     * @param height   是否计算高度
     * @return 最近的玩家，如果没有人在这个世界则为null
     */
    @Nullable
    public static Player getShortest(Location location, boolean height) {
        World world = location.getWorld();
        Player nearest = null;
        double minDistSq = Double.MAX_VALUE;

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.getWorld().equals(world)) {
                continue;
            }

            Location pLoc = player.getLocation();
            double distSq;

            if (height) {
                distSq = location.distanceSquared(pLoc);
            } else {
                double dx = location.getX() - pLoc.getX();
                double dz = location.getZ() - pLoc.getZ();
                distSq = dx * dx + dz * dz;
            }

            if (distSq < minDistSq) {
                nearest = player;
                minDistSq = distSq;
            }
        }
        return nearest;
    }

    /**
     * 获取距离这个坐标最远的玩家
     * 使用距离平方比较，避免开方运算
     *
     * @param location 涉及的坐标
     * @return 最远的玩家，如果没有人在这个世界则为null
     */
    @Nullable
    public static Player getLongest(Location location) {
        return getLongest(location, true);
    }

    /**
     * 获取距离这个坐标最远的玩家
     * 使用距离平方比较，避免开方运算
     *
     * @param location 涉及的坐标
     * @param height   是否计算高度
     * @return 最远的玩家，如果没有人在这个世界则为null
     */
    @Nullable
    public static Player getLongest(Location location, boolean height) {
        World world = location.getWorld();
        Player farthest = null;
        double maxDistSq = -1;

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.getWorld().equals(world)) {
                continue;
            }

            Location pLoc = player.getLocation();
            double distSq;

            if (height) {
                distSq = location.distanceSquared(pLoc);
            } else {
                double dx = location.getX() - pLoc.getX();
                double dz = location.getZ() - pLoc.getZ();
                distSq = dx * dx + dz * dz;
            }

            if (distSq > maxDistSq) {
                farthest = player;
                maxDistSq = distSq;
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
    public static void seeLocation(Entity entity, Location location) {
        Location entityLoc = entity.getLocation();
        entityLoc.setDirection(IVector.getVector(entityLoc, location));
        entity.teleport(entityLoc);
    }
}

