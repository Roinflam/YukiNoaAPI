// 文件：SkullUtil.java
// 路径：src/main/java/pers/tany/yukinoaapi/realizationpart/item/SkullUtil.java
package pers.tany.yukinoaapi.realizationpart.item;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitRunnable;
import pers.tany.yukinoaapi.Main;
import pers.tany.yukinoaapi.interfacepart.builder.IItemBuilder;
import pers.tany.yukinoaapi.realizationpart.builder.ItemBuilder;
import pers.tany.yukinoaapi.util.LangUtil;
import pers.tany.yukinoaapi.util.LogUtil;
import pers.tany.yukinoaapi.util.NMSUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 玩家头颅工具类
 * 提供跨版本的玩家头颅获取功能，支持异步预加载和缓存机制
 *
 * <p>核心功能：</p>
 * <ul>
 *     <li>异步预加载：玩家进服时在异步线程预加载头颅，避免使用时卡主线程</li>
 *     <li>智能缓存：使用 UUID 作为缓存键，支持改名后依然有效</li>
 *     <li>配置控制：可在配置文件中开关预加载功能</li>
 *     <li>版本兼容：支持 1.7.10 - 1.21+ 所有版本</li>
 * </ul>
 *
 * <p>版本差异：</p>
 * <ul>
 *     <li>1.12 及以下：使用 SKULL_ITEM + 子ID 3，setOwner(String)</li>
 *     <li>1.12.2+：使用 setOwningPlayer(OfflinePlayer)</li>
 *     <li>1.13+：使用 PLAYER_HEAD</li>
 * </ul>
 */
public class SkullUtil implements Listener {

    /**
     * 单例实例
     */
    private static final SkullUtil instance = new SkullUtil();

    /**
     * 头颅缓存（玩家UUID -> 头颅物品）
     * 使用 ConcurrentHashMap 保证线程安全
     */
    private static final Map<UUID, ItemStack> skulls = new ConcurrentHashMap<>();

    /**
     * 是否启用预加载功能
     */
    private boolean preloadEnabled = false;

    /**
     * 玩家退出时是否清除缓存
     */
    private boolean clearOnQuit = false;

    /**
     * 预加载延迟（tick）
     */
    private int preloadDelay = 20;

    /**
     * 监听器是否已注册
     */
    private boolean listenerRegistered = false;

    /**
     * 是否为高版本（1.13+，使用 PLAYER_HEAD）
     */
    private static final boolean IS_HIGH_VERSION = NMSUtil.isVersionAtLeast(13);

    /**
     * 是否支持 setOwningPlayer 方法（1.12.2+）
     */
    private static final boolean SUPPORT_OWNING_PLAYER = NMSUtil.isVersionAtLeast(12);

    /**
     * 私有构造函数
     */
    private SkullUtil() {
    }

    /**
     * 获取单例实例
     *
     * @return SkullUtil 实例
     */
    @Nonnull
    public static SkullUtil getInstance() {
        return instance;
    }

    // ==================== 初始化与配置 ====================

    /**
     * 初始化头颅工具
     * 应在插件启动时调用，会读取配置并根据需要注册监听器
     */
    public void init() {
        loadConfig();

        if (preloadEnabled && !listenerRegistered) {
            registerListener();
        }

        LogUtil.debug(LangUtil.get("skull.init-complete",
                preloadEnabled ? LangUtil.get("common.enabled") : LangUtil.get("common.disabled")));
    }

    /**
     * 从配置文件加载设置
     */
    private void loadConfig() {
        if (Main.config == null) {
            return;
        }

        preloadEnabled = Main.config.getBoolean("Skull.Preload", false);
        clearOnQuit = Main.config.getBoolean("Skull.ClearOnQuit", false);
        preloadDelay = Main.config.getInt("Skull.PreloadDelay", 20);

        // 确保延迟不小于1
        if (preloadDelay < 1) {
            preloadDelay = 1;
        }
    }

