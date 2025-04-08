package pers.tany.yukinoaapi.realizationpart.item;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitRunnable;
import pers.tany.yukinoaapi.Main;
import pers.tany.yukinoaapi.interfacepart.builder.IItemBuilder;
import pers.tany.yukinoaapi.interfacepart.register.IRegister;
import pers.tany.yukinoaapi.realizationpart.builder.ItemBuilder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 无延迟获取玩家头颅
 */
public class SkullUtil implements Listener {
    private static final SkullUtil skullUtil = new SkullUtil();
    private static final Map<String, ItemStack> skulls = new ConcurrentHashMap<>();

    private SkullUtil() {
//        for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
//            if (offlinePlayer.getName() != null) {
//                new BukkitRunnable() {
//
//                    @Override
//                    public void run() {
//                        IItemBuilder itemBuilder = new ItemBuilder("SKULL_ITEM", 1, (short) 3);
//                        @Nullable ItemStack itemStack = itemBuilder.getItemStack();
//                        SkullMeta skullMeta = (SkullMeta) itemStack.getItemMeta();
//                        skullMeta.setOwner(offlinePlayer.getName());
//                        itemStack.setItemMeta(skullMeta);
//                        skulls.put(offlinePlayer.getName(), itemStack);
//                        Inventory inventory = Bukkit.createInventory(null, 9);
//                        inventory.setItem(0, itemStack);
//                    }
//
//                }.runTaskAsynchronously(Main.plugin);
//            }
//        }
//        IRegister.registerEvents(Main.plugin, this);
    }

    public static SkullUtil getInstance() {
        return skullUtil;
    }

    /**
     * 获取玩家物品头颅
     *
     * @param name 玩家名
     * @return 头颅
     */
    
    public static IItemBuilder getSkull(String name) {
        return new ItemBuilder(skulls.get(name));
    }

    /**
     * 获取玩家物品头颅
     *
     * @param offlinePlayer 玩家
     * @return 头颅
     */
    
    public static IItemBuilder getSkull(OfflinePlayer offlinePlayer) {
        return new ItemBuilder(skulls.get(offlinePlayer.getName()));
    }

    @EventHandler
    private void onPlayerJoin(PlayerJoinEvent evt) {
        Player player = evt.getPlayer();
        if (!skulls.containsKey(player.getName())) {
            new BukkitRunnable() {

                @Override
                public void run() {
                    IItemBuilder itemBuilder = new ItemBuilder("SKULL_ITEM", 1, (short) 3);
                    @Nullable ItemStack itemStack = itemBuilder.getItemStack();
                    SkullMeta skullMeta = (SkullMeta) itemStack.getItemMeta();
                    skullMeta.setOwner(player.getName());
                    itemStack.setItemMeta(skullMeta);
                    skulls.put(player.getName(), itemStack);
                    Inventory inventory = Bukkit.createInventory(null, 9);
                    inventory.setItem(0, itemStack);
                }

            }.runTaskAsynchronously(Main.plugin);
        }
    }

}
