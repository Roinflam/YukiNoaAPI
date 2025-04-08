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
import java.util.stream.Stream;

/**
 * Localtion相关操作，默认计算高度
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
        double far = 0;
        if (!one.getWorld().equals(two.getWorld())) {
            throw new IllegalArgumentException("必须为同一个世界");
        } else {
            far += one.getX() > two.getX() ? one.getX() - two.getX() : two.getX() - one.getX();
            if (height) {
                far += one.getY() > two.getY() ? one.getY() - two.getY() : two.getY() - one.getY();
            }
            far += one.getZ() > two.getZ() ? one.getZ() - two.getZ() : two.getZ() - one.getZ();
        }
        return far;
    }

    /**
     * 获取这两个坐标的距离(同世界)
     *
     * @param one 其中一个坐标
     * @param two 另一个坐标
     * @return 距离
     */
    static int getFarGrid(Location one, Location two) {
        return getFarGrid(one, two, true);
    }

    /**
     * 获取这两个坐标的距离(同世界)
     *
     * @param one    其中一个坐标
     * @param two    另一个坐标
     * @param height 是否计算高度
     * @return 距离
     */
    static int getFarGrid(Location one, Location two, boolean height) {
        int far = 0;
        if (!one.getWorld().equals(two.getWorld())) {
            throw new IllegalArgumentException("必须为同一个世界");
        } else {
            far += one.getBlockX() > two.getBlockX() ? one.getBlockX() - two.getBlockX() : two.getBlockX() - one.getBlockX();
            if (height) {
                far += one.getBlockY() > two.getBlockY() ? one.getBlockY() - two.getBlockY() : two.getBlockY() - one.getBlockY();
            }
            far += one.getBlockZ() > two.getBlockZ() ? one.getBlockZ() - two.getBlockZ() : two.getBlockZ() - one.getBlockZ();
        }
        return far;
    }

    /**
     * 判断坐标是否在范围内
     *
     * @param xLocation 一点
     * @param zLocation 另外一点
     * @param location  坐标
     * @return 在则true，否则为false
     */
    static boolean isWithinRange(Location xLocation, Location zLocation, Location location) {
        return isWithinRange(xLocation, zLocation, location, true);
    }

    /**
     * 判断坐标是否在范围内
     *
     * @param xLocation 一点
     * @param zLocation 另外一点
     * @param location  坐标
     * @param height    是否计算高度（false为无视）
     * @return 在则true，否则为false
     */
    static boolean isWithinRange(Location xLocation, Location zLocation, Location location, boolean height) {
        World world = xLocation.getWorld();
        if (!world.equals(zLocation.getWorld())) {
            throw new IllegalArgumentException("必须为同一个世界");
        } else {
            if (!world.equals(location.getWorld())) {
                return false;
            } else {
                if (xLocation.getX() > zLocation.getX()) {
                    if (location.getX() <= xLocation.getX() && location.getX() >= zLocation.getX()) {
                        if (xLocation.getZ() > zLocation.getZ()) {
                            if (location.getZ() <= xLocation.getZ() && location.getZ() >= zLocation.getZ()) {
                                if (xLocation.getY() > zLocation.getY()) {
                                    return location.getY() <= xLocation.getY() && location.getY() >= zLocation.getY();
                                } else if (xLocation.getY() < zLocation.getY()) {
                                    return location.getY() >= xLocation.getY() && location.getY() <= zLocation.getY();
                                } else if (xLocation.getY() == zLocation.getY()) {
                                    return location.getY() == xLocation.getY() && location.getY() == zLocation.getY();
                                }
                            }
                        } else if (xLocation.getZ() < zLocation.getZ()) {
                            if (location.getZ() >= xLocation.getZ() && location.getZ() <= zLocation.getZ()) {
                                if (xLocation.getY() > zLocation.getY()) {
                                    return location.getY() <= xLocation.getY() && location.getY() >= zLocation.getY();
                                } else if (xLocation.getY() < zLocation.getY()) {
                                    return location.getY() >= xLocation.getY() && location.getY() <= zLocation.getY();
                                } else if (xLocation.getY() == zLocation.getY()) {
                                    return location.getY() == xLocation.getY() && location.getY() == zLocation.getY();
                                }
                            }
                        } else if (xLocation.getZ() == zLocation.getZ()) {
                            if (location.getZ() == xLocation.getZ() && location.getZ() == zLocation.getZ()) {
                                if (xLocation.getY() > zLocation.getY()) {
                                    return location.getY() <= xLocation.getY() && location.getY() >= zLocation.getY();
                                } else if (xLocation.getY() < zLocation.getY()) {
                                    return location.getY() >= xLocation.getY() && location.getY() <= zLocation.getY();
                                } else if (xLocation.getY() == zLocation.getY()) {
                                    return location.getY() == xLocation.getY() && location.getY() == zLocation.getY();
                                }
                            }
                        }
                    }
                } else if (xLocation.getX() < zLocation.getX()) {
                    if (location.getX() >= xLocation.getX() && location.getX() <= zLocation.getX()) {
                        if (xLocation.getZ() > zLocation.getZ()) {
                            if (location.getZ() <= xLocation.getZ() && location.getZ() >= zLocation.getZ()) {
                                if (xLocation.getY() > zLocation.getY()) {
                                    return location.getY() <= xLocation.getY() && location.getY() >= zLocation.getY();
                                } else if (xLocation.getY() < zLocation.getY()) {
                                    return location.getY() >= xLocation.getY() && location.getY() <= zLocation.getY();
                                } else if (xLocation.getY() == zLocation.getY()) {
                                    return location.getY() == xLocation.getY() && location.getY() == zLocation.getY();
                                }
                            }
                        } else if (xLocation.getZ() < zLocation.getZ()) {
                            if (location.getZ() >= xLocation.getZ() && location.getZ() <= zLocation.getZ()) {
                                if (xLocation.getY() > zLocation.getY()) {
                                    return location.getY() <= xLocation.getY() && location.getY() >= zLocation.getY();
                                } else if (xLocation.getY() < zLocation.getY()) {
                                    return location.getY() >= xLocation.getY() && location.getY() <= zLocation.getY();
                                } else if (xLocation.getY() == zLocation.getY()) {
                                    return location.getY() == xLocation.getY() && location.getY() == zLocation.getY();
                                }
                            }
                        } else if (xLocation.getZ() == zLocation.getZ()) {
                            if (location.getZ() == xLocation.getZ() && location.getZ() == zLocation.getZ()) {
                                if (xLocation.getY() > zLocation.getY()) {
                                    return location.getY() <= xLocation.getY() && location.getY() >= zLocation.getY();
                                } else if (xLocation.getY() < zLocation.getY()) {
                                    return location.getY() >= xLocation.getY() && location.getY() <= zLocation.getY();
                                } else if (xLocation.getY() == zLocation.getY()) {
                                    return location.getY() == xLocation.getY() && location.getY() == zLocation.getY();
                                }
                            }
                        }
                    }
                } else {
                    if (location.getX() == xLocation.getX() && location.getX() == zLocation.getX()) {
                        if (xLocation.getZ() > zLocation.getZ()) {
                            if (location.getZ() <= xLocation.getZ() && location.getZ() >= zLocation.getZ()) {
                                if (xLocation.getY() > zLocation.getY()) {
                                    return location.getY() <= xLocation.getY() && location.getY() >= zLocation.getY();
                                } else if (xLocation.getY() < zLocation.getY()) {
                                    return location.getY() >= xLocation.getY() && location.getY() <= zLocation.getY();
                                } else if (xLocation.getY() == zLocation.getY()) {
                                    return location.getY() == xLocation.getY() && location.getY() == zLocation.getY();
                                }
                            }
                        } else if (xLocation.getZ() < zLocation.getZ()) {
                            if (location.getZ() >= xLocation.getZ() && location.getZ() <= zLocation.getZ()) {
                                if (xLocation.getY() > zLocation.getY()) {
                                    return location.getY() <= xLocation.getY() && location.getY() >= zLocation.getY();
                                } else if (xLocation.getY() < zLocation.getY()) {
                                    return location.getY() >= xLocation.getY() && location.getY() <= zLocation.getY();
                                } else if (xLocation.getY() == zLocation.getY()) {
                                    return location.getY() == xLocation.getY() && location.getY() == zLocation.getY();
                                }
                            }
                        } else if (xLocation.getZ() == zLocation.getZ()) {
                            if (location.getZ() == xLocation.getZ() && location.getZ() == zLocation.getZ()) {
                                if (xLocation.getY() > zLocation.getY()) {
                                    return location.getY() <= xLocation.getY() && location.getY() >= zLocation.getY();
                                } else if (xLocation.getY() < zLocation.getY()) {
                                    return location.getY() >= xLocation.getY() && location.getY() <= zLocation.getY();
                                } else if (xLocation.getY() == zLocation.getY()) {
                                    return location.getY() == xLocation.getY() && location.getY() == zLocation.getY();
                                }
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * 获取这个范围内的所有实体
     *
     * @param xLocation 一点
     * @param zLocation 另外一点
     * @return 这范围区域内的所有实体
     */
    
    static List<Entity> getLocationEntitys(Location xLocation, Location zLocation) {
        return getLocationEntitys(xLocation, zLocation, true);
    }

    /**
     * 获取这个范围内的所有实体
     *
     * @param xLocation 一点
     * @param zLocation 另外一点
     * @param height    是否计算高度（false为无视）
     * @return 这范围区域内的所有实体
     */
    
    static List<Entity> getLocationEntitys(Location xLocation, Location zLocation, boolean height) {
        List<Entity> entities = new ArrayList<>();
        World world = xLocation.getWorld();
        if (!world.equals(zLocation.getWorld())) {
            throw new IllegalArgumentException("必须为同一个世界");
        } else {
            for (Entity entity : xLocation.getWorld().getEntities()) {
                if (isWithinRange(xLocation, zLocation, entity.getLocation(), height)) {
                    entities.add(entity);
                }
            }
        }
        return entities;
    }

    /**
     * 获取这个范围内的所有生物
     *
     * @param xLocation 一点
     * @param zLocation 另外一点
     * @return 这范围区域内的所有实体
     */
    
    static List<LivingEntity> getLocationLivingEntitys(Location xLocation, Location zLocation) {
        return getLocationLivingEntitys(xLocation, zLocation, true);
    }

    /**
     * 获取这个范围内的所有生物
     *
     * @param xLocation 一点
     * @param zLocation 另外一点
     * @param height    是否计算高度（false为无视）
     * @return 这范围区域内的所有实体
     */
    
    static List<LivingEntity> getLocationLivingEntitys(Location xLocation, Location zLocation, boolean height) {
        List<LivingEntity> livingEntities = new ArrayList<>();
        World world = xLocation.getWorld();
        if (!world.equals(zLocation.getWorld())) {
            throw new IllegalArgumentException("必须为同一个世界");
        } else {
            for (LivingEntity livingEntity : xLocation.getWorld().getLivingEntities()) {
                if (isWithinRange(xLocation, zLocation, livingEntity.getLocation(), height)) {
                    livingEntities.add(livingEntity);
                }
            }
        }
        return livingEntities;
    }

    /**
     * 获取这个范围内的所有方块
     *
     * @param xLocation 一点
     * @param zLocation 另外一点
     * @return 这范围区域内的所有实体
     */
    
    static List<Block> getLocationBlocks(Location xLocation, Location zLocation) {
        return getLocationBlocks(xLocation, zLocation, true);
    }

    /**
     * 获取这个范围内的所有方块
     *
     * @param xLocation 一点
     * @param zLocation 另外一点
     * @param height    是否计算高度（false为无视）
     * @return 这范围区域内的所有实体
     */
    
    static List<Block> getLocationBlocks(Location xLocation, Location zLocation, boolean height) {
        List<Block> blocks = new Vector<>();
        World world = xLocation.getWorld();
        if (!world.equals(zLocation.getWorld())) {
            throw new IllegalArgumentException("必须为同一个世界");
        } else {
            int x1 = Math.min(xLocation.getBlockX(), zLocation.getBlockX());
            int x2 = Math.max(xLocation.getBlockX(), zLocation.getBlockX());
            int y1 = Math.min(xLocation.getBlockY(), zLocation.getBlockY());
            int y2 = Math.max(xLocation.getBlockY(), zLocation.getBlockY());
            if (!height) {
                y1 = 0;
                y2 = 255;
            }
            int z1 = Math.min(xLocation.getBlockZ(), zLocation.getBlockZ());
            int z2 = Math.max(xLocation.getBlockZ(), zLocation.getBlockZ());
            Integer[] locationListX = new Integer[16];
            Integer[] locationListZ = new Integer[16];
            Integer[] locationListY = new Integer[256];
            for (int x = x1; x <= x2; ++x) {
                locationListX[x] = x;
            }
            for (int z = z1; z <= z2; ++z) {
                locationListZ[z] = z;
            }
            for (int y = y1; y < y2; ++y) {
                locationListY[y] = y;
            }
            Stream.of(locationListX).parallel().forEach(x -> Stream.of(locationListZ).parallel().forEach(z -> Stream.of(locationListY).parallel().forEach(y -> {
                Block block = world.getBlockAt(x, y, z);
                blocks.add(block);
            })));
        }
        return blocks;
    }


    /**
     * 获取这个范围内所有的location实例
     *
     * @param xLocation 一点
     * @param zLocation 另外一点
     * @return location集合
     */
    
    static List<Location> getRanges(Location xLocation, Location zLocation) {
        return getRanges(xLocation, zLocation, true);
    }

    /**
     * 获取这个范围内所有的location实例
     *
     * @param xLocation 一点
     * @param zLocation 另外一点
     * @param height    是否细分高度，为false则0-255高度
     * @return location集合
     */
    
    static List<Location> getRanges(Location xLocation, Location zLocation, boolean height) {
        World world = xLocation.getWorld();
        if (!world.equals(zLocation.getWorld())) {
            throw new IllegalArgumentException("必须为同一个世界");
        }
        ArrayList<Location> locations = new ArrayList<>();
        if (xLocation.getBlockX() > zLocation.getBlockX()) {
            if (xLocation.getBlockZ() > zLocation.getBlockZ()) {
                if (height) {
                    if (xLocation.getBlockY() > zLocation.getBlockY()) {
                        for (int x = zLocation.getBlockX(); x <= xLocation.getBlockX(); x++) {
                            for (int z = zLocation.getBlockZ(); z <= xLocation.getBlockZ(); z++) {
                                for (int y = zLocation.getBlockY(); y < xLocation.getBlockY(); y++) {
                                    locations.add(new Location(world, x, y, z));
                                }
                            }
                        }
                    } else if (xLocation.getBlockY() < zLocation.getBlockY()) {
                        for (int x = zLocation.getBlockX(); x <= xLocation.getBlockX(); x++) {
                            for (int z = zLocation.getBlockZ(); z <= xLocation.getBlockZ(); z++) {
                                for (int y = xLocation.getBlockY(); y < zLocation.getBlockY(); y++) {
                                    locations.add(new Location(world, x, y, z));
                                }
                            }
                        }
                    } else {
                        for (int x = zLocation.getBlockX(); x <= xLocation.getBlockX(); x++) {
                            for (int z = zLocation.getBlockZ(); z <= xLocation.getBlockZ(); z++) {
                                locations.add(new Location(world, x, zLocation.getBlockY(), z));
                            }
                        }
                    }
                } else {
                    for (int x = zLocation.getBlockX(); x <= xLocation.getBlockX(); x++) {
                        for (int z = zLocation.getBlockZ(); z <= xLocation.getBlockZ(); z++) {
                            for (int y = 0; y < 256; y++) {
                                locations.add(new Location(world, x, y, z));
                            }
                        }
                    }
                }
            } else if (xLocation.getBlockZ() < zLocation.getBlockZ()) {
                if (height) {
                    if (xLocation.getBlockY() > zLocation.getBlockY()) {
                        for (int x = zLocation.getBlockX(); x <= xLocation.getBlockX(); x++) {
                            for (int z = xLocation.getBlockZ(); z <= zLocation.getBlockZ(); z++) {
                                for (int y = zLocation.getBlockY(); y < xLocation.getBlockY(); y++) {
                                    locations.add(new Location(world, x, y, z));
                                }
                            }
                        }
                    } else if (xLocation.getBlockY() < zLocation.getBlockY()) {
                        for (int x = zLocation.getBlockX(); x <= xLocation.getBlockX(); x++) {
                            for (int z = xLocation.getBlockZ(); z <= zLocation.getBlockZ(); z++) {
                                for (int y = xLocation.getBlockY(); y < zLocation.getBlockY(); y++) {
                                    locations.add(new Location(world, x, y, z));
                                }
                            }
                        }
                    } else {
                        for (int x = zLocation.getBlockX(); x <= xLocation.getBlockX(); x++) {
                            for (int z = xLocation.getBlockZ(); z <= zLocation.getBlockZ(); z++) {
                                locations.add(new Location(world, x, zLocation.getBlockY(), z));
                            }
                        }
                    }
                } else {
                    for (int x = zLocation.getBlockX(); x <= xLocation.getBlockX(); x++) {
                        for (int z = xLocation.getBlockZ(); z <= zLocation.getBlockZ(); z++) {
                            for (int y = 0; y < 256; y++) {
                                locations.add(new Location(world, x, y, z));
                            }
                        }
                    }
                }
            } else {
                if (height) {
                    if (xLocation.getBlockY() > zLocation.getBlockY()) {
                        for (int x = zLocation.getBlockX(); x <= xLocation.getBlockX(); x++) {
                            for (int y = zLocation.getBlockY(); y < xLocation.getBlockY(); y++) {
                                locations.add(new Location(world, x, y, zLocation.getBlockZ()));
                            }
                        }
                    } else if (xLocation.getBlockY() < zLocation.getBlockY()) {
                        for (int x = zLocation.getBlockX(); x <= xLocation.getBlockX(); x++) {
                            for (int y = xLocation.getBlockY(); y < zLocation.getBlockY(); y++) {
                                locations.add(new Location(world, x, y, zLocation.getBlockZ()));
                            }
                        }
                    } else {
                        for (int x = zLocation.getBlockX(); x <= xLocation.getBlockX(); x++) {
                            locations.add(new Location(world, x, zLocation.getBlockY(), zLocation.getBlockZ()));
                        }
                    }
                } else {
                    for (int x = zLocation.getBlockX(); x <= xLocation.getBlockX(); x++) {
                        for (int y = 0; y < 256; y++) {
                            locations.add(new Location(world, x, y, zLocation.getBlockZ()));
                        }
                    }
                }
            }
        } else if (xLocation.getBlockX() < zLocation.getBlockX()) {
            if (xLocation.getBlockZ() > zLocation.getBlockZ()) {
                if (height) {
                    if (xLocation.getBlockY() > zLocation.getBlockY()) {
                        for (int x = xLocation.getBlockX(); x <= zLocation.getBlockX(); x++) {
                            for (int z = zLocation.getBlockZ(); z <= xLocation.getBlockZ(); z++) {
                                for (int y = zLocation.getBlockY(); y < xLocation.getBlockY(); y++) {
                                    locations.add(new Location(world, x, y, z));
                                }
                            }
                        }
                    } else if (xLocation.getBlockY() < zLocation.getBlockY()) {
                        for (int x = xLocation.getBlockX(); x <= zLocation.getBlockX(); x++) {
                            for (int z = zLocation.getBlockZ(); z <= xLocation.getBlockZ(); z++) {
                                for (int y = xLocation.getBlockY(); y < zLocation.getBlockY(); y++) {
                                    locations.add(new Location(world, x, y, z));
                                }
                            }
                        }
                    } else {
                        for (int x = xLocation.getBlockX(); x <= zLocation.getBlockX(); x++) {
                            for (int z = zLocation.getBlockZ(); z <= xLocation.getBlockZ(); z++) {
                                locations.add(new Location(world, x, zLocation.getBlockY(), z));
                            }
                        }
                    }
                } else {
                    for (int x = xLocation.getBlockX(); x <= zLocation.getBlockX(); x++) {
                        for (int z = zLocation.getBlockZ(); z <= xLocation.getBlockZ(); z++) {
                            for (int y = 0; y < 256; y++) {
                                locations.add(new Location(world, x, y, z));
                            }
                        }
                    }
                }
            } else if (xLocation.getBlockZ() < zLocation.getBlockZ()) {
                if (height) {
                    if (xLocation.getBlockY() > zLocation.getBlockY()) {
                        for (int x = xLocation.getBlockX(); x <= zLocation.getBlockX(); x++) {
                            for (int z = xLocation.getBlockZ(); z <= zLocation.getBlockZ(); z++) {
                                for (int y = zLocation.getBlockY(); y < xLocation.getBlockY(); y++) {
                                    locations.add(new Location(world, x, y, z));
                                }
                            }
                        }
                    } else if (xLocation.getBlockY() < zLocation.getBlockY()) {
                        for (int x = xLocation.getBlockX(); x <= zLocation.getBlockX(); x++) {
                            for (int z = xLocation.getBlockZ(); z <= zLocation.getBlockZ(); z++) {
                                for (int y = xLocation.getBlockY(); y < zLocation.getBlockY(); y++) {
                                    locations.add(new Location(world, x, y, z));
                                }
                            }
                        }
                    } else {
                        for (int x = xLocation.getBlockX(); x <= zLocation.getBlockX(); x++) {
                            for (int z = xLocation.getBlockZ(); z <= zLocation.getBlockZ(); z++) {
                                locations.add(new Location(world, x, zLocation.getBlockY(), z));
                            }
                        }
                    }
                } else {
                    for (int x = xLocation.getBlockX(); x <= zLocation.getBlockX(); x++) {
                        for (int z = xLocation.getBlockZ(); z <= zLocation.getBlockZ(); z++) {
                            for (int y = 0; y < 256; y++) {
                                locations.add(new Location(world, x, y, z));
                            }
                        }
                    }
                }
            } else {
                if (height) {
                    if (xLocation.getBlockY() > zLocation.getBlockY()) {
                        for (int x = xLocation.getBlockX(); x <= zLocation.getBlockX(); x++) {
                            for (int y = zLocation.getBlockY(); y < xLocation.getBlockY(); y++) {
                                locations.add(new Location(world, x, y, zLocation.getBlockZ()));
                            }
                        }
                    } else if (xLocation.getBlockY() < zLocation.getBlockY()) {
                        for (int x = xLocation.getBlockX(); x <= zLocation.getBlockX(); x++) {
                            for (int y = xLocation.getBlockY(); y < zLocation.getBlockY(); y++) {
                                locations.add(new Location(world, x, y, zLocation.getBlockZ()));
                            }
                        }
                    } else {
                        for (int x = xLocation.getBlockX(); x <= zLocation.getBlockX(); x++) {
                            locations.add(new Location(world, x, zLocation.getBlockY(), zLocation.getBlockZ()));
                        }
                    }
                } else {
                    for (int x = xLocation.getBlockX(); x <= zLocation.getBlockX(); x++) {
                        for (int y = 0; y < 256; y++) {
                            locations.add(new Location(world, x, y, zLocation.getBlockZ()));
                        }
                    }
                }
            }
        } else {
            if (xLocation.getBlockZ() > zLocation.getBlockZ()) {
                if (height) {
                    if (xLocation.getBlockY() > zLocation.getBlockY()) {
                        for (int z = zLocation.getBlockZ(); z <= xLocation.getBlockZ(); z++) {
                            for (int y = zLocation.getBlockY(); y < xLocation.getBlockY(); y++) {
                                locations.add(new Location(world, zLocation.getBlockX(), y, z));
                            }

                        }
                    } else if (xLocation.getBlockY() < zLocation.getBlockY()) {
                        for (int z = zLocation.getBlockZ(); z <= xLocation.getBlockZ(); z++) {
                            for (int y = xLocation.getBlockY(); y < zLocation.getBlockY(); y++) {
                                locations.add(new Location(world, zLocation.getBlockX(), y, z));
                            }
                        }
                    } else {
                        for (int z = zLocation.getBlockZ(); z <= xLocation.getBlockZ(); z++) {
                            locations.add(new Location(world, zLocation.getBlockX(), zLocation.getBlockY(), z));
                        }
                    }
                } else {
                    for (int z = zLocation.getBlockZ(); z <= xLocation.getBlockZ(); z++) {
                        for (int y = 0; y < 256; y++) {
                            locations.add(new Location(world, zLocation.getBlockX(), y, z));
                        }
                    }
                }
            } else if (xLocation.getBlockZ() < zLocation.getBlockZ()) {
                if (height) {
                    if (xLocation.getBlockY() > zLocation.getBlockY()) {
                        for (int z = xLocation.getBlockZ(); z <= zLocation.getBlockZ(); z++) {
                            for (int y = zLocation.getBlockY(); y < xLocation.getBlockY(); y++) {
                                locations.add(new Location(world, zLocation.getBlockX(), y, z));
                            }
                        }
                    } else if (xLocation.getBlockY() < zLocation.getBlockY()) {

                        for (int z = xLocation.getBlockZ(); z <= zLocation.getBlockZ(); z++) {
                            for (int y = xLocation.getBlockY(); y < zLocation.getBlockY(); y++) {
                                locations.add(new Location(world, zLocation.getBlockX(), y, z));
                            }
                        }
                    } else {

                        for (int z = xLocation.getBlockZ(); z <= zLocation.getBlockZ(); z++) {
                            locations.add(new Location(world, zLocation.getBlockX(), zLocation.getBlockY(), z));
                        }
                    }
                } else {

                    for (int z = xLocation.getBlockZ(); z <= zLocation.getBlockZ(); z++) {
                        for (int y = 0; y < 256; y++) {
                            locations.add(new Location(world, zLocation.getBlockX(), y, z));
                        }
                    }
                }
            } else {
                if (height) {
                    if (xLocation.getBlockY() > zLocation.getBlockY()) {
                        for (int y = zLocation.getBlockY(); y < xLocation.getBlockY(); y++) {
                            locations.add(new Location(world, zLocation.getBlockX(), y, zLocation.getBlockZ()));
                        }
                    } else if (xLocation.getBlockY() < zLocation.getBlockY()) {
                        for (int y = xLocation.getBlockY(); y < zLocation.getBlockY(); y++) {
                            locations.add(new Location(world, zLocation.getBlockX(), y, zLocation.getBlockZ()));
                        }
                    } else {
                        locations.add(new Location(world, zLocation.getBlockX(), zLocation.getBlockY(), zLocation.getBlockZ()));
                    }
                } else {
                    for (int y = 0; y < 256; y++) {
                        locations.add(new Location(world, zLocation.getBlockX(), y, zLocation.getBlockZ()));
                    }
                }
            }
        }
        return locations;
    }

    /**
     * 获取距离这个坐标最近的玩家
     *
     * @param location 涉及的坐标
     * @return 距离的玩家，如果没有人在这个世界，则为null
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
     * @return 距离的玩家，如果没有人在这个世界，则为null
     */
    @Nullable
    static Player getShortest(Location location, boolean height) {
        World world = location.getWorld();
        @Nullable Player player = null;
        double distance = Double.MAX_VALUE;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getLocation().getWorld().equals(world)) {
                double d = getFar(location, p.getLocation(), height);
                if (d < distance) {
                    player = p;
                    distance = d;
                }
            }
        }
        return player;
    }

    /**
     * 获取距离这个坐标最远的玩家
     *
     * @param location 涉及的坐标
     * @return 距离最远的玩家，如果没有人在这个世界，则为null
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
     * @return 距离最远的玩家，如果没有人在这个世界，则为null
     */
    @Nullable
    static Player getLongest(Location location, boolean height) {
        World world = location.getWorld();
        @Nullable Player player = null;
        double distance = -1;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getLocation().getWorld().equals(world)) {
                double d = getFar(location, p.getLocation(), height);
                if (d > distance) {
                    player = p;
                    distance = d;
                }
            }
        }
        return player;
    }

    /**
     * 让生物看向某个地点
     *
     * @param entity   涉及的生物
     * @param location 看向的地点
     */
    static void seeLocation(Entity entity, Location location) {
        entity.teleport(entity.getLocation().setDirection(IVector.getVector(entity.getLocation(), location)));
    }
}
