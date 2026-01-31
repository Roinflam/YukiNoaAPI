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
     * 保存全息图数据
     *
     * @param id       全息图ID
     * @param location 位置
     * @param lines    文本行列表
     */
    public void saveHologram(String id, Location location, List<String> lines) {
        try {
            ConfigurationSection section = config.createSection(id);
            section.set("world", location.getWorld().getName());
            section.set("x", location.getX());
            section.set("y", location.getY());
            section.set("z", location.getZ());
            section.set("lines", lines);

            config.save(dataFile);
            LogUtil.debug("保存全息图: " + id);
        } catch (Exception e) {
            LogUtil.error("保存全息图数据失败: " + id, e);
        }
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

            data.put(id, new HologramData(id, location, lines));
            LogUtil.debug("读取全息图数据: " + id + " (" + lines.size() + " 行)");
        }

        return data;
    }

    /**
     * 保存所有全息图
     *
     * @param holograms 全息图映射
     */
    public void saveAll(Map<String, IHologram> holograms) {
        try {
            // 清空现有配置
            for (String key : new ArrayList<>(config.getKeys(false))) {
                config.set(key, null);
            }

            // 保存所有全息图
            for (Map.Entry<String, IHologram> entry : holograms.entrySet()) {
                IHologram hologram = entry.getValue();
                if (hologram.isDeleted()) continue;

                saveHologram(entry.getKey(), hologram.getLocation(), hologram.getLines());
            }

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
        private final String id;
        private final Location location;
        private final List<String> lines;

        public HologramData(String id, Location location, List<String> lines) {
            this.id = id;
            this.location = location;
            this.lines = new ArrayList<>(lines);
        }

        public String getId() {
            return id;
        }

        public Location getLocation() {
            return location.clone();
        }

        public List<String> getLines() {
            return new ArrayList<>(lines);
        }
    }
}