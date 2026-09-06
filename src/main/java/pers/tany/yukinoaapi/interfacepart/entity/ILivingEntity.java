package pers.tany.yukinoaapi.interfacepart.entity;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.BlockIterator;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 生物操作相关
 */
public final class ILivingEntity {

    /**
     * 工具类,禁止实例化
     *
     * @throws UnsupportedOperationException 总是抛出
     */
    private ILivingEntity() {
        throw new UnsupportedOperationException("ILivingEntity 是工具类,不允许实例化");
    }

    /**
     * 安全地设置实体的血量
     * 如果小于0，则为0
     * 如果大于血量上限，则为血量上限
     *
     * @param livingEntity 被设置的实体
     * @param health       被设置的血量
     */
    public static void setEntityHealth(LivingEntity livingEntity, double health) {
        if (livingEntity.isValid()) {
            if (health > livingEntity.getMaxHealth()) {
                health = livingEntity.getMaxHealth();
            }
            if (health < 0) {
                health = 0;
            }
            livingEntity.setHealth(health);
        }
    }

    /**
     * 获取此生物视野内所有的生物
     *
     * @param livingEntity 涉及的生物
     * @return 视野内的生物
     */
    
    public static List<LivingEntity> getEntityLookingAt(LivingEntity livingEntity) {
        Set<Chunk> chunkSet = new HashSet<>();
        BlockIterator blockIterator = new BlockIterator(livingEntity.getEyeLocation(), 0, Bukkit.getServer().getViewDistance() * 16);
        while (blockIterator.hasNext()) {
            chunkSet.add(blockIterator.next().getChunk());
        }
        List<LivingEntity> entityList = new ArrayList<>();
        for (Chunk chunk : chunkSet) {
            for (Entity entity : chunk.getEntities()) {
                if (entity instanceof LivingEntity && livingEntity.hasLineOfSight(entity)) {
                    entityList.add((LivingEntity) entity);
                }
            }
        }
        return entityList;
    }

    /**
     * 获取此生物视野内所有的生物
     *
     * @param livingEntity 涉及的生物
     * @param length       视野长度
     * @return 视野内的生物
     */
    
    public static List<LivingEntity> getEntityLookingAt(LivingEntity livingEntity, int length) {
        Set<Chunk> chunkSet = new HashSet<>();
        BlockIterator iterator = new BlockIterator(livingEntity.getEyeLocation(), 0, length);
        while (iterator.hasNext()) {
            chunkSet.add(iterator.next().getChunk());
        }
        List<LivingEntity> entityList = new ArrayList<>();
        for (Chunk chunk : chunkSet) {
            for (Entity entity : chunk.getEntities()) {
                if (entity instanceof LivingEntity && livingEntity.hasLineOfSight(entity)) {
                    entityList.add((LivingEntity) entity);
                }
            }
        }
        return entityList;
    }
}


