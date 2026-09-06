package pers.tany.yukinoaapi.hologram.impl;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.scheduler.BukkitRunnable;
import pers.tany.yukinoaapi.Main;
import pers.tany.yukinoaapi.hologram.IHologram;
import pers.tany.yukinoaapi.hologram.RainbowText;
import pers.tany.yukinoaapi.util.LogUtil;
import pers.tany.yukinoaapi.util.NMSUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 原生全息图实现（1.12+）
 * 使用盔甲架显示文本
 *
 * @author Roinflam
 * @since 3.1.0
 */
public class NativeHologram implements IHologram {

    /** 全息图ID */
    private final String id;

    /** 基础位置（中心点）*/
    private Location location;

    /** 文本行列表（原始文本，包含 &u 等标记）*/
    private final List<String> lines;

    /** 盔甲架实体列表 */
    private final List<ArmorStand> armorStands;

    /** 彩虹文本处理器映射 */
    private final Map<Integer, RainbowText> rainbowProcessors;

    /** 彩虹更新任务 */
    private BukkitRunnable rainbowTask;

    /** 是否已显示 */
    private boolean shown;

    /** 是否已删除 */
    private boolean deleted;

    /** 全息图来源（"command" / "plugin" / "unknown"）*/
    private String source;

    /** 行间距（格）*/
    private static final double LINE_HEIGHT = 0.25;

    /** 全息图盔甲架标记前缀 */
    private static final String HOLOGRAM_TAG = "YukiNoaAPI_Hologram";

    /**
     * 构造函数
     *
     * @param id       全息图唯一标识
     * @param location 全息图位置
     */
    public NativeHologram(String id, Location location) {
        this.id = id;
        this.location = location.clone();
        this.lines = new ArrayList<>();
        this.armorStands = new ArrayList<>();
        this.rainbowProcessors = new HashMap<>();
        this.shown = false;
        this.deleted = false;
        this.source = "unknown";
    }

    /**
     * 清理指定区块中的孤儿盔甲架
     * 【关键方法】在区块加载时调用，清理该区块内残留的旧盔甲架
     *
     * @param chunk    要清理的区块
     * @param logClean 是否输出清理日志
     * @return 清理的盔甲架数量
     */
    public static int cleanupOrphanedStandsInChunk(Chunk chunk, boolean logClean) {
        int cleanedCount = 0;

        // 使用 chunk.getEntities() 获取该区块内的所有实体
        for (org.bukkit.entity.Entity entity : chunk.getEntities()) {
            if (entity instanceof ArmorStand) {
                ArmorStand stand = (ArmorStand) entity;
                // 检查是否是本插件创建的全息图盔甲架
                if (stand.getScoreboardTags().contains(HOLOGRAM_TAG)) {
                    // 强制移除：先取消无敌状态，确保能被移除
                    try {
                        stand.setInvulnerable(false);
                    } catch (NoSuchMethodError ignored) {
                        // 低版本没有这个方法
                    }
                    stand.remove();
                    cleanedCount++;
                }
            }
        }

        // 输出清理日志
        if (cleanedCount > 0 && logClean) {
            LogUtil.debug(((Main) Main.plugin).getLanguageManager().getMessage(
                    "hologram.cleanup-chunk",
                    String.valueOf(cleanedCount),
                    String.valueOf(chunk.getX()),
                    String.valueOf(chunk.getZ())
            ));
        }

        return cleanedCount;
    }