    /**
     * 重载配置
     * 会根据新配置决定是否注册/注销监听器
     */
    public void reload() {
        loadConfig();

        // 根据配置变化处理监听器
        if (preloadEnabled && !listenerRegistered) {
            registerListener();
        } else if (!preloadEnabled && listenerRegistered) {
            unregisterListener();
        }

        LogUtil.debug(LangUtil.get("skull.reload-complete",
                preloadEnabled ? LangUtil.get("common.enabled") : LangUtil.get("common.disabled")));
    }

    /**
     * 注册事件监听器
     */
    private void registerListener() {
        if (!listenerRegistered && Main.plugin != null) {
            Bukkit.getPluginManager().registerEvents(this, Main.plugin);
            listenerRegistered = true;
            LogUtil.debug(LangUtil.get("skull.listener-registered"));
        }
    }

    /**
     * 注销事件监听器
     */
    private void unregisterListener() {
        if (listenerRegistered) {
            HandlerList.unregisterAll(this);
            listenerRegistered = false;
            LogUtil.debug(LangUtil.get("skull.listener-unregistered"));
        }
    }

    // ==================== 事件监听 ====================

    /**
     * 玩家进服事件 - 异步预加载头颅
     *
     * @param event 玩家进服事件
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!preloadEnabled) {
            return;
        }

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // 已缓存则跳过
        if (skulls.containsKey(uuid)) {
            LogUtil.debug(LangUtil.get("skull.already-cached", player.getName()));
            return;
        }

        // 延迟异步预加载
        preloadSkullAsync(player, preloadDelay);
    }

    /**
     * 玩家退出事件 - 根据配置决定是否清除缓存
     *
     * @param event 玩家退出事件
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (clearOnQuit) {
            UUID uuid = event.getPlayer().getUniqueId();
            if (skulls.remove(uuid) != null) {
                LogUtil.debug(LangUtil.get("skull.cache-removed", event.getPlayer().getName()));
            }
        }
    }

    // ==================== 预加载方法 ====================

    /**
     * 异步预加载玩家头颅
     *
     * @param player 玩家
     */
    public void preloadSkullAsync(@Nonnull Player player) {
        preloadSkullAsync(player, 0);
    }

