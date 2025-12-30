// 文件：PlayerEatFood.java
// 路径：src/main/java/pers/tany/yukinoaapi/realizationpart/listenevent/PlayerEatFood.java
package pers.tany.yukinoaapi.realizationpart.listenevent;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import pers.tany.yukinoaapi.Main;
import pers.tany.yukinoaapi.interfacepart.listenevent.PlayerEatFoodEvent;
import pers.tany.yukinoaapi.interfacepart.register.IRegister;
import pers.tany.yukinoaapi.realizationpart.container.KeyToValue;

import java.util.HashMap;

/**
 * 玩家吃食物事件监听器
 * 用于触发自定义的 PlayerEatFoodEvent 事件
 *
 * <p>工作原理：</p>
 * <ul>
 *     <li>监听 PlayerItemConsumeEvent 记录玩家吃食物前的状态</li>
 *     <li>通过定时任务检测饥饿值变化来确认食物被吃完</li>
 *     <li>触发 PlayerEatFoodEvent，支持取消操作（返还食物和饥饿值）</li>
 * </ul>
 */
public class PlayerEatFood implements Listener {

    /**
     * 单例实例
     */
    private static final PlayerEatFood playerEatFood = new PlayerEatFood();

    /**
     * 玩家上一次记录的饥饿值
     */
    private final HashMap<String, Integer> lastFoodLevel = new HashMap<>();

    /**
     * 玩家正在吃的食物记录
     * Key: 食物物品
     * Value: 吃之前的饥饿值
     */
    private final HashMap<String, KeyToValue<ItemStack, Integer>> lastPlayerItemConsume = new HashMap<>();

    /**
     * 玩家吃食物时是否在主手
     * Key: 玩家名
     * Value: 是否主手
     */
    private final HashMap<String, Boolean> playerMainHand = new HashMap<>();

    /**
     * 私有构造函数
     */
    private PlayerEatFood() {
        new BukkitRunnable() {

            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    String playerName = player.getName();
                    if (lastPlayerItemConsume.containsKey(playerName)) {
                        ItemStack itemStack = lastPlayerItemConsume.get(playerName).getKey();
                        int lastFood = lastPlayerItemConsume.get(playerName).getValue();
                        int nowFood = player.getFoodLevel();

                        if (nowFood > lastFood) {
                            // 触发自定义事件
                            PlayerEatFoodEvent playerEatFoodEvent = new PlayerEatFoodEvent(
                                    player, itemStack, nowFood - lastFood, lastFood, nowFood
                            );
                            Bukkit.getServer().getPluginManager().callEvent(playerEatFoodEvent);

                            // 事件被取消，恢复状态
                            if (playerEatFoodEvent.isCancelled()) {
                                player.setFoodLevel(lastFood);
                                restoreFood(player, itemStack, playerMainHand.getOrDefault(playerName, true));
                            }
                        }

                        // 清理记录
                        lastPlayerItemConsume.remove(playerName);
                        playerMainHand.remove(playerName);
                    } else {
                        lastFoodLevel.put(playerName, player.getFoodLevel());
                    }
                }
            }

        }.runTaskTimer(Main.plugin, 1, 1);
        IRegister.registerEvents(Main.plugin, this);
    }

    /**
     * 获取单例实例
     *
     * @return PlayerEatFood 实例
     */
    public static PlayerEatFood getInstance() {
        return playerEatFood;
    }

    /**
     * 恢复玩家的食物
     *
     * @param player   玩家
     * @param food     食物物品
     * @param mainHand 是否在主手
     */
    @SuppressWarnings("deprecation")
    private void restoreFood(Player player, ItemStack food, boolean mainHand) {
        if (food.getAmount() > 1) {
            // 堆叠食物，找到对应槽位恢复数量
            for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
                ItemStack item = player.getInventory().getItem(slot);
                if (item != null && food.isSimilar(item) && food.getAmount() == item.getAmount() + 1) {
                    item.setAmount(item.getAmount() + 1);
                    player.updateInventory();
                    break;
                }
            }
        } else {
            // 单个食物，放回原手
            if (mainHand) {
                player.getInventory().setItemInHand(food);
            } else {
                // 1.9+ 才有副手，这里不做版本判断
                // 因为 1.8 及以下 mainHand 永远是 true
                player.getInventory().setItemInOffHand(food);
            }
        }
    }

    /**
     * 玩家消耗物品事件
     *
     * @param evt 事件
     */
    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.HIGHEST)
    private void onPlayerItemConsume(PlayerItemConsumeEvent evt) {
        Player player = evt.getPlayer();
        String playerName = player.getName();

        // 记录吃食物前的状态
        lastPlayerItemConsume.put(playerName, new KeyToValue<>(
                evt.getItem(),
                lastFoodLevel.getOrDefault(playerName, 20)
        ));

        // 记录是否在主手
        playerMainHand.put(playerName, player.getItemInHand().equals(evt.getItem()));
    }

    /**
     * 玩家退出事件
     * 清理数据防止内存泄漏
     *
     * @param evt 事件
     */
    @EventHandler(priority = EventPriority.MONITOR)
    private void onPlayerQuit(PlayerQuitEvent evt) {
        String playerName = evt.getPlayer().getName();
        lastFoodLevel.remove(playerName);
        lastPlayerItemConsume.remove(playerName);
        playerMainHand.remove(playerName);
    }
}