    /**
     * 清理所有已加载区块中的孤儿盔甲架
     * 仅在服务器启动时调用，清理已加载区块
     * 未加载区块的盔甲架会在区块加载时由 cleanupOrphanedStandsInChunk 清理
     */
    public static void cleanupAllOrphanedStands() {
        // 输出开始清理日志
        LogUtil.info(((Main) Main.plugin).getLanguageManager().getMessage(
                "hologram.cleanup-start"
        ));

        int cleanedCount = 0;

        for (World world : Bukkit.getWorlds()) {
            // 遍历所有已加载的区块
            for (Chunk chunk : world.getLoadedChunks()) {
                // 不输出每个区块的日志，避免刷屏
                cleanedCount += cleanupOrphanedStandsInChunk(chunk, false);
            }
        }

        // 输出总清理数量日志
        if (cleanedCount > 0) {
            LogUtil.info(((Main) Main.plugin).getLanguageManager().getMessage(
                    "hologram.cleanup-all",
                    String.valueOf(cleanedCount)
            ));
        } else {
            LogUtil.info(((Main) Main.plugin).getLanguageManager().getMessage(
                    "hologram.cleanup-none"
            ));
        }
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Location getLocation() {
        return location.clone();
    }

    @Override
    public void teleport(Location location) {
        if (deleted) {
            LogUtil.warning(((Main) Main.plugin).getLanguageManager().getMessage(
                    "hologram.teleport-deleted",
                    id
            ));
            return;
        }

        this.location = location.clone();

        if (shown) {
            updateAllPositions();
        }
    }

    @Override
    public List<String> getLines() {
        return new ArrayList<>(lines);
    }

    @Override
    public void setLine(int index, String text) {
        if (deleted) return;

        if (index < 0 || index >= lines.size()) {
            LogUtil.warning(((Main) Main.plugin).getLanguageManager().getMessage(
                    "hologram.index-out-of-bounds",
                    String.valueOf(index)
            ));
            return;
        }

        lines.set(index, text);
        updateRainbowProcessor(index, text);

        if (shown && index < armorStands.size()) {
            updateArmorStandText(index);
        }
    }

    @Override
    public void appendLine(String text) {
        if (deleted) return;

        int index = lines.size();
        lines.add(text);
        updateRainbowProcessor(index, text);

        if (shown) {
            updateAllPositions();
            spawnArmorStand(index, text);
        }

        checkRainbowTask();
    }

    @Override
    public void insertLine(int index, String text) {
        if (deleted) return;

        if (index < 0 || index > lines.size()) {
            LogUtil.warning(((Main) Main.plugin).getLanguageManager().getMessage(
                    "hologram.index-out-of-bounds",
                    String.valueOf(index)
            ));
            return;
        }

        lines.add(index, text);

        if (shown) {
            hide();
            show();
        }

        rebuildRainbowProcessors();
        checkRainbowTask();
    }

    @Override
    public void removeLine(int index) {
        if (deleted) return;

        if (index < 0 || index >= lines.size()) {
            LogUtil.warning(((Main) Main.plugin).getLanguageManager().getMessage(
                    "hologram.index-out-of-bounds",
                    String.valueOf(index)
            ));
            return;
        }

        lines.remove(index);

        if (shown) {
            hide();
            show();
        }

        rebuildRainbowProcessors();
        checkRainbowTask();
    }

    @Override
    public void clearLines() {
        if (deleted) return;

        lines.clear();
        rainbowProcessors.clear();

        if (shown) {
            hide();
        }

        stopRainbowTask();
    }

    @Override
    public int size() {
        return lines.size();
    }

    @Override
    public void show() {
        if (deleted || shown) return;

        if (location.getWorld() == null) {
            LogUtil.warning(((Main) Main.plugin).getLanguageManager().getMessage(
                    "hologram.world-null",
                    id
            ));
            return;
        }

        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(Main.plugin, this::show);
            return;
        }

        // 【关键修复】区块未加载时不生成盔甲架，避免强制加载区块。
        // 原因：world.spawnEntity() 在未加载区块会强制加载该区块并生成实体，
        //   这会导致两类问题：
        //   1. 启动时 loadHolograms() 对所有全息图调用 show()，会一次性强制加载
        //      全部全息图所在区块，造成启动卡顿且这些区块被长期占用。
        //   2. 此时启动清理 cleanupAllOrphanedStands() 早已执行完（且当时这些区块
        //      尚未加载，清不到），新盔甲架会直接叠加在上一次会话残留的旧孤儿之上，
        //      重启多次后越堆越多。
        // 处理：未加载时直接返回，shown 保持 false，待区块加载事件（onChunkLoad）
        //   在区块就绪后重新调用 show() 完成显示与孤儿清理。
        int chunkX = location.getBlockX() >> 4;
        int chunkZ = location.getBlockZ() >> 4;
        if (!location.getWorld().isChunkLoaded(chunkX, chunkZ)) {
            return;
        }

        // 【关键修复】生成前先清理本全息图遗留在区块内的旧盔甲架（携带本全息图 id 标记），
        // 确保「show() 执行后，本全息图在区块内有且仅有当前这一组盔甲架」这一不变式，
        // 从根源杜绝服务器重启 / 重复显示导致的盔甲架堆叠。
        removeOwnOrphanStands();

        for (int i = 0; i < lines.size(); i++) {
            spawnArmorStand(i, lines.get(i));
        }

        shown = true;
        checkRainbowTask();
    }

    @Override
    public void hide() {
        if (!shown) return;

        stopRainbowTask();

        for (ArmorStand stand : armorStands) {
            if (stand != null && !stand.isDead()) {
                stand.remove();
            }
        }

        armorStands.clear();
        shown = false;
    }

    @Override
    public boolean isShown() {
        return shown;
    }

    @Override
    public void delete() {
        if (deleted) return;
        hide();
        deleted = true;
    }

    @Override
    public boolean isDeleted() {
        return deleted;
    }

    @Override
    public String getSource() {
        return source;
    }

