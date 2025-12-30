// 文件：PlayerEatFoodEvent.java
// 路径：src/main/java/pers/tany/yukinoaapi/interfacepart/listenevent/PlayerEatFoodEvent.java
package pers.tany.yukinoaapi.interfacepart.listenevent;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;

/**
 * 玩家吃食物事件
 * 当玩家成功吃完食物并恢复饥饿值后触发
 *
 * <p>与 PlayerItemConsumeEvent 的区别：</p>
 * <ul>
 *     <li>PlayerItemConsumeEvent：玩家开始消耗物品时触发（可能被打断）</li>
 *     <li>PlayerEatFoodEvent：玩家确认吃完食物并恢复饥饿值后触发</li>
 * </ul>
 *
 * <p>取消事件效果：</p>
 * <ul>
 *     <li>恢复玩家吃食物前的饥饿值</li>
 *     <li>返还被吃掉的食物</li>
 * </ul>
 */
public class PlayerEatFoodEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    /**
     * 被吃的食物（吃之前的状态）
     */
    private final ItemStack food;

    /**
     * 恢复的饥饿值
     */
    private final int satiety;

    /**
     * 吃食物前的饥饿值
     */
    private final int oldSatiety;

    /**
     * 吃食物后的饥饿值
     */
    private final int nowSatiety;

    /**
     * 事件是否被取消
     */
    private boolean cancelled;

    /**
     * 构造函数
     *
     * @param player     玩家
     * @param food       被吃的食物
     * @param satiety    恢复的饥饿值
     * @param oldSatiety 吃食物前的饥饿值
     * @param nowSatiety 吃食物后的饥饿值
     */
    public PlayerEatFoodEvent(@Nonnull Player player, @Nonnull ItemStack food,
                              int satiety, int oldSatiety, int nowSatiety) {
        super(player);
        this.food = food;
        this.satiety = satiety;
        this.oldSatiety = oldSatiety;
        this.nowSatiety = nowSatiety;
    }

    @Nonnull
    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Nonnull
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    /**
     * 获取被吃的食物
     * 返回的是吃之前的物品状态（包含完整数量）
     *
     * @return 食物物品
     */
    @Nonnull
    public ItemStack getFood() {
        return food;
    }

    /**
     * 获取这个食物恢复的饥饿值
     * 如果玩家饥饿值已满或接近满，实际恢复值可能小于食物本身的恢复值
     *
     * @return 实际恢复的饥饿值
     */
    public int getSatiety() {
        return satiety;
    }

    /**
     * 获取吃食物前的饥饿值
     *
     * @return 吃食物前的饥饿值（0-20）
     */
    public int getOldSatiety() {
        return oldSatiety;
    }

    /**
     * 获取吃食物后的饥饿值
     *
     * @return 吃食物后的饥饿值（0-20）
     */
    public int getNowSatiety() {
        return nowSatiety;
    }

    /**
     * 获取事件是否被取消
     *
     * @return 已取消返回 true
     */
    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * 设置事件取消状态
     * 取消后会恢复玩家的饥饿值并返还食物
     *
     * @param cancelled 是否取消
     */
    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}