// 文件：HologramManager.java
// 路径：src/main/java/pers/tany/yukinoaapi/hologram/HologramManager.java
package pers.tany.yukinoaapi.hologram;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import pers.tany.yukinoaapi.Main;
import pers.tany.yukinoaapi.hologram.impl.HolographicDisplaysHologram;
import pers.tany.yukinoaapi.hologram.impl.NativeHologram;
import pers.tany.yukinoaapi.util.LogUtil;
import pers.tany.yukinoaapi.util.NMSUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 全息图管理器
 * 负责创建、管理、持久化全息图
 *
 * @author Roinflam
 * @since 3.1.0
 */
public class HologramManager implements Listener {

    /** 单例 */
    private static HologramManager instance;

    /** 插件实例 */
    private final Plugin plugin;

    /** 全息图映射 */
    private final Map<String, IHologram> holograms;

    /** 是否使用HD插件 */
    private final boolean useHD;

    /** 数据管理器 */
    private HologramDataManager dataManager;

    /** 是否已加载完成 */
    private boolean loaded = false;

    /**
     * 私有构造函数
     *
     * @param plugin 插件实例
     */
    private HologramManager(Plugin plugin) {
        this.plugin = plugin;
        this.holograms = new ConcurrentHashMap<>();

        // 检测是否使用 HolographicDisplays
        this.useHD = !NMSUtil.isVersionAtLeast(12) &&
                Bukkit.getPluginManager().getPlugin("HolographicDisplays") != null;

        // 初始化数据管理器
        this.dataManager = new HologramDataManager((Main) plugin);

        // 注册事件
        Bukkit.getPluginManager().registerEvents(this, plugin);

        LogUtil.info(((Main) Main.plugin).getLanguageManager().getMessage(
                "hologram.mode",
                useHD ? "HolographicDisplays" : "Native (1.12+)"
        ));

        // 延迟加载全息图（等待所有世界加载完成）
        new BukkitRunnable() {
            @Override
            public void run() {
                loadHolograms();
                loaded = true;
            }
        }.runTaskLater(plugin, 20L); // 延迟1秒（20 tick）
    }

    /**
     * 获取单例
     *
     * @return HologramManager 实例
     */
    public static HologramManager getInstance() {
        if (instance == null) {
            instance = new HologramManager(Main.plugin);
        }
        return instance;
    }

    /**
     * 创建全息图
     *
     * @param id       全息图唯一标识
     * @param location 全息图位置
     * @return 创建的全息图对象
     */
    public IHologram createHologram(String id, Location location) {
        return createHologram(id, location, null);
    }

    /**
     * 创建全息图（带初始文本）
     *
     * @param id          全息图唯一标识
     * @param location    全息图位置
     * @param initialText 初始文本（可为null）
     * @return 创建的全息图对象
     */
    public IHologram createHologram(String id, Location location, String initialText) {
        if (holograms.containsKey(id)) {
            LogUtil.warning(((Main) Main.plugin).getLanguageManager().getMessage(
                    "hologram.id-exists",
                    id
            ));
            return holograms.get(id);
        }

        IHologram hologram;

        if (useHD) {
            hologram = new HolographicDisplaysHologram(id, location, plugin);
        } else {
            hologram = new NativeHologram(id, location);
        }

        // 添加初始文本
        if (initialText != null && !initialText.isEmpty()) {
            hologram.appendLine(initialText);
        }

        // 显示全息图
        hologram.show();

        holograms.put(id, hologram);
        saveHolograms();

        return hologram;
    }

    /**
     * 获取全息图
     *
     * @param id 全息图唯一标识
     * @return 全息图对象，不存在则返回null
     */
    public IHologram getHologram(String id) {
        return holograms.get(id);
    }

    /**
     * 删除全息图
     *
     * @param id 全息图唯一标识
     */
    public void deleteHologram(String id) {
        IHologram hologram = holograms.remove(id);
        if (hologram != null) {
            hologram.delete();
            dataManager.deleteHologram(id);
        }
    }

    /**
     * 获取所有全息图
     *
     * @return 所有全息图的集合
     */
    public Collection<IHologram> getAllHolograms() {
        return new ArrayList<>(holograms.values());
    }

    /**
     * 保存所有全息图
     */
    public void saveHolograms() {
        dataManager.saveAll(holograms);
    }