    @Override
    public void setSource(String source) {
        this.source = source != null ? source : "unknown";
    }

    /**
     * 检查全息图实体是否仍然有效
     * 遍历所有盔甲架，若有任何一个死亡或为null则返回false
     *
     * @return 所有实体存活返回true
     */
    @Override
    public boolean isValid() {
        // 未显示或已删除的全息图不参与有效性检查
        if (!shown || deleted) return true;

        // 无行时视为有效
        if (lines.isEmpty()) return true;

        // 盔甲架数量与行数不匹配，说明有问题
        if (armorStands.size() != lines.size()) return false;

        // 检查每个盔甲架是否存活
        for (ArmorStand stand : armorStands) {
            if (stand == null || stand.isDead() || !stand.isValid()) {
                return false;
            }
        }

        return true;
    }

    /**
     * 刷新全息图：隐藏后重新显示
     * 用于修复被外部移除的盔甲架实体
     */
    @Override
    public void refresh() {
        if (deleted) return;

        // 记录当前显示状态
        boolean wasShown = shown;

        // 强制隐藏（清理残余实体）
        hide();

        // 如果之前是显示状态，重新显示
        if (wasShown) {
            show();
        }
    }

    /**
     * 计算居中后的Y坐标
     *
     * @param index 行索引
     * @return 居中后的Y坐标
     */
    private double calculateCenteredY(int index) {
        int totalLines = lines.size();
        double halfHeight = (totalLines - 1) * LINE_HEIGHT / 2.0;
        return location.getY() + halfHeight - (index * LINE_HEIGHT);
    }

    /**
     * 更新所有盔甲架的位置
     */
    private void updateAllPositions() {
        if (!shown || location.getWorld() == null) return;

        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(Main.plugin, this::updateAllPositions);
            return;
        }

