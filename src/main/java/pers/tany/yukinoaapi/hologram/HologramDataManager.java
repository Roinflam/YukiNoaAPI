// HologramDataManager.java
// src/main/java/pers/tany/yukinoaapi/hologram/HologramDataManager.java
package pers.tany.yukinoaapi.hologram;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import pers.tany.yukinoaapi.Main;
import pers.tany.yukinoaapi.util.LogUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 全息图数据管理器
 * 负责加载、保存全息图数据
 *
 * 性能修复（v3.2.1）：
 *   saveAll() 原实现循环调用 saveHologram()，每个 saveHologram 内部都 config.save(dataFile)，
 *   200 个全息图 = 200 次文件写入 + YAML 序列化，全部阻塞主线程。
 *   改为内联写 ConfigurationSection，末尾统一 config.save() 一次。
 *   单个保存 saveHologram() 保持不变（仍立即写盘），供命令等单次操作使用。
 *
 * @author Roinflam
 * @since 3.1.0
 */
public class HologramDataManager {

    /** 插件实例 */
    private final Main plugin;

    /** 存储文件 */
    private final File dataFile;

    /** 全息图数据缓存 */
    private YamlConfiguration config;

    /**
     * 构造函数
     *
     * @param plugin 插件实例
     */
    public HologramDataManager(Main plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "holograms.yml");
        loadConfig();
    }

    /**
     * 加载配置文件
     */
    private void loadConfig() {
        if (!dataFile.exists()) {
            try {
                dataFile.getParentFile().mkdirs();
                dataFile.createNewFile();
                config = new YamlConfiguration();
                LogUtil.info(plugin.getLanguageManager().getMessage("hologram.data-created"));
            } catch (Exception e) {
                LogUtil.error("创建全息图数据文件失败", e);
                config = new YamlConfiguration();
            }
        } else {
            try {
                config = YamlConfiguration.loadConfiguration(dataFile);
                LogUtil.debug("全息图数据文件加载成功");
            } catch (Exception e) {
                LogUtil.error("加载全息图数据文件失败", e);
                config = new YamlConfiguration();
            }
        }
    }

    /**
     * 保存单个全息图数据（立即写盘）
     * 适用于命令创建等单次操作场景
     *
     * @param id       全息图ID
     * @param location 位置
     * @param lines    文本行列表
     * @param source   来源标识（"command" / "plugin" / "unknown"）
     */
    public void saveHologram(String id, Location location, List<String> lines, String source) {
        try {
            ConfigurationSection section = config.createSection(id);
            section.set("world", location.getWorld().getName());
            section.set("x", location.getX());
            section.set("y", location.getY());
            section.set("z", location.getZ());
            section.set("lines", lines);
            // 保存来源字段
            section.set("source", source != null ? source : "unknown");

            config.save(dataFile);
            LogUtil.debug("保存全息图: " + id);
        } catch (Exception e) {
            LogUtil.error("保存全息图数据失败: " + id, e);
        }
    }

    /**
     * 保存单个全息图数据（向后兼容，无source参数）
     *
     * @param id       全息图ID
     * @param location 位置
     * @param lines    文本行列表
     */
    public void saveHologram(String id, Location location, List<String> lines) {
        saveHologram(id, location, lines, "unknown");
    }

    /**
     * 删除全息图数据
     *
     * @param id 全息图ID
     */
    public void deleteHologram(String id) {
        try {
            config.set(id, null);
            config.save(dataFile);
            LogUtil.debug("删除全息图数据: " + id);
        } catch (Exception e) {
            LogUtil.error("删除全息图数据失败: " + id, e);
        }
    }

    /**
     * 加载所有全息图数据
     *
     * @return 全息图数据映射 (ID -> HologramData)
     */
    public Map<String, HologramData> loadAllHolograms() {
        Map<String, HologramData> data = new HashMap<>();

        for (String id : config.getKeys(false)) {
            ConfigurationSection section = config.getConfigurationSection(id);
            if (section == null) {
                LogUtil.warning("无效的全息图数据: " + id);
                continue;
            }

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

            // 读取来源字段，旧数据默认为 "unknown"
            String source = section.getString("source", "unknown");

            data.put(id, new HologramData(id, location, lines, source));
            LogUtil.debug("读取全息图数据: " + id + " (" + lines.size() + " 行, 来源: " + source + ")");
        }

        return data;
    }

    /**
     * 保存所有全息图（批量，仅在末尾写盘一次）
     *
     * 性能修复：原实现循环调用 saveHologram()，每次都触发 config.save(dataFile)，
     * N 个全息图 = N 次文件写入。改为内联写 ConfigurationSection，末尾统一写盘一次。
     * 200 个全息图从 200 次磁盘 I/O 降为 1 次。
     *
     * @param holograms 全息图映射
     */
    public void saveAll(Map<String, IHologram> holograms) {
        try {
            // 清空现有配置
            for (String key : new ArrayList<>(config.getKeys(false))) {
                config.set(key, null);
            }

            // 写入所有全息图到内存中的 config 对象（不触发磁盘 I/O）
            for (Map.Entry<String, IHologram> entry : holograms.entrySet()) {
                IHologram hologram = entry.getValue();
                if (hologram.isDeleted()) continue;

                String id = entry.getKey();
                Location location = hologram.getLocation();
                if (location == null || location.getWorld() == null) continue;

                ConfigurationSection section = config.createSection(id);
                section.set("world", location.getWorld().getName());
                section.set("x", location.getX());
                section.set("y", location.getY());
                section.set("z", location.getZ());
                section.set("lines", hologram.getLines());
                section.set("source", hologram.getSource() != null ? hologram.getSource() : "unknown");
            }

            // 统一写盘一次
            config.save(dataFile);

            LogUtil.debug("批量保存全息图完成: " + holograms.size() + " 个");
        } catch (Exception e) {
            LogUtil.error("批量保存全息图失败", e);
        }
    }

    /**
     * 重载配置
     */
    public void reload() {
        loadConfig();
    }

    /**
     * 全息图数据类
     */
    public static class HologramData {

        /** 全息图ID */
        private final String id;

        /** 位置 */
        private final Location location;

        /** 文本行列表 */
        private final List<String> lines;

        /** 来源标识 */
        private final String source;

        /**
         * 构造函数（带source）
         *
         * @param id       全息图ID
         * @param location 位置
         * @param lines    文本行
         * @param source   来源
         */
        public HologramData(String id, Location location, List<String> lines, String source) {
            this.id = id;
            this.location = location;
            this.lines = new ArrayList<>(lines);
            this.source = source != null ? source : "unknown";
        }

        /**
         * 构造函数（向后兼容，无source）
         *
         * @param id       全息图ID
         * @param location 位置
         * @param lines    文本行
         */
        public HologramData(String id, Location location, List<String> lines) {
            this(id, location, lines, "unknown");
        }

        public String getId() { return id; }
        public Location getLocation() { return location.clone(); }
        public List<String> getLines() { return new ArrayList<>(lines); }
        public String getSource() { return source; }
    }
}