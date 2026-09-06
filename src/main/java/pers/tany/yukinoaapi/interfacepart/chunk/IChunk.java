package pers.tany.yukinoaapi.interfacepart.chunk;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public interface IChunk {

    /**
     * 根据坐标，获取周围区块的数组
     *
     * @param location 涉及的坐标
     * @param range    周围范围(1为3x3)
     * @return 区块列表
     */
    
    static List<Chunk> getChunks(Location location, int range) {
        List<Chunk> chunks = new ArrayList<>();
        for (int x = -range; x <= range; x++) {
            for (int z = -range; z <= range; z++) {
                Location l = location.clone();
                Chunk chunk = location.getChunk();
                l.setX(chunk.getX() * 16 - 16 * x);
                l.setZ(chunk.getZ() * 16 - 16 * z);
                chunks.add(l.getChunk());
            }
        }
        return chunks;
    }

    /**
     * 根据坐标，获取周围区块的数组，然后查看是否含有玩家
     *
     * @param location 涉及的坐标
     * @param range    周围范围(0为1x1,1为3x3,2为5x5)
     * @return 区块组里是否有玩家
     */
    static boolean chunksHasPlayer(Location location, int range) {
        List<Integer> xList = new ArrayList<>();
        List<Integer> zList = new ArrayList<>();
        for (int x = -range; x <= range; x++) {
            xList.add(x);
        }
        for (int z = -range; z <= range; z++) {
            zList.add(z);
        }
        boolean[] has = new boolean[]{false};
        xList.parallelStream().forEach(x -> zList.parallelStream().forEach(z -> {
            Location l = location.clone();
            Chunk chunk = location.getChunk();
            l.setX(chunk.getX() * 16 - 16 * x);
            l.setZ(chunk.getZ() * 16 - 16 * z);
            for (Entity entity : l.getChunk().getEntities()) {
                if (entity instanceof Player) {
                    has[0] = true;
                    break;
                }
            }
        }));
        return has[0];
    }
}

