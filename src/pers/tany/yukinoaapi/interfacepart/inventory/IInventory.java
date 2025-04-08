package pers.tany.yukinoaapi.interfacepart.inventory;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.bukkit.scheduler.BukkitRunnable;
import pers.tany.yukinoaapi.Main;

import javax.annotation.Nonnull;

/**
 * 容器物品相关
 */
public interface IInventory {
    /**
     * 同步打开物品栏
     *
     * @param inventory 涉及物品栏
     * @param player    涉及玩家
     */
    static void openInventory(Inventory inventory, Player player) {
        new BukkitRunnable() {

            @Override
            public void run() {
                player.openInventory(inventory);
            }

        }.runTask(Main.plugin);
    }

    /**
     * 同步打开物品栏
     *
     * @param inventoryHolder 涉及物品栏
     * @param player          涉及玩家
     */
    static void openInventory(InventoryHolder inventoryHolder, Player player) {
        new BukkitRunnable() {

            @Override
            public void run() {
                player.openInventory(inventoryHolder.getInventory());
            }

        }.runTask(Main.plugin);
    }

    /**
     * 同步打开物品栏
     *
     * @param inventoryView 涉及物品栏
     * @param player        涉及玩家
     */
    static void openInventory(InventoryView inventoryView, Player player) {
        new BukkitRunnable() {

            @Override
            public void run() {
                player.openInventory(inventoryView);
            }

        }.runTask(Main.plugin);
    }
}
