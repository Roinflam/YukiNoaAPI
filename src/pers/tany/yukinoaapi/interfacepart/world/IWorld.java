package pers.tany.yukinoaapi.interfacepart.world;

import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;


/**
 * 世界相关操作
 */
public interface IWorld {
    /**
     * 获取这个世界的玩家数量
     *
     * @param world 涉及的世界
     * @return 玩家的数量
     */
    static int playerNumber(World world) {
        int i = 0;
        for (LivingEntity livingEntity : world.getLivingEntities()) {
            if (livingEntity instanceof Player) {
                i++;
            }
        }
        return i;
    }

    /**
     * 获取这个世界的玩家数量
     *
     * @param world   涉及的世界
     * @param mutiple 倍率换算
     * @return 玩家的数量)
     */
    static double playerNumber(World world, boolean mutiple) {
        double i = 0;
        for (Entity entity : world.getEntities()) {
            if (entity instanceof Player) {
                i += mutiple && i > 0 ? 0.5 : 1;
            }
        }
        return i;
    }
}
