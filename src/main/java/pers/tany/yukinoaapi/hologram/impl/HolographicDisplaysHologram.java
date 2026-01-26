// HolographicDisplaysHologram.java - 简化版（HD 插件自动处理彩虹）
package pers.tany.yukinoaapi.hologram.impl;

import com.gmail.filoghost.holographicdisplays.api.Hologram;
import com.gmail.filoghost.holographicdisplays.api.HologramsAPI;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;
import pers.tany.yukinoaapi.hologram.IHologram;

import java.util.ArrayList;
import java.util.List;

/**
 * HolographicDisplays 全息图实现（1.12-）
 * HD 插件原生支持 &u 彩虹颜色，无需特殊处理
 *
 * @author Roinflam
 * @since 3.1.0
 */
public class HolographicDisplaysHologram implements IHologram {

    /** 全息图ID */
    private final String id;

    /** HD全息图对象 */
    private Hologram hologram;

    /** 插件实例 */
    private final Plugin plugin;

    /** 是否已删除 */
    private boolean deleted;

    /**
     * 构造函数
     */
    public HolographicDisplaysHologram(String id, Location location, Plugin plugin) {
        this.id = id;
        this.plugin = plugin;
        this.deleted = false;
        this.hologram = HologramsAPI.createHologram(plugin, location);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Location getLocation() {
        return hologram.getLocation();
    }

    @Override
    public void teleport(Location location) {
        if (deleted) return;
        hologram.teleport(location);
    }

    @Override
    public List<String> getLines() {
        List<String> lines = new ArrayList<>();
        for (int i = 0; i < hologram.size(); i++) {
            lines.add(hologram.getLine(i).toString());
        }
        return lines;
    }

    @Override
    public void setLine(int index, String text) {
        if (deleted || index < 0 || index >= hologram.size()) return;

        hologram.removeLine(index);
        // HD 插件会自动处理 &u 彩虹色，直接传递原始文本即可
        hologram.insertTextLine(index, text);
    }

    @Override
    public void appendLine(String text) {
        if (deleted) return;
        // HD 插件会自动处理 &u 彩虹色，直接传递原始文本即可
        hologram.appendTextLine(text);
    }

    @Override
    public void insertLine(int index, String text) {
        if (deleted || index < 0 || index > hologram.size()) return;
        // HD 插件会自动处理 &u 彩虹色，直接传递原始文本即可
        hologram.insertTextLine(index, text);
    }

    @Override
    public void removeLine(int index) {
        if (deleted || index < 0 || index >= hologram.size()) return;
        hologram.removeLine(index);
    }

    @Override
    public void clearLines() {
        if (deleted) return;
        hologram.clearLines();
    }

    @Override
    public int size() {
        return hologram.size();
    }

    @Override
    public void show() {
        // HD插件创建后自动显示，无需额外操作
    }

    @Override
    public void hide() {
        // HD插件不支持临时隐藏，只能删除
    }

    @Override
    public boolean isShown() {
        return !deleted;
    }

    @Override
    public void delete() {
        if (deleted) return;

        hologram.delete();
        deleted = true;
    }

    @Override
    public boolean isDeleted() {
        return deleted;
    }
}