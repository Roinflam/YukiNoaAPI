package pers.tany.yukinoaapi.interfacepart.location;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

import javax.annotation.Nonnull;


/**
 * 矢量相关操作
 */
public final class IVector {

    /**
     * 工具类,禁止实例化
     *
     * @throws UnsupportedOperationException 总是抛出
     */
    private IVector() {
        throw new UnsupportedOperationException("IVector 是工具类,不允许实例化");
    }


    /**
     * 获取起始点对终点的矢量
     * xyz不超1
     * 如果需要使用，可提高倍率，此返回值仅提供方向
     *
     * @param startingLocation 起点
     * @param endLocation      终点
     * @return 矢量方向
     */
    
    public static Vector getVector(Location startingLocation, Location endLocation) {
        double x = endLocation.getBlockX() - startingLocation.getBlockX();
        double y = endLocation.getY() - startingLocation.getY();
        double z = endLocation.getBlockZ() - startingLocation.getBlockZ();
        while (x > 1 || z > 1 || y > 1 || x < -1 || z < -1 || y < -1) {
            x /= 10;
            z /= 10;
            y /= 10;
        }
        return new Vector(x, y, z);
    }

    /**
     * 让实体看向坐标
     *
     * @param livingEntity 起点
     * @param lookPos      终点
     */
    public static void lookLocaltion(LivingEntity livingEntity, Location lookPos) {
        Location entityPos = livingEntity.getLocation();
        int z = Math.abs(lookPos.getBlockZ() - entityPos.getBlockZ());
        int x = Math.abs(lookPos.getBlockX() - entityPos.getBlockX());

        float degree = (float) Math.toDegrees(Math.asin(x / Math.sqrt(Math.pow(x, 2) + Math.pow(z, 2))));
        float yaw = 0;

        if (lookPos.getBlockZ() - entityPos.getBlockZ() > 0 && lookPos.getBlockX() - entityPos.getBlockX() > 0) {
            yaw = 360 - degree;
        } else if (lookPos.getBlockZ() - entityPos.getBlockZ() < 0 && lookPos.getBlockX() - entityPos.getBlockX() > 0) {
            yaw = 270 - degree;
        } else if (lookPos.getBlockZ() - entityPos.getBlockZ() < 0 && lookPos.getBlockX() - entityPos.getBlockX() < 0) {
            yaw = 180 - degree;
        } else if (lookPos.getBlockZ() - entityPos.getBlockZ() > 0 && lookPos.getBlockX() - entityPos.getBlockX() < 0) {
            yaw = 90 - degree;
        } else if (lookPos.getBlockZ() - entityPos.getBlockZ() == 0) {
            yaw = lookPos.getBlockX() - entityPos.getBlockX() > 0 ? 270 : 90;
        } else if (lookPos.getBlockX() - entityPos.getBlockX() == 0) {
            yaw = lookPos.getBlockZ() - entityPos.getBlockZ() > 0 ? 0 : -180;
        }

        entityPos.setYaw(yaw);

        int y = Math.abs(lookPos.getBlockY() - entityPos.getBlockY());

        degree = (float) Math.toDegrees(Math.asin(y / Math.sqrt(Math.pow(x, 2) + Math.pow(z, 2) + Math.pow(y, 2))));
        float pitch = 0;

        if (lookPos.getBlockY() - entityPos.getBlockY() > 0) {
            pitch += degree;
        } else if (lookPos.getBlockY() - entityPos.getBlockY() < 0) {
            pitch -= degree;
        } else {
            pitch = 0;
        }

        entityPos.setPitch(pitch);
    }

}


