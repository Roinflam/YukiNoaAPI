package pers.tany.yukinoaapi.hologram.impl;

import org.bukkit.Bukkit;
import org.bukkit.Location;
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

    /** 基础位置 */
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

    /** 行间距（格） */
    private static final double LINE_HEIGHT = 0.25;

    /**
     * 构造函数
     */
    public NativeHologram(String id, Location location) {
        this.id = id;
        this.location = location.clone();
        this.lines = new ArrayList<>();
        this.armorStands = new ArrayList<>();
        this.rainbowProcessors = new HashMap<>();
        this.shown = false;
        this.deleted = false;
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
            LogUtil.warning("尝试传送已删除的全息图: " + id);
            return;
        }

        this.location = location.clone();

        if (shown) {
            // 重新生成所有盔甲架
            hide();
            show();
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
            LogUtil.warning("行索引越界: " + index);
            return;
        }

        lines.set(index, text);

        // 更新彩虹处理器
        updateRainbowProcessor(index, text);

        // 更新对应的盔甲架
        if (shown && index < armorStands.size()) {
            updateArmorStandText(index);
        }
    }

    @Override
    public void appendLine(String text) {
        if (deleted) return;

        int index = lines.size();
        lines.add(text);

        // 更新彩虹处理器
        updateRainbowProcessor(index, text);

        if (shown) {
            // 创建新的盔甲架
            spawnArmorStand(index, text);
        }

        // 启动彩虹任务（如果需要）
        checkRainbowTask();
    }

    @Override
    public void insertLine(int index, String text) {
        if (deleted) return;

        if (index < 0 || index > lines.size()) {
            LogUtil.warning("行索引越界: " + index);
            return;
        }

        lines.add(index, text);

        if (shown) {
            // 重新生成所有盔甲架
            hide();
            show();
        }

        // 重建彩虹处理器
        rebuildRainbowProcessors();
        checkRainbowTask();
    }

    @Override
    public void removeLine(int index) {
        if (deleted) return;

        if (index < 0 || index >= lines.size()) {
            LogUtil.warning("行索引越界: " + index);
            return;
        }

        lines.remove(index);

        if (shown) {
            // 重新生成所有盔甲架
            hide();
            show();
        }

        // 重建彩虹处理器
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
            LogUtil.warning("全息图世界为空: " + id);
            return;
        }

        // 确保在主线程中执行
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(Main.plugin, this::show);
            return;
        }

        // 生成所有盔甲架
        for (int i = 0; i < lines.size(); i++) {
            spawnArmorStand(i, lines.get(i));
        }

        shown = true;

        // 启动彩虹更新任务
        checkRainbowTask();
    }

    @Override
    public void hide() {
        if (!shown) return;

        // 停止彩虹任务
        stopRainbowTask();

        // 移除所有盔甲架
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

    /**
     * 生成盔甲架
     */
    private void spawnArmorStand(int index, String text) {
        if (location.getWorld() == null) return;

        // 计算Y坐标（从上往下）
        double y = location.getY() - (index * LINE_HEIGHT);
        Location spawnLoc = new Location(
                location.getWorld(),
                location.getX(),
                y,
                location.getZ()
        );

        // 在主线程中生成
        ArmorStand stand = (ArmorStand) location.getWorld().spawnEntity(
                spawnLoc,
                EntityType.ARMOR_STAND
        );

        // 先设置所有基础属性（避免闪现）
        stand.setGravity(false);
        stand.setVisible(false);        // 隐形
        stand.setSmall(true);            // 小型
        stand.setBasePlate(false);       // 无底板
        stand.setArms(false);            // 无手臂
        stand.setRemoveWhenFarAway(false); // 不会因距离远而移除

        // 1.8.3+ 设置为标记（无碰撞箱）
        if (NMSUtil.isVersionAtLeast(8)) {
            try {
                stand.setMarker(true);
            } catch (NoSuchMethodError ignored) {
            }
        }

        // 1.9+ 设置无敌
        if (NMSUtil.isVersionAtLeast(9)) {
            try {
                stand.setInvulnerable(true);
            } catch (NoSuchMethodError ignored) {
            }
        }

        // 1.10+ 禁用AI
        if (NMSUtil.isVersionAtLeast(10)) {
            try {
                stand.setAI(false);
            } catch (NoSuchMethodError ignored) {
            }
        }

        // 1.12+ 设置沉默（不发出声音）
        if (NMSUtil.isVersionAtLeast(12)) {
            try {
                stand.setSilent(true);
            } catch (NoSuchMethodError ignored) {
            }
        }

        // 延迟1 tick设置文本（避免闪现）
        final int finalIndex = index;
        Bukkit.getScheduler().runTaskLater(Main.plugin, () -> {
            if (!stand.isDead()) {
                updateArmorStandText(stand, finalIndex);
            }
        }, 1L);

        // 确保索引正确
        while (armorStands.size() <= index) {
            armorStands.add(null);
        }
        armorStands.set(index, stand);
    }

    /**
     * 更新盔甲架文本
     */
    private void updateArmorStandText(int index) {
        if (index < 0 || index >= armorStands.size()) return;

        ArmorStand stand = armorStands.get(index);
        if (stand == null || stand.isDead()) return;

        updateArmorStandText(stand, index);
    }

    /**
     * 更新盔甲架文本
     */
    private void updateArmorStandText(ArmorStand stand, int index) {
        String text = lines.get(index);
        String displayText;

        // 处理彩虹文本
        if (RainbowText.hasRainbow(text)) {
            RainbowText processor = rainbowProcessors.get(index);
            if (processor != null) {
                displayText = processor.applyRainbow(text);
            } else {
                displayText = text.replace("&u", "§c"); // 默认红色
            }
        } else {
            displayText = text.replace("&", "§");
        }

        stand.setCustomName(displayText);
        stand.setCustomNameVisible(displayText != null && !displayText.isEmpty());
    }

    /**
     * 更新彩虹处理器
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
     * 检查并启动彩虹任务
     */
    private void checkRainbowTask() {
        if (!rainbowProcessors.isEmpty() && rainbowTask == null && shown) {
            startRainbowTask();
        } else if (rainbowProcessors.isEmpty() && rainbowTask != null) {
            stopRainbowTask();
        }
    }

    /**
     * 启动彩虹更新任务
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

                // 更新所有彩虹处理器
                for (RainbowText processor : rainbowProcessors.values()) {
                    processor.tick();
                }

                // 更新所有使用彩虹的盔甲架
                for (int index : rainbowProcessors.keySet()) {
                    updateArmorStandText(index);
                }
            }
        };

        // 每 10 tick 更新一次（0.5秒，降低变化速度）
        rainbowTask.runTaskTimer(Main.plugin, 10L, 10L);
    }

    /**
     * 停止彩虹更新任务
     */
    private void stopRainbowTask() {
        if (rainbowTask != null) {
            rainbowTask.cancel();
            rainbowTask = null;
        }
    }
}