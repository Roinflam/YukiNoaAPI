package pers.tany.yukinoaapi.hologram;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.plugin.Plugin;
import pers.tany.yukinoaapi.Main;
import pers.tany.yukinoaapi.hologram.impl.HolographicDisplaysHologram;
import pers.tany.yukinoaapi.hologram.impl.NativeHologram;
import pers.tany.yukinoaapi.util.LogUtil;
import pers.tany.yukinoaapi.util.NMSUtil;

import java.io.File;
import java.util.*;
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

    /** 存储文件 */
    private File dataFile;

    /**
     * 私有构造函数
     */
    private HologramManager(Plugin plugin) {
        this.plugin = plugin;
        this.holograms = new ConcurrentHashMap<>();

        // 检测是否使用 HolographicDisplays
        this.useHD = !NMSUtil.isVersionAtLeast(12) &&
                Bukkit.getPluginManager().getPlugin("HolographicDisplays") != null;

        // 初始化存储
        initStorage();

        // 注册事件
        Bukkit.getPluginManager().registerEvents(this, plugin);

        LogUtil.info(((Main) Main.plugin).getLanguageManager().getMessage(
                "hologram.mode",
                useHD ? "HolographicDisplays" : "Native (1.12+)"
        ));
    }

    /**
     * 获取单例
     */
    public static HologramManager getInstance() {
        if (instance == null) {
            instance = new HologramManager(Main.plugin);
        }
        return instance;
    }

    /**
     * 初始化存储
     */
    private void initStorage() {
        dataFile = new File(plugin.getDataFolder(), "holograms.yml");
        loadHolograms();
    }

    /**
     * 创建全息图
     */
    public IHologram createHologram(String id, Location location) {
        if (holograms.containsKey(id)) {
            LogUtil.warning("全息图ID已存在: " + id);
            return holograms.get(id);
        }

        IHologram hologram;

        if (useHD) {
            hologram = new HolographicDisplaysHologram(id, location, plugin);
        } else {
            hologram = new NativeHologram(id, location);
        }

        holograms.put(id, hologram);
        saveHolograms();

        return hologram;
    }

    /**
     * 获取全息图
     */
    public IHologram getHologram(String id) {
        return holograms.get(id);
    }

    /**
     * 删除全息图
     */
    public void deleteHologram(String id) {
        IHologram hologram = holograms.remove(id);
        if (hologram != null) {
            hologram.delete();
            saveHolograms();
        }
    }

    /**
     * 获取所有全息图
     */
    public Collection<IHologram> getAllHolograms() {
        return new ArrayList<>(holograms.values());
    }

    /**
     * 保存所有全息图
     */
    public void saveHolograms() {
        try {
            YamlConfiguration config = new YamlConfiguration();

            for (Map.Entry<String, IHologram> entry : holograms.entrySet()) {
                IHologram hologram = entry.getValue();
                if (hologram.isDeleted()) continue;

                ConfigurationSection section = config.createSection(entry.getKey());
                Location loc = hologram.getLocation();

                section.set("world", loc.getWorld().getName());
                section.set("x", loc.getX());
                section.set("y", loc.getY());
                section.set("z", loc.getZ());
                section.set("lines", hologram.getLines());
            }

            config.save(dataFile);
        } catch (Exception e) {
            LogUtil.error("保存全息图数据失败", e);
        }
    }

    /**
     * 加载所有全息图
     */
    public void loadHolograms() {
        if (!dataFile.exists()) {
            return;
        }

        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(dataFile);

            for (String id : config.getKeys(false)) {
                ConfigurationSection section = config.getConfigurationSection(id);
                if (section == null) continue;

                String worldName = section.getString("world");
                World world = Bukkit.getWorld(worldName);
                if (world == null) {
                    LogUtil.warning("世界不存在，跳过全息图: " + id + " (世界: " + worldName + ")");
                    continue;
                }

                double x = section.getDouble("x");
                double y = section.getDouble("y");
                double z = section.getDouble("z");
                Location location = new Location(world, x, y, z);

                List<String> lines = section.getStringList("lines");

                // 直接创建全息图对象，不调用 createHologram 避免触发保存
                IHologram hologram;
                if (useHD) {
                    hologram = new HolographicDisplaysHologram(id, location, plugin);
                } else {
                    hologram = new NativeHologram(id, location);
                }

                // 添加所有行
                for (String line : lines) {
                    hologram.appendLine(line);
                }

                // 显示全息图
                hologram.show();

                // 添加到管理器
                holograms.put(id, hologram);

                LogUtil.debug("加载全息图: " + id + " (" + lines.size() + " 行)");
            }

            LogUtil.info(((Main) Main.plugin).getLanguageManager().getMessage(
                    "hologram.loaded",
                    holograms.size()
            ));
        } catch (Exception e) {
            LogUtil.error("加载全息图数据失败", e);
        }
    }

    /**
     * 区块加载事件
     */
    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        // 原生实现需要重新显示全息图
        if (!useHD) {
            for (IHologram hologram : holograms.values()) {
                Location loc = hologram.getLocation();
                if (loc.getWorld().equals(event.getWorld()) &&
                        loc.getChunk().equals(event.getChunk()) &&
                        !hologram.isShown()) {
                    hologram.show();
                }
            }
        }
    }

    /**
     * 区块卸载事件
     */
    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        // 原生实现需要隐藏全息图
        if (!useHD) {
            for (IHologram hologram : holograms.values()) {
                Location loc = hologram.getLocation();
                if (loc.getWorld().equals(event.getWorld()) &&
                        loc.getChunk().equals(event.getChunk()) &&
                        hologram.isShown()) {
                    hologram.hide();
                }
            }
        }
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
    }

    /**
     * 是否使用 HD 插件
     */
    public boolean isUsingHD() {
        return useHD;
    }
}