    /**
     * 异步预加载玩家头颅（带延迟）
     *
     * @param player    玩家
     * @param delayTick 延迟时间（tick）
     */
    public void preloadSkullAsync(@Nonnull Player player, int delayTick) {
        UUID uuid = player.getUniqueId();
        String playerName = player.getName();

        // 已缓存则跳过
        if (skulls.containsKey(uuid)) {
            return;
        }

        // 异步任务
        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                // 检查玩家是否还在线
                if (!player.isOnline()) {
                    LogUtil.debug(LangUtil.get("skull.player-offline", playerName));
                    return;
                }

                // 回到主线程创建头颅物品（Bukkit API 必须在主线程调用）
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        // 再次检查玩家是否在线
                        if (!player.isOnline()) {
                            return;
                        }

                        // 再次检查缓存（可能在等待期间已被缓存）
                        if (skulls.containsKey(uuid)) {
                            return;
                        }

                        try {
                            // 创建头颅物品
                            ItemStack skull = createSkullItem(player);

                            // 触发皮肤纹理加载
                            triggerTextureLoad(skull);

                            // 存入缓存
                            skulls.put(uuid, skull);

                            LogUtil.debug(LangUtil.get("skull.preload-success", playerName));
                        } catch (Exception e) {
                            LogUtil.error(LangUtil.get("skull.preload-fail", playerName), e);
                        }
                    }
                }.runTask(Main.plugin);
            }
        };

        // 执行任务
        if (delayTick > 0) {
            task.runTaskLaterAsynchronously(Main.plugin, delayTick);
        } else {
            task.runTaskAsynchronously(Main.plugin);
        }
    }

    /**
     * 触发头颅纹理加载
     * 通过创建临时容器并放入头颅来预热纹理数据
     *
     * @param skull 头颅物品
     */
    private void triggerTextureLoad(@Nonnull ItemStack skull) {
        try {
            // 创建临时容器触发纹理加载
            Inventory tempInventory = Bukkit.createInventory(null, 9);
            tempInventory.setItem(0, skull);
        } catch (Exception e) {
            // 忽略异常，纹理加载失败不影响功能
            LogUtil.debug(LangUtil.get("skull.texture-load-fail", e.getMessage()));
        }
    }

    // ==================== 静态方法（兼容旧代码） ====================

    /**
     * 获取玩家头颅物品
     * 兼容旧版调用方式
     *
     * @param name 玩家名
     * @return 头颅物品建造器
     */
    @Nonnull
    public static IItemBuilder getSkull(@Nonnull String name) {
        return instance.get(name);
    }

    /**
     * 获取玩家头颅物品
     * 兼容旧版调用方式
     *
     * @param offlinePlayer 离线玩家
     * @return 头颅物品建造器
     */
    @Nonnull
    public static IItemBuilder getSkull(@Nonnull OfflinePlayer offlinePlayer) {
        return instance.get(offlinePlayer);
    }

    // ==================== 实例获取方法 ====================

    /**
     * 获取玩家头颅物品
     *
     * @param player 在线玩家
     * @return 玩家头颅物品建造器
     */
    @Nonnull
    public IItemBuilder get(@Nonnull Player player) {
        return get((OfflinePlayer) player);
    }

    /**
     * 获取玩家头颅物品
     * 支持离线玩家，优先从缓存获取
     *
     * @param offlinePlayer 离线玩家对象
     * @return 玩家头颅物品建造器
     */
    @Nonnull
    public IItemBuilder get(@Nonnull OfflinePlayer offlinePlayer) {
        UUID uuid = offlinePlayer.getUniqueId();

        // 尝试从缓存获取
        ItemStack cached = skulls.get(uuid);
        if (cached != null) {
            // 返回克隆，避免缓存被外部修改
            return new ItemBuilder(cached.clone());
        }

        // 缓存未命中，实时创建
        ItemStack skull = createSkullItem(offlinePlayer);
        return new ItemBuilder(skull);
    }

    /**
     * 根据玩家名获取头颅物品
     *
     * @param playerName 玩家名称
     * @return 玩家头颅物品建造器
     */
    @Nonnull
    @SuppressWarnings("deprecation")
    public IItemBuilder get(@Nonnull String playerName) {
        // 先尝试获取在线玩家
        Player onlinePlayer = Bukkit.getPlayerExact(playerName);
        if (onlinePlayer != null) {
            return get(onlinePlayer);
        }

        // 获取离线玩家
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
        return get(offlinePlayer);
    }

    /**
     * 根据 UUID 获取头颅物品
     *
     * @param uuid 玩家 UUID
     * @return 玩家头颅物品建造器
     */
    @Nonnull
    public IItemBuilder get(@Nonnull UUID uuid) {
        // 尝试从缓存获取
        ItemStack cached = skulls.get(uuid);
        if (cached != null) {
            return new ItemBuilder(cached.clone());
        }

        // 缓存未命中，获取离线玩家并创建
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
        return get(offlinePlayer);
    }

    // ==================== 直接获取 ItemStack ====================

    /**
     * 获取玩家头颅 ItemStack
     *
     * @param player 玩家
     * @return 头颅物品
     */
    @Nonnull
    public ItemStack getItem(@Nonnull Player player) {
        return get(player).getItemStack();
    }

    /**
     * 获取玩家头颅 ItemStack
     *
     * @param playerName 玩家名称
     * @return 头颅物品
     */
    @Nonnull
    public ItemStack getItem(@Nonnull String playerName) {
        return get(playerName).getItemStack();
    }

    /**
     * 获取玩家头颅 ItemStack
     *
     * @param uuid 玩家 UUID
     * @return 头颅物品
     */
    @Nonnull
    public ItemStack getItem(@Nonnull UUID uuid) {
        return get(uuid).getItemStack();
    }

    // ==================== 创建头颅物品 ====================

    /**
     * 创建玩家头颅物品
     *
     * @param offlinePlayer 玩家对象
     * @return 头颅物品
     */
    @Nonnull
    @SuppressWarnings("deprecation")
    private ItemStack createSkullItem(@Nonnull OfflinePlayer offlinePlayer) {
        ItemStack skull = createBaseSkull();
        SkullMeta meta = (SkullMeta) skull.getItemMeta();

        if (meta != null) {
            // 根据版本使用不同的 API
            if (SUPPORT_OWNING_PLAYER) {
                // 1.12.2+ 使用 setOwningPlayer（更可靠，支持 UUID）
                meta.setOwningPlayer(offlinePlayer);
            } else {
                // 1.7.10 - 1.12.1 使用 setOwner（已废弃但必须用）
                String name = offlinePlayer.getName();
                if (name != null && !name.isEmpty()) {
                    meta.setOwner(name);
                }
            }
            skull.setItemMeta(meta);
        }

        return skull;
    }

    /**
     * 创建基础头颅物品（无皮肤）
     *
     * @return 基础头颅物品
     */
    @Nonnull
    private ItemStack createBaseSkull() {
        if (IS_HIGH_VERSION) {
            // 1.13+ 使用 PLAYER_HEAD
            return new ItemStack(Material.PLAYER_HEAD);
        } else {
            // 1.7.10 - 1.12.2 使用 SKULL_ITEM + 子ID 3（玩家头颅）
            return new ItemBuilder("SKULL_ITEM", 1, (short) 3).getItemStack();
        }
    }

    // ==================== 缓存管理 ====================

    /**
     * 手动添加头颅到缓存
     *
     * @param uuid  玩家 UUID
     * @param skull 头颅物品
     */
    public void addToCache(@Nonnull UUID uuid, @Nonnull ItemStack skull) {
        skulls.put(uuid, skull.clone());
    }

    /**
     * 从缓存中移除指定玩家的头颅
     *
     * @param uuid 玩家 UUID
     * @return 被移除的头颅物品，如果不存在返回 null
     */
    @Nullable
    public ItemStack removeFromCache(@Nonnull UUID uuid) {
        return skulls.remove(uuid);
    }

    /**
     * 从缓存中移除指定玩家的头颅
     *
     * @param player 玩家
     * @return 被移除的头颅物品，如果不存在返回 null
     */
    @Nullable
    public ItemStack removeFromCache(@Nonnull OfflinePlayer player) {
        return skulls.remove(player.getUniqueId());
    }

    /**
     * 清空所有头颅缓存
     */
    public void clearCache() {
        int size = skulls.size();
        skulls.clear();
        LogUtil.debug(LangUtil.get("skull.cache-cleared", size));
    }

    /**
     * 检查玩家头颅是否已缓存
     *
     * @param uuid 玩家 UUID
     * @return 已缓存返回 true
     */
    public boolean isCached(@Nonnull UUID uuid) {
        return skulls.containsKey(uuid);
    }

    /**
     * 检查玩家头颅是否已缓存
     *
     * @param player 玩家
     * @return 已缓存返回 true
     */
    public boolean isCached(@Nonnull OfflinePlayer player) {
        return skulls.containsKey(player.getUniqueId());
    }

    /**
     * 获取当前缓存大小
     *
     * @return 缓存中的头颅数量
     */
    public int getCacheSize() {
        return skulls.size();
    }

    // ==================== 配置相关 ====================

    /**
     * 检查预加载功能是否启用
     *
     * @return 启用返回 true
     */
    public boolean isPreloadEnabled() {
        return preloadEnabled;
    }

    /**
     * 设置预加载功能开关（运行时修改，不会保存到配置文件）
     *
     * @param enabled 是否启用
     */
    public void setPreloadEnabled(boolean enabled) {
        if (this.preloadEnabled != enabled) {
            this.preloadEnabled = enabled;

            if (enabled && !listenerRegistered) {
                registerListener();
            } else if (!enabled && listenerRegistered) {
                unregisterListener();
            }
        }
    }

    /**
     * 获取预加载延迟
     *
     * @return 延迟时间（tick）
     */
    public int getPreloadDelay() {
        return preloadDelay;
    }

    /**
     * 设置预加载延迟
     *
     * @param delayTick 延迟时间（tick），最小为 1
     */
    public void setPreloadDelay(int delayTick) {
        this.preloadDelay = Math.max(1, delayTick);
    }
}