        for (int i = 0; i < armorStands.size(); i++) {
            ArmorStand stand = armorStands.get(i);
            if (stand != null && !stand.isDead()) {
                double centeredY = calculateCenteredY(i);
                Location newLoc = new Location(
                        location.getWorld(),
                        location.getX(),
                        centeredY,
                        location.getZ()
                );
                stand.teleport(newLoc);
            }
        }
    }

    /**
     * 清理本全息图遗留在所在区块内的旧盔甲架（携带本全息图专属 id 标记 "Hologram_&lt;id&gt;"）
     *
     * 必要性说明（修复盔甲架重复堆叠的核心）：
     *   盔甲架以世界实体形式持久化保存在区块数据中。服务器重启 / 崩溃 / 区块卸载时机异常
     *   等情况下，上一次会话生成的盔甲架可能残留在世界里，但已不再被任何全息图对象引用，
     *   成为孤儿。仅依赖启动时的 cleanupAllOrphanedStands() 无法覆盖「启动时未加载、
     *   运行中才加载」的区块，导致这些区块每次重启都在旧孤儿之上叠加新盔甲架，越堆越多。
     *
     *   本方法在每次 show() 生成盔甲架前调用，按本全息图专属 id 标记精确清理，保证
     *   「show() 执行后，本全息图在区块内有且仅有当前这一组盔甲架」这一不变式。
     *
     * 性能与安全：
     *   仅扫描本全息图所在的单个区块，且区块已加载时才执行（调用方 show() 已先行确认
     *   区块已加载），不会强制加载区块。
     *   注意：在 ChunkLoadEvent 回调内调用 show() 时，必须延迟 1 tick，避免在事件回调内
     *   直接调用 chunk.getEntities() 触发 Mohist 主线程 park 阻塞（HologramManager
     *   的 onChunkLoad 已做此延迟处理）。
     */
    private void removeOwnOrphanStands() {
        World world = location.getWorld();
        if (world == null) return;

        int chunkX = location.getBlockX() >> 4;
        int chunkZ = location.getBlockZ() >> 4;
        if (!world.isChunkLoaded(chunkX, chunkZ)) return;

        // 本全息图专属标记：与 spawnArmorStand 中 addScoreboardTag("Hologram_" + id) 对应
        String selfTag = "Hologram_" + id;

        for (org.bukkit.entity.Entity entity : world.getChunkAt(chunkX, chunkZ).getEntities()) {
            if (!(entity instanceof ArmorStand)) continue;
            if (!entity.getScoreboardTags().contains(selfTag)) continue;

            // 强制移除：先取消无敌状态，确保能被移除
            try {
                ((ArmorStand) entity).setInvulnerable(false);
            } catch (NoSuchMethodError ignored) {
                // 低版本没有这个方法
            }
            entity.remove();
        }
    }

    /**
     * 生成盔甲架（在临时位置生成，设置好属性后再传送到目标位置）
     *
     * @param index 行索引
     * @param text  文本内容
     */
    private void spawnArmorStand(int index, String text) {
        if (location.getWorld() == null) return;

        // 1. 先在临时位置生成（使用虚空位置，玩家看不到）
        Location tempLoc = new Location(
                location.getWorld(),
                location.getX(),
                -100,  // 使用虚空位置，避免玩家看到盔甲架闪现
                location.getZ()
        );

        ArmorStand stand = (ArmorStand) location.getWorld().spawnEntity(
                tempLoc,
                EntityType.ARMOR_STAND
        );

        // 2. 立即设置所有属性
        stand.setGravity(false);
        stand.setVisible(false);
        stand.setBasePlate(false);
        stand.setArms(false);
        stand.setRemoveWhenFarAway(false);

        stand.addScoreboardTag(HOLOGRAM_TAG);
        stand.addScoreboardTag("Hologram_" + id);

        if (NMSUtil.isVersionAtLeast(8)) {
            try {
                stand.setMarker(true);
            } catch (NoSuchMethodError ignored) {
            }
        }

        if (NMSUtil.isVersionAtLeast(9)) {
            try {
                stand.setInvulnerable(true);
            } catch (NoSuchMethodError ignored) {
            }
        }

        if (NMSUtil.isVersionAtLeast(10)) {
            try {
                stand.setAI(false);
            } catch (NoSuchMethodError ignored) {
            }
        }

        if (NMSUtil.isVersionAtLeast(12)) {
            try {
                stand.setSilent(true);
            } catch (NoSuchMethodError ignored) {
            }
        }

        // 3. 设置文本
        updateArmorStandText(stand, index);

        // 4. 添加到列表
        while (armorStands.size() <= index) {
            armorStands.add(null);
        }
        armorStands.set(index, stand);

        // 5. 最后传送到目标位置（此时属性已全部设置好）
        double centeredY = calculateCenteredY(index);
        Location targetLoc = new Location(
                location.getWorld(),
                location.getX(),
                centeredY,
                location.getZ()
        );
        stand.teleport(targetLoc);
    }

    /**
     * 更新指定盔甲架的文本
     *
     * @param index 行索引
     */
    private void updateArmorStandText(int index) {
        if (index < 0 || index >= armorStands.size()) return;

        ArmorStand stand = armorStands.get(index);
        if (stand == null || stand.isDead()) return;

        updateArmorStandText(stand, index);
    }

    /**
     * 更新盔甲架的文本显示
     *
     * @param stand 盔甲架实体
     * @param index 行索引
     */
    private void updateArmorStandText(ArmorStand stand, int index) {
        if (index >= lines.size()) return;

        String text = lines.get(index);
        String displayText;

        if (RainbowText.hasRainbow(text)) {
            RainbowText processor = rainbowProcessors.get(index);
            if (processor != null) {
                displayText = processor.applyRainbow(text);
            } else {
                displayText = text.replace("&u", "§c");
            }
        } else {
            displayText = text.replace("&", "§");
        }

        stand.setCustomName(displayText);
        stand.setCustomNameVisible(displayText != null && !displayText.isEmpty());
    }

    /**
     * 更新彩虹处理器
     *
     * @param index 行索引
     * @param text  文本内容
     */
    private void updateRainbowProcessor(int index, String text) {
        if (RainbowText.hasRainbow(text)) {
            if (!rainbowProcessors.containsKey(index)) {
                rainbowProcessors.put(index, new RainbowText());
            }
        } else {
            rainbowProcessors.remove(index);
        }
    }

    /**
     * 重建所有彩虹处理器
     */
    private void rebuildRainbowProcessors() {
        rainbowProcessors.clear();
        for (int i = 0; i < lines.size(); i++) {
            updateRainbowProcessor(i, lines.get(i));
        }
    }

    /**
     * 检查是否需要启动或停止彩虹任务
     */
    private void checkRainbowTask() {
        if (!rainbowProcessors.isEmpty() && rainbowTask == null && shown) {
            startRainbowTask();
        } else if (rainbowProcessors.isEmpty() && rainbowTask != null) {
            stopRainbowTask();
        }
    }

    /**
     * 启动彩虹任务
     */
    private void startRainbowTask() {
        if (rainbowTask != null) return;

        rainbowTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (deleted || !shown) {
                    cancel();
                    return;
                }

                for (RainbowText processor : rainbowProcessors.values()) {
                    processor.tick();
                }

                for (int index : rainbowProcessors.keySet()) {
                    updateArmorStandText(index);
                }
            }
        };

        rainbowTask.runTaskTimer(Main.plugin, 10L, 10L);
    }

    /**
     * 停止彩虹任务
     */
    private void stopRainbowTask() {
        if (rainbowTask != null) {
            rainbowTask.cancel();
            rainbowTask = null;
        }
    }
}