    /**
     * 加载所有全息图
     */
    private void loadHolograms() {
        // 在原生模式下，先清理上次崩溃/异常关闭遗留的孤儿盔甲架
        // HolographicDisplays 会自动处理，不需要清理
        if (!useHD) {
            NativeHologram.cleanupAllOrphanedStands();
        }

        Map<String, HologramDataManager.HologramData> dataMap = dataManager.loadAllHolograms();

        if (dataMap.isEmpty()) {
            LogUtil.info(((Main) Main.plugin).getLanguageManager().getMessage(
                    "hologram.no-data"
            ));
            return;
        }

        int successCount = 0;

        for (HologramDataManager.HologramData data : dataMap.values()) {
            try {
                // 检查是否已存在（避免重复加载）
                if (holograms.containsKey(data.getId())) {
                    LogUtil.warning(((Main) Main.plugin).getLanguageManager().getMessage(
                            "hologram.skip-exists",
                            data.getId()
                    ));
                    continue;
                }

                // 创建全息图对象
                IHologram hologram;
                if (useHD) {
                    hologram = new HolographicDisplaysHologram(data.getId(), data.getLocation(), plugin);
                } else {
                    hologram = new NativeHologram(data.getId(), data.getLocation());
                }

                // 添加所有行（此时 shown=false，不会生成盔甲架）
                for (String line : data.getLines()) {
                    hologram.appendLine(line);
                }

                // 显示全息图（一次性生成所有盔甲架）
                hologram.show();

                // 添加到管理器
                holograms.put(data.getId(), hologram);

                successCount++;
                LogUtil.debug(((Main) Main.plugin).getLanguageManager().getMessage(
                        "hologram.load-single",
                        data.getId(),
                        String.valueOf(data.getLines().size())
                ));
            } catch (Exception e) {
                LogUtil.error(((Main) Main.plugin).getLanguageManager().getMessage(
                        "hologram.load-fail",
                        data.getId()
                ), e);
            }
        }

        LogUtil.info(((Main) Main.plugin).getLanguageManager().getMessage(
                "hologram.loaded",
                String.valueOf(successCount)
        ));
    }

    /**
     * 区块加载事件
     * 【关键】先清理该区块中残留的旧盔甲架，再显示新全息图
     *
     * @param event 区块加载事件
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onChunkLoad(ChunkLoadEvent event) {
        // 原生实现需要处理
        if (!useHD && loaded) {
            // 【核心修复】先清理该区块中可能残留的旧盔甲架
            // 这是解决盔甲架越来越多问题的关键
            // 第二个参数为 true，表示输出清理日志
            NativeHologram.cleanupOrphanedStandsInChunk(event.getChunk(), true);

            // 再显示该区块内的全息图
            int chunkX = event.getChunk().getX();
            int chunkZ = event.getChunk().getZ();

            for (IHologram hologram : holograms.values()) {
                Location loc = hologram.getLocation();
                if (loc.getWorld() != null &&
                        loc.getWorld().equals(event.getWorld()) &&
                        isInChunk(loc, chunkX, chunkZ) &&
                        !hologram.isShown()) {
                    hologram.show();
                }
            }
        }
    }

    /**
     * 区块卸载事件
     *
     * @param event 区块卸载事件
     */
    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        // 原生实现需要隐藏全息图
        if (!useHD && loaded) {
            int chunkX = event.getChunk().getX();
            int chunkZ = event.getChunk().getZ();

            for (IHologram hologram : holograms.values()) {
                Location loc = hologram.getLocation();
                if (loc.getWorld() != null &&
                        loc.getWorld().equals(event.getWorld()) &&
                        isInChunk(loc, chunkX, chunkZ) &&
                        hologram.isShown()) {
                    hologram.hide();
                }
            }
        }
    }

    /**
     * 检查位置是否在指定区块内
     *
     * @param location 位置
     * @param chunkX   区块X坐标
     * @param chunkZ   区块Z坐标
     * @return 是否在区块内
     */
    private boolean isInChunk(Location location, int chunkX, int chunkZ) {
        int locChunkX = location.getBlockX() >> 4;
        int locChunkZ = location.getBlockZ() >> 4;
        return locChunkX == chunkX && locChunkZ == chunkZ;
    }

    /**
     * 关闭管理器
     */
    public void shutdown() {
        saveHolograms();

        for (IHologram hologram : holograms.values()) {
            hologram.hide();
        }

        holograms.clear();
        loaded = false;
    }

    /**
     * 是否使用 HD 插件
     *
     * @return true 表示使用 HolographicDisplays 插件
     */
    public boolean isUsingHD() {
        return useHD;
    }

    /**
     * 重载数据
     */
    public void reload() {
        // 隐藏并清空现有全息图
        for (IHologram hologram : holograms.values()) {
            hologram.hide();
        }
        holograms.clear();

        // 原生模式下重新清理孤儿盔甲架
        if (!useHD) {
            NativeHologram.cleanupAllOrphanedStands();
        }

        // 重新加载数据
        dataManager.reload();
        loadHolograms();

        LogUtil.info(((Main) Main.plugin).getLanguageManager().getMessage(
                "hologram.reloaded",
                String.valueOf(holograms.size())
        ));
    }
}