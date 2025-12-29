package pers.tany.yukinoaapi.realizationpart.listenevent;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import pers.tany.yukinoaapi.Main;
import pers.tany.yukinoaapi.interfacepart.listenevent.PlayerEatFoodEvent;
import pers.tany.yukinoaapi.interfacepart.register.IRegister;
import pers.tany.yukinoaapi.realizationpart.container.KeyToValue;

import javax.annotation.Nonnull;
import java.util.HashMap;


public class PlayerEatFood implements Listener {
    private static final PlayerEatFood playerEatFood = new PlayerEatFood();
    private final HashMap<String, Integer> lastFoodLevel = new HashMap<>();
    private final HashMap<String, KeyToValue<ItemStack, Integer>> lastPlayerItemConsume = new HashMap<>();
    private boolean mainHand;

    private PlayerEatFood() {
        new BukkitRunnable() {

            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (lastPlayerItemConsume.containsKey(player.getName())) {
                        ItemStack itemStack = lastPlayerItemConsume.get(player.getName()).getKey();
                        int lastFoodLevel = lastPlayerItemConsume.get(player.getName()).getValue();
                        int nowFoodLevel = player.getFoodLevel();
                        if (nowFoodLevel > lastFoodLevel) {
                            PlayerEatFoodEvent playerEatFoodEvent = new PlayerEatFoodEvent(player, itemStack, nowFoodLevel - lastFoodLevel, lastFoodLevel, nowFoodLevel);
                            Bukkit.getServer().getPluginManager().callEvent(playerEatFoodEvent);
                            if (playerEatFoodEvent.isCancelled()) {
                                player.setFoodLevel(lastFoodLevel);
                                if (itemStack.getAmount() > 1) {
                                    for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
                                        ItemStack i = player.getInventory().getItem(slot);
                                        if (itemStack.isSimilar(i) && itemStack.getAmount() == i.getAmount() + 1) {
                                            i.setAmount(i.getAmount() + 1);
                                            player.updateInventory();
                                            break;
                                        }
                                    }
                                } else {
                                    if (mainHand) {
                                        player.getInventory().setItemInHand(itemStack);
                                    } else {
                                        player.getInventory().setItemInOffHand(itemStack);
                                    }
                                }
                            }
                        }

                        lastPlayerItemConsume.remove(player.getName());
                    } else {
                        lastFoodLevel.put(player.getName(), player.getFoodLevel());
                    }
                }
            }

        }.runTaskTimer(Main.plugin, 1, 1);
        IRegister.registerEvents(Main.plugin, this);
    }

    public static PlayerEatFood getInstance() {
        return playerEatFood;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onPlayerItemConsume(PlayerItemConsumeEvent evt) {
        Player player = evt.getPlayer();
        lastPlayerItemConsume.put(player.getName(), new KeyToValue<ItemStack, Integer>(evt.getItem(), lastFoodLevel.getOrDefault(player.getName(), 20)));
        mainHand = player.getItemInHand().equals(evt.getItem());
    }
}
