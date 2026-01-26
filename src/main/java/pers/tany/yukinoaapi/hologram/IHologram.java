// IHologram.java - 全息图接口
package pers.tany.yukinoaapi.hologram;

import org.bukkit.Location;

import java.util.List;

/**
 * 全息图接口
 * 统一的API，隐藏底层实现差异
 *
 * @author Roinflam
 * @since 3.1.0
 */
public interface IHologram {

    /**
     * 获取全息图ID
     */
    String getId();

    /**
     * 获取位置
     */
    Location getLocation();

    /**
     * 设置位置并传送
     */
    void teleport(Location location);

    /**
     * 获取所有行
     */
    List<String> getLines();

    /**
     * 设置指定行的文本
     *
     * @param index 行索引（从0开始）
     * @param text  文本内容
     */
    void setLine(int index, String text);

    /**
     * 添加一行
     */
    void appendLine(String text);

    /**
     * 插入一行
     */
    void insertLine(int index, String text);

    /**
     * 删除指定行
     */
    void removeLine(int index);

    /**
     * 清空所有行
     */
    void clearLines();

    /**
     * 获取行数
     */
    int size();

    /**
     * 显示全息图
     */
    void show();

    /**
     * 隐藏全息图
     */
    void hide();

    /**
     * 是否已显示
     */
    boolean isShown();

    /**
     * 删除全息图（永久）
     */
    void delete();

    /**
     * 是否已删除
     */
    boolean isDeleted();
}