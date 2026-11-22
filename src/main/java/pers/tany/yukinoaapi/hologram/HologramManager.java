package pers.tany.yukinoaapi.hologram;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import pers.tany.yukinoaapi.Main;
import pers.tany.yukinoaapi.hologram.impl.HolographicDisplaysHologram;
import pers.tany.yukinoaapi.hologram.impl.NativeHologram;
import pers.tany.yukinoaapi.util.LogUtil;
import pers.tany.yukinoaapi.util.NMSUtil;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 全息图管理器
 * 负责创建、管理、持久化全息图
 *
 * 版本兼容策略：
 *   1.12+          → 原生实现（ArmorStand + ScoreboardTag）
 *   1.7-1.11 + HD  → HolographicDisplays 实现
 *   1.7-1.11 无 HD → 全息图系统禁用，输出警告
 *
 * 性能优化（v3.2.1）：
 *   createHologram / deleteHologram 不再即时调用 config.save()，
 *   改为标记 dirty，由定时任务（每 SAVE_INTERVAL tick）统一调 saveAll() 写盘一次。
 *   批量操作场景（区块加载恢复 N 个全息图）从 N×(N+1) 次写盘降为 1 次。
 *   shutdown 时强制 flush 确保数据不丢。
 *
 * 性能优化（v3.2.2）：
 *   onChunkLoad / onChunkUnload 原实现遍历所有全息图检查坐标匹配，O(N)。
 *   新增区块索引 chunkIndex（chunkKey → hologram ID 集合），create/delete/load 时维护，
 *   chunk 事件通过索引 O(1) 查到候选集，无全息图的区块直接跳过。
 *
 * 性能优化（v3.2.3）：
 *   onChunkLoad 中 cleanupOrphanedStandsInChunk 改为延迟 1 tick 异步执行，
 *   避免在 ChunkLoadEvent 中同步调用 chunk.getEntities() 阻塞主线程
 *   （Mohist 环境下 CraftChunk.getEntities() 会等待区块实体数据就绪，导致 park）。
 *   新生成的区块（isNewChunk）直接跳过清理，不可能有孤儿盔甲架。
 *
 * 盔甲架重复堆叠修复（v3.2.4）：
 *   v3.2.3 完全移除了区块加载时的孤儿清理，但其「启动清理覆盖崩溃残留」的推断有漏洞：
 *   cleanupAllOrphanedStands() 只能清理「启动时已加载」的区块，覆盖不到「启动时未加载、
 *   运行中才加载」的区块，导致这些区块每次重启都在旧孤儿之上叠加新盔甲架，越堆越多。
 *
 *   修复方案改为在 NativeHologram.show() 内按本全息图专属 id 标记精确清理孤儿，
 *   并在 show() 中增加「区块未加载不生成」守卫（避免 spawnEntity 强制加载区块、
 *   且避免在旧孤儿之上叠加生成）。本类 onChunkLoad 因此将 show() 延迟 1 tick 执行：
 *   show() 内部会调用 chunk.getEntities() 清理孤儿，在 ChunkLoadEvent 回调内直接调用
 *   会触发 Mohist 主线程 park 阻塞，延迟 1 tick 待区块实体就绪后执行即可两全。
 *
 * chunkIndex 移动防护（v3.2.5）：
 *   IHologram.teleport() 只更新全息图自身持有的位置字段，不会同步更新本类
 *   维护的 chunkIndex。若外部直接调用 teleport() 将一个已存在的全息图移动到
 *   不同区块，chunkIndex 会与实际位置脱节：旧区块卸载时 onChunkUnload 仍按
 *   过期索引误触发 hide()；新区块从未登记，若之后自然卸载不会被本插件
 *   hide()，盔甲架会被 Minecraft 当作普通实体持久化进区块 NBT；该区块下次
 *   加载时 onChunkLoad 因索引缺失直接跳过，不会清理孤儿，导致野生盔甲架
 *   复活，且 isValid()/refresh() 只检查追踪列表内实体是否存活，无法发现
 *   世界中多余的同 tag 实体，现有自愈机制对此无效。
 *
 *   新增 moveHologram(id, newLocation) 作为外部移动已存在全息图位置的唯一
 *   正确入口，内部在 teleport 前后正确维护 chunkIndex（详见该方法注释）。
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

    /**
     * 全息图系统是否可用
     * 1.12+ 始终可用；1.7-1.11 需要 HolographicDisplays
     */
    private final boolean enabled;

    /** 数据管理器 */
    private HologramDataManager dataManager;

    /** 是否已加载完成 */
    private boolean loaded = false;

    /** 定时刷新任务（实体有效性检查） */
    private BukkitRunnable refreshTask;

    /** 刷新间隔（tick），10分钟 = 12000 tick */
    private static final long REFRESH_INTERVAL = 12000L;

    /** 文字展示实体不存盘，丢了不会自己回来，检查勤一些（30 秒） */
    private static final long REFRESH_INTERVAL_TEXT_DISPLAY = 600L;

    // ── 延迟保存机制 ──────────────────────────────────────────────────────

    /** 脏标记：有数据变更待写盘 */
    private volatile boolean dirty = false;

    /** 定时保存任务 */
    private BukkitRunnable saveTask;

    /** 保存间隔（tick），60 tick = 3 秒 */
    private static final long SAVE_INTERVAL = 60L;

    // ── 区块索引（chunk 事件 O(1) 查找） ──────────────────────────────────

    /**
     * 区块索引：chunkKey → 该区块内的全息图 ID 集合
     * chunkKey 格式："worldName:chunkX:chunkZ"
     * create/delete/load/move 时维护，chunk 事件通过此索引 O(1) 查到候选集
     */
    private final Map<String, Set<String>> chunkIndex = new HashMap<>();

    /**
     * 私有构造函数
     *
     * @param plugin 插件实例
     */
    private HologramManager(Plugin plugin) {
        this.plugin = plugin;
        this.holograms = new ConcurrentHashMap<>();

        // 检测 HolographicDisplays 是否存在
        boolean hdAvailable = Bukkit.getPluginManager().getPlugin("HolographicDisplays") != null;

        // ==================== 版本兼容策略 ====================
        // 1.12+：原生实现（ArmorStand + ScoreboardTag 均可用）
        // 1.7-1.11 + HD：使用 HolographicDisplays
        // 1.7-1.11 无 HD：禁用全息图系统
        if (NMSUtil.isVersionAtLeast(12)) {
            this.useHD = false;
            this.enabled = true;
        } else if (hdAvailable) {
            this.useHD = true;
            this.enabled = true;
        } else {
            this.useHD = false;
            this.enabled = false;
        }

        // 系统不可用时输出警告，跳过后续初始化
        if (!enabled) {
            LogUtil.warning(((Main) Main.plugin).getLanguageManager().getMessage(
                    "hologram.disabled-no-hd"
            ));
            return;
        }

        // 初始化数据管理器
        this.dataManager = new HologramDataManager((Main) plugin);

        // 注册事件
        Bukkit.getPluginManager().registerEvents(this, plugin);
        if (!useHD && NativeHologram.usesTextDisplay()) {
            registerEntitySectionEvents();
        }

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

                // 加载完毕后立即合并保存一次
                flushSave();

                // 启动定时刷新任务（实体有效性检查）
                startRefreshTask();

                // 启动定时保存任务（延迟写盘）
                startSaveTask();
            }
        }.runTaskLater(plugin, 20L);
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
     * 全息图系统是否可用
     * 1.7-1.11 未安装 HolographicDisplays 时返回 false
     *
     * @return true 表示可用
     */
    public boolean isEnabled() {
        return enabled;
    }

    // ══════════════════════════════════════════════════════════════════════
    // 创建 / 获取 / 删除 / 移动
    // ══════════════════════════════════════════════════════════════════════

    /**
     * 创建全息图（插件调用，来源标记为 "plugin"）
     *
     * @param id       全息图唯一标识
     * @param location 全息图位置
     * @return 创建的全息图对象，系统不可用时返回 null
     */
    public IHologram createHologram(String id, Location location) {
        return createHologram(id, location, null, "plugin");
    }

    /**
     * 创建全息图（带初始文本，插件调用，来源标记为 "plugin"）
     *
     * @param id          全息图唯一标识
     * @param location    全息图位置
     * @param initialText 初始文本（可为null）
     * @return 创建的全息图对象，系统不可用时返回 null
     */
    public IHologram createHologram(String id, Location location, String initialText) {
        return createHologram(id, location, initialText, "plugin");
    }

    /**
     * 创建全息图（完整版，可指定来源）
     *
     * 性能优化：不再调用 saveHolograms() 即时写盘，改为标记 dirty，
     * 由定时保存任务统一写盘。批量创建 N 个全息图只触发 1 次磁盘 I/O。
     *
     * @param id          全息图唯一标识
     * @param location    全息图位置
     * @param initialText 初始文本（可为null）
     * @param source      来源标识（"command" / "plugin"）
     * @return 创建的全息图对象，系统不可用时返回 null
     */
    public IHologram createHologram(String id, Location location, String initialText, String source) {
        // 系统不可用时返回 null
        if (!enabled) {
            LogUtil.warning(((Main) Main.plugin).getLanguageManager().getMessage(
                    "hologram.disabled-no-hd"
            ));
            return null;
        }

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

        // 设置来源
        hologram.setSource(source != null ? source : "plugin");

        // 添加初始文本
        if (initialText != null && !initialText.isEmpty()) {
            hologram.appendLine(initialText);
        }

        // 显示全息图
        hologram.show();

        holograms.put(id, hologram);

        // 注册到区块索引
        addToChunkIndex(id, location);

        // 标记脏数据，由定时任务统一写盘
        markDirty();

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
     * 性能优化：不再调用 dataManager.deleteHologram() 即时写盘，
     * 改为标记 dirty，由定时保存任务通过 saveAll() 全量覆盖写盘。
     * 连续删除 N 个全息图只触发 1 次磁盘 I/O。
     *
     * @param id 全息图唯一标识
     */
    public void deleteHologram(String id) {
        IHologram hologram = holograms.remove(id);
        if (hologram != null) {
            // 从区块索引移除（须在 delete 之前获取位置，delete 后可能无法获取）
            removeFromChunkIndex(id, hologram.getLocation());
            hologram.delete();
            // 标记脏数据，由定时任务统一写盘（saveAll 全量覆盖，已删除的自然不会被写入）
            markDirty();
        }
    }

    /**
     * 移动全息图到新位置（跨区块安全）
     *
     * 背景：{@link IHologram#teleport(Location)} 只会更新全息图自身持有的
     * location 字段，不会同步更新本管理器的区块索引（chunkIndex）。若移动
     * 前后跨越了不同区块，chunkIndex 仍然指向旧区块，会导致：
     *   1. 旧区块卸载时，onChunkUnload 通过过期索引误触发该全息图的 hide()；
     *   2. 新区块从未被登记，若新区块之后自然卸载，本插件不会主动 hide()，
     *      盔甲架实体会被 Minecraft 当作普通实体持久化进区块 NBT；
     *   3. 该区块下次加载时，onChunkLoad 因索引缺失直接跳过，不会调用
     *      show() / removeOwnOrphanStands() 清理，导致野生盔甲架复活；
     *   4. isValid() 只检查追踪列表中的实体是否存活，不会扫描世界中多余的
     *      同 tag 实体，定时刷新任务无法发现并清理这种情况。
     *
     * 本方法在调用 teleport() 前后正确维护 chunkIndex（先移除旧索引，
     * 执行传送，再注册新索引），是外部移动一个已存在全息图位置的唯一
     * 正确入口。请勿直接调用 IHologram.teleport() 来移动一个已显示的全息图。
     *
     * @param id          全息图唯一标识
     * @param newLocation 目标位置
     * @return 移动成功返回 true；全息图不存在或已删除返回 false
     */
    public boolean moveHologram(String id, Location newLocation) {
        IHologram hologram = holograms.get(id);
        if (hologram == null || hologram.isDeleted()) {
            return false;
        }

        // 记录旧位置，用于从旧区块索引中移除（须在 teleport 之前获取）
        Location oldLocation = hologram.getLocation();

        // 先从旧区块索引移除，避免旧区块卸载时误触发 hide()
        removeFromChunkIndex(id, oldLocation);

        // 执行实际的位置移动
        hologram.teleport(newLocation);

        // 注册到新区块索引，确保新区块加载/卸载事件能正确识别本全息图
        addToChunkIndex(id, newLocation);

        // 标记脏数据，由定时任务统一写盘
        markDirty();

        return true;
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
     * 获取指定来源的全息图
     *
     * @param source 来源标识（"command" / "plugin" / "unknown"）
     * @return 匹配来源的全息图集合
     */
    public Collection<IHologram> getHologramsBySource(String source) {
        ArrayList<IHologram> result = new ArrayList<>();
        for (IHologram hologram : holograms.values()) {
            if (source.equals(hologram.getSource())) {
                result.add(hologram);
            }
        }
        return result;
    }

    // ══════════════════════════════════════════════════════════════════════
    // 延迟保存机制
    // ══════════════════════════════════════════════════════════════════════

    /**
     * 标记有数据变更，需要写盘
     * O(1) 操作，不触发任何 I/O
     */
    private void markDirty() {
        dirty = true;
    }

    /**
     * 启动定时保存任务
     * 每 SAVE_INTERVAL tick 检查 dirty 标记，有变更则调 saveAll 统一写盘一次
     */
    private void startSaveTask() {
        stopSaveTask();

        saveTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!loaded || dataManager == null) return;
                if (!dirty) return;

                dirty = false;
                // 序列化和写盘交给后台线程，主线程只拍快照；关服、重载时的 flushSave 仍是同步写盘
                dataManager.saveAllAsync(holograms);
            }
        };

        saveTask.runTaskTimer(plugin, SAVE_INTERVAL, SAVE_INTERVAL);
    }

    /**
     * 停止定时保存任务
     */
    private void stopSaveTask() {
        if (saveTask != null) {
            saveTask.cancel();
            saveTask = null;
        }
    }

    /**
     * 立即写盘（flush）
     * 仅在 shutdown / reload / 加载完成后调用，确保数据不丢
     */
    private void flushSave() {
        if (!loaded || dataManager == null) return;
        dirty = false;
        dataManager.saveAll(holograms);
    }

    /**
     * 保存所有全息图（公共接口，向后兼容）
     * 改为标记 dirty，由定时任务统一写盘
     * 加载完成前不标记，避免覆盖 YAML 中已有的数据
     */
    public void saveHolograms() {
        if (!loaded || dataManager == null) return;
        markDirty();
    }

    // ══════════════════════════════════════════════════════════════════════
    // 加载
    // ══════════════════════════════════════════════════════════════════════

    /**
     * 加载所有全息图
     *
     * 说明：本方法对所有全息图调用 show()，但 NativeHologram.show() 已增加
     * 「区块未加载不生成」守卫，因此未加载区块的全息图不会在此处被强制加载生成，
     * 而是等待区块加载事件（onChunkLoad）就绪后再显示并清理孤儿。
     */
    private void loadHolograms() {
        if (!enabled || dataManager == null) return;

        // 原生模式下先清理孤儿盔甲架
        if (!useHD) {
            NativeHologram.cleanupAllOrphanedStands();

            // 重新显示在 loaded=false 期间由其他插件创建的全息图
            for (IHologram hologram : holograms.values()) {
                if (hologram.isShown()) {
                    hologram.hide();
                    hologram.show();
                }
            }
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

                // 设置来源
                hologram.setSource(data.getSource());

                // 添加所有行
                for (String line : data.getLines()) {
                    hologram.appendLine(line);
                }

                // 显示全息图（区块未加载时 show() 内部会跳过生成，待区块加载后再显示）
                hologram.show();

                // 添加到管理器（不触发 markDirty，加载完成后统一 flushSave）
                holograms.put(data.getId(), hologram);

                // 注册到区块索引
                addToChunkIndex(data.getId(), data.getLocation());

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

    // ══════════════════════════════════════════════════════════════════════
    // 定时刷新任务（实体有效性检查）
    // ══════════════════════════════════════════════════════════════════════

    /**
     * 启动定时刷新任务
     * 定时检查所有全息图实体有效性，自动修复丢失的实体
     */
    private void startRefreshTask() {
        if (!enabled) return;

        stopRefreshTask();

        refreshTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!loaded) return;

                int refreshedCount = 0;

                for (IHologram hologram : holograms.values()) {
                    if (hologram.isDeleted()) continue;

                    if (!hologram.isValid()) {
                        hologram.refresh();
                        refreshedCount++;
                        LogUtil.debug(((Main) Main.plugin).getLanguageManager().getMessage(
                                "hologram.refresh-single",
                                hologram.getId()
                        ));
                    }
                }

                if (refreshedCount > 0) {
                    LogUtil.info(((Main) Main.plugin).getLanguageManager().getMessage(
                            "hologram.refresh-complete",
                            String.valueOf(refreshedCount)
                    ));
                }
            }
        };

        long interval = NativeHologram.usesTextDisplay() && !useHD ? REFRESH_INTERVAL_TEXT_DISPLAY : REFRESH_INTERVAL;
        refreshTask.runTaskTimer(plugin, 1200L, interval);

        LogUtil.info(((Main) Main.plugin).getLanguageManager().getMessage(
                "hologram.refresh-task-started",
                String.valueOf(interval / 20)
        ));
    }

    /**
     * 停止定时刷新任务
     */
    private void stopRefreshTask() {
        if (refreshTask != null) {
            refreshTask.cancel();
            refreshTask = null;
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // 区块索引维护
    // ══════════════════════════════════════════════════════════════════════

    /**
     * 构建区块索引键
     *
     * @param worldName 世界名称
     * @param chunkX    区块 X 坐标
     * @param chunkZ    区块 Z 坐标
     * @return "worldName:chunkX:chunkZ"
     */
    private String buildChunkKey(String worldName, int chunkX, int chunkZ) {
        return worldName + ":" + chunkX + ":" + chunkZ;
    }

    /**
     * 通过位置构建区块索引键
     *
     * @param location 全息图位置
     * @return 区块键；世界为 null 时返回 null
     */
    private String buildChunkKey(Location location) {
        if (location == null || location.getWorld() == null) return null;
        return buildChunkKey(
                location.getWorld().getName(),
                location.getBlockX() >> 4,
                location.getBlockZ() >> 4);
    }

    /**
     * 将全息图注册到区块索引
     *
     * @param id       全息图 ID
     * @param location 全息图位置
     */
    private void addToChunkIndex(String id, Location location) {
        String key = buildChunkKey(location);
        if (key == null) return;
        chunkIndex.computeIfAbsent(key, k -> new HashSet<>()).add(id);
    }

    /**
     * 将全息图从区块索引中移除
     *
     * @param id       全息图 ID
     * @param location 全息图位置
     */
    private void removeFromChunkIndex(String id, Location location) {
        String key = buildChunkKey(location);
        if (key == null) return;
        Set<String> ids = chunkIndex.get(key);
        if (ids != null) {
            ids.remove(id);
            if (ids.isEmpty()) chunkIndex.remove(key);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // 区块事件
    // ══════════════════════════════════════════════════════════════════════

    /**
     * 区块加载事件
     * 仅原生模式（1.12+）处理
     *
     * 盔甲架重复堆叠修复（v3.2.4）：
     *   将 show() 延迟 1 tick 执行。NativeHologram.show() 内部会调用
     *   chunk.getEntities() 清理本全息图遗留的孤儿盔甲架；若在 ChunkLoadEvent 回调内
     *   直接调用 getEntities()，在 Mohist 环境下 CraftChunk.getEntities() 会等待区块
     *   实体数据就绪，导致主线程 park 阻塞。延迟 1 tick 待区块实体就绪后再执行 show()，
     *   既能完成孤儿清理（杜绝重启堆叠），又不阻塞主线程。
     *
     * @param event 区块加载事件
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onChunkLoad(ChunkLoadEvent event) {
        if (useHD || !enabled || !loaded) return;

        // 通过区块索引 O(1) 查找该区块内的全息图，无全息图的区块直接跳过
        String chunkKey = buildChunkKey(
                event.getWorld().getName(),
                event.getChunk().getX(),
                event.getChunk().getZ());
        Set<String> ids = chunkIndex.get(chunkKey);
        if (ids == null || ids.isEmpty()) return;

        // 快照一份 id 列表，避免延迟执行期间 chunkIndex 被并发修改导致迭代异常
        final List<String> idSnapshot = new ArrayList<>(ids);

        // 延迟 1 tick 执行 show()：避开 ChunkLoadEvent 回调内调用 chunk.getEntities()
        // 在 Mohist 下触发的主线程 park 阻塞（show() 内部会调用 getEntities() 清理孤儿）
        Bukkit.getScheduler().runTaskLater(plugin, () -> ensureShown(idSnapshot), 1L);
    }

    /** 没显示的显示；以为在显示、实体其实已经没了的重建 */
    private void ensureShown(List<String> ids) {
        for (String id : ids) {
            IHologram hologram = holograms.get(id);
            if (hologram == null || hologram.isDeleted()) continue;
            if (!hologram.isShown()) {
                hologram.show();
            } else if (!hologram.isValid()) {
                hologram.refresh();
            }
        }
    }

    private List<String> idsInChunk(Chunk chunk) {
        Set<String> ids = chunkIndex.get(buildChunkKey(chunk.getWorld().getName(), chunk.getX(), chunk.getZ()));
        if (ids == null || ids.isEmpty()) return null;
        return new ArrayList<>(ids);
    }

    // 文字展示实体不存盘，跟着区块的实体数据一起卸掉。实体数据的读写是排队的，和区块事件不在同一刻，
    // 只听区块事件时线上出现过「以为在显示、实体已经没了」，要 /ynhd reload 才回来，所以再听实体数据的事件。
    // 这两个事件 1.18 起才有，编译依赖是 1.17.1，只能反射注册。
    private void registerEntitySectionEvents() {
        registerChunkEvent("org.bukkit.event.world.EntitiesLoadEvent", this::onEntitiesLoad);
        registerChunkEvent("org.bukkit.event.world.EntitiesUnloadEvent", this::onEntitiesUnload);
    }

    private void registerChunkEvent(String className, java.util.function.Consumer<Chunk> handler) {
        final Class<? extends Event> type;
        try {
            type = Class.forName(className).asSubclass(Event.class);
        } catch (ClassNotFoundException | ClassCastException e) {
            return;
        }
        Bukkit.getPluginManager().registerEvent(type, this, EventPriority.MONITOR, (listener, event) -> {
            if (type.isInstance(event) && event instanceof ChunkEvent) {
                handler.accept(((ChunkEvent) event).getChunk());
            }
        }, plugin);
    }

    private void onEntitiesLoad(Chunk chunk) {
        if (!enabled || !loaded) return;
        List<String> ids = idsInChunk(chunk);
        if (ids == null) return;
        Bukkit.getScheduler().runTaskLater(plugin, () -> ensureShown(ids), 1L);
    }

    private void onEntitiesUnload(Chunk chunk) {
        if (!enabled || !loaded) return;
        List<String> ids = idsInChunk(chunk);
        if (ids == null) return;
        for (String id : ids) {
            IHologram hologram = holograms.get(id);
            if (hologram != null && hologram.isShown() && !hologram.isDeleted()) {
                hologram.hide();
            }
        }
    }

    /**
     * 区块卸载事件
     * 仅原生模式（1.12+）处理
     *
     * @param event 区块卸载事件
     */
    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        if (!useHD && enabled && loaded) {
            // 通过区块索引 O(1) 查找该区块内的全息图
            String chunkKey = buildChunkKey(
                    event.getWorld().getName(),
                    event.getChunk().getX(),
                    event.getChunk().getZ());
            Set<String> ids = chunkIndex.get(chunkKey);
            if (ids == null || ids.isEmpty()) return;

            for (String id : ids) {
                IHologram hologram = holograms.get(id);
                if (hologram != null && hologram.isShown() && !hologram.isDeleted()) {
                    hologram.hide();
                }
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // 生命周期
    // ══════════════════════════════════════════════════════════════════════

    /**
     * 关闭管理器
     * 先停止定时任务，再强制 flush 确保数据不丢，最后清理全息实体
     */
    public void shutdown() {
        stopRefreshTask();
        stopSaveTask();

        if (enabled) {
            // 强制写盘，确保关服前所有变更都已持久化
            flushSave();

            for (IHologram hologram : holograms.values()) {
                hologram.hide();
            }
        }

        holograms.clear();
        chunkIndex.clear();
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
        if (!enabled) return;

        stopRefreshTask();
        stopSaveTask();

        for (IHologram hologram : holograms.values()) {
            hologram.hide();
        }
        holograms.clear();
        chunkIndex.clear();
        dirty = false;

        if (!useHD) {
            NativeHologram.cleanupAllOrphanedStands();
        }

        if (dataManager != null) {
            dataManager.reload();
        }
        loadHolograms();
        loaded = true;

        // 重载后强制写盘一次
        flushSave();

        startRefreshTask();
        startSaveTask();

        LogUtil.info(((Main) Main.plugin).getLanguageManager().getMessage(
                "hologram.reloaded",
                String.valueOf(holograms.size())
        ));
    }
}