// 文件：IHologram.java
// 路径：src/main/java/pers/tany/yukinoaapi/hologram/IHologram.java
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
     *
     * @return 全息图唯一标识
     */
    String getId();

    /**
     * 获取位置
     *
     * @return 全息图所在位置的副本
     */
    Location getLocation();

    /**
     * 设置位置并传送
     *
     * @param location 目标位置
     */
    void teleport(Location location);

    /**
     * 获取所有行
     *
     * @return 文本行列表的副本
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
     *
     * @param text 文本内容
     */
    void appendLine(String text);

    /**
     * 插入一行
     *
     * @param index 行索引（从0开始）
     * @param text  文本内容
     */
    void insertLine(int index, String text);

    /**
     * 删除指定行
     *
     * @param index 行索引（从0开始）
     */
    void removeLine(int index);

    /**
     * 清空所有行
     */
    void clearLines();

    /**
     * 获取行数
     *
     * @return 当前全息图的行数
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
     *
     * @return 已显示返回true
     */
    boolean isShown();

    /**
     * 删除全息图（永久）
     */
    void delete();

    /**
     * 是否已删除
     *
     * @return 已删除返回true
     */
    boolean isDeleted();

    // ==================== 3.2.0 新增方法 ====================

    /**
     * 获取全息图来源
     * "command" = 命令创建, "plugin" = 插件创建, "unknown" = 未知/旧数据
     *
     * @return 来源标识
     */
    String getSource();

    /**
     * 设置全息图来源
     *
     * @param source 来源标识（"command" / "plugin" / "unknown"）
     */
    void setSource(String source);

    /**
     * 检查全息图实体是否仍然有效（未被外部移除）
     * 用于定时刷新检测
     *
     * @return 所有实体存活返回true，有实体丢失返回false
     */
    boolean isValid();

    /**
     * 刷新全息图（隐藏后重新显示）
     * 用于修复被外部移除的实体
     */
    void refresh();
}