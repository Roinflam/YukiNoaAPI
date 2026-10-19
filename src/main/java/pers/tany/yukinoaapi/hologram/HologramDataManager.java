// HologramDataManager.java
// src/main/java/pers/tany/yukinoaapi/hologram/HologramDataManager.java
package pers.tany.yukinoaapi.hologram;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import pers.tany.yukinoaapi.Main;
import pers.tany.yukinoaapi.interfacepart.configuration.IConfig;
import pers.tany.yukinoaapi.util.LogUtil;

import java.io.File;
import java.io.IOException;
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
 * 性能修复（3.4.0）：
 *   定时保存改用 saveAllAsync()：主线程只填内存里的配置并拍快照，YAML 序列化和写盘交给
 *   IConfig.saveConfigAsync 的后台线程。全息图多时整份序列化每次 20~60 ms，每 3 秒一次就是一次卡顿。
 *   saveAll() 仍是同步写盘（关服、重载、加载完成后用）。提交过异步保存以后，同步写盘交给
 *   IConfig.saveConfig：和后台写入共用同一把文件锁，并作废排队中更早的快照，不会被它们晚到覆盖；
 *   重新读盘之前先等后台写完，不会读到还没写完的旧文件。
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

    /** 定时保存走后台写盘（关掉就和以前一样在主线程同步写） */
    private static final boolean ASYNC_SAVE = true;

    /** 提交过异步保存：之后同步写盘要走 IConfig 的文件锁，重新读盘前要先等后台写完 */
    private boolean asyncUsed;

    /** IConfig 解析出的保存位置是不是就是 dataFile（第一次异步保存时判断一次） */
    private Boolean asyncTargetOk;

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

            writeToDisk();
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
            writeToDisk();
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
            fillSections(holograms);

            // 统一写盘一次（排队中更早的异步快照会被作废，不会晚到把这次的覆盖回去）
            writeToDisk();

            LogUtil.debug("批量保存全息图完成: " + holograms.size() + " 个");
        } catch (Exception e) {
            LogUtil.error("批量保存全息图失败", e);
        }
    }

    /**
     * 保存所有全息图（定时保存用，3.4.0 新增）
     *
     * 和 {@link #saveAll} 写出同样的文件，但主线程上只把数据写进内存里的配置、拍一份快照，
     * YAML 序列化和写盘在后台单线程完成（同一文件连续提交只写最新一份）。
     * 数据文件不在 IConfig 解析出的位置时退回同步的 {@link #saveAll}；快照失败时 IConfig 自己会退回同步保存。
     * 必须在主线程调用。
     *
     * @param holograms 全息图映射
     */
    public void saveAllAsync(Map<String, IHologram> holograms) {
        if (!ASYNC_SAVE || !asyncTargetMatches()) {
            saveAll(holograms);
            return;
        }
        try {
            fillSections(holograms);
            asyncUsed = true;
            IConfig.saveConfigAsync(plugin, config, "", dataFile.getName());
            LogUtil.debug("批量保存全息图已提交后台写盘: " + holograms.size() + " 个");
        } catch (Exception e) {
            LogUtil.error("批量保存全息图失败", e);
        }
    }

    /**
     * 把所有全息图写进内存中的 config 对象（不触发磁盘 I/O），先清空原有内容
     */
    private void fillSections(Map<String, IHologram> holograms) {
        // 清空现有配置
        for (String key : new ArrayList<>(config.getKeys(false))) {
            config.set(key, null);
        }

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
    }

    /**
     * IConfig 按插件取数据目录（别的插件可能用自定义目录）；本库自己的目录就是插件目录，
     * 正常总是同一个文件。万一不是，异步保存会写到别处，所以只在确认是同一个文件时才用。
     */
    private boolean asyncTargetMatches() {
        if (asyncTargetOk == null) {
            boolean ok;
            try {
                File target = new File(new File(IConfig.getValidPluginDataFolder(plugin), ""), dataFile.getName());
                ok = target.toPath().toAbsolutePath().normalize().equals(dataFile.toPath().toAbsolutePath().normalize());
            } catch (Throwable e) {
                ok = false;
            }
            asyncTargetOk = ok;
        }
        return asyncTargetOk;
    }

    /**
     * 同步写盘
     *
     * 提交过异步保存以后交给 IConfig.saveConfig：后台正好在写这个文件就等它写完这一份，
     * 排队中更早的快照直接作废，这次写的内容不会被它们晚到覆盖回去，也不会和后台同时写同一个文件。
     * 只靠先等队列清空挡不住这一点：等待有上限，队列卡住超过上限就直接往下走了。
     * asyncUsed 只在 asyncTargetMatches() 确认过 IConfig 解析出的就是 dataFile 之后才会置上，所以写的是同一个文件。
     * IConfig.saveConfig 写失败时自己在控制台报错、不往外抛，所以这条路径上调用方的 LogUtil.error 不会触发。
     *
     * 没提交过异步保存时照旧直接 config.save：这个文件没有后台写入，两者写出的东西一样，出错时还能留下完整堆栈。
     */
    private void writeToDisk() throws IOException {
        if (asyncUsed) {
            IConfig.saveConfig(plugin, config, "", dataFile.getName());
        } else {
            config.save(dataFile);
        }
    }

    /**
     * 提交过异步保存时，等后台把排队中的快照写完（重新读盘之前调用）
     */
    private void awaitAsyncWrites() {
        if (asyncUsed) {
            IConfig.flushAsyncSaves();
        }
    }

    /**
     * 重载配置
     */
    public void reload() {
        // 后台还没写完就读，会读到旧文件。等待最多 30 秒，超时读到的就是上一次落盘的内容，
        // 和重载本来就丢掉还没到定时保存的改动是同一回事。HologramManager 重载完马上 saveAll 一次，
        // 晚到的旧快照要么被作废、要么被这次覆盖，文件最后和重载读进来的内容一致
        awaitAsyncWrites();
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