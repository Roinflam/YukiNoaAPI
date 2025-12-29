package pers.tany.yukinoaapi.interfacepart.listenevent;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;


/**
 * 玩家吃食物事件
 */
public class PlayerEatFoodEvent extends PlayerEvent implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private final ItemStack food;
    private final int satiety;
    private final int oldSatiety;
    private final int nowSatiety;
    private boolean cancellable;

    public PlayerEatFoodEvent(Player player, ItemStack food, int satiety, int oldSatiety, int nowSatiety) {
        super(player);
        this.food = food;
        this.satiety = satiety;
        this.oldSatiety = oldSatiety;
        this.nowSatiety = nowSatiety;
    }

    
    public static HandlerList getHandlerList() {
        return handlers;
    }

    
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    /**
     * 获取食物吃前的物品堆
     *
     * @return 食物
     */
    public ItemStack getFood() {
        return food;
    }

    /**
     * 获取这个食物恢复了多少饥饿度（如果超出上限，则按恢复的计算）
     *
     * @return 饥饿度
     */
    public int getSatiety() {
        return satiety;
    }

    /**
     * 获取吃食物前的饥饿度
     *
     * @return 吃食物前的饥饿度
     */
    public int getOldSatiety() {
        return oldSatiety;
    }

    /**
     * 获取吃食物后的饥饿度
     *
     * @return 吃食物后的饥饿度
     */
    public int getNowSatiety() {
        return nowSatiety;
    }

    /**
     * 获取该事件是否被取消
     *
     * @return 事件取消状态
     */
    @Override
    public boolean isCancelled() {
        return cancellable;
    }

    /**
     * 设置该事件取消的状态
     *
     * @param cancellable 是否取消
     */
    @Override
    public void setCancelled(boolean cancellable) {
        this.cancellable = cancellable;
    }
}
