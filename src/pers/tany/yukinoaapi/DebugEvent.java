package pers.tany.yukinoaapi;

import de.tr7zw.nbtapi.NBTEntity;
import de.tr7zw.nbtapi.NBTTileEntity;
import net.minecraft.server.v1_12_R1.Container;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.EnchantingTable;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import pers.tany.yukinoaapi.command.DebugCommands;
import pers.tany.yukinoaapi.interfacepart.enchantment.IEnchantment;
import pers.tany.yukinoaapi.interfacepart.entity.IEntity;
import pers.tany.yukinoaapi.interfacepart.item.IItem;
import pers.tany.yukinoaapi.interfacepart.other.IDouble;

import javax.annotation.Nonnull;

public class DebugEvent implements Listener {

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent evt) {
        Player player = evt.getPlayer();
        if (DebugCommands.DEBUG.contains(player.getName())) {
            if (evt.getAction().equals(Action.LEFT_CLICK_AIR) || evt.getAction().equals(Action.RIGHT_CLICK_AIR)) {
                ItemStack itemStack = evt.getItem();
                if (!IItem.isEmpty(itemStack)) {
                    player.sendMessage("§a物品名称： §f" + IItem.getName(itemStack));
                    player.sendMessage("§a物品Type： §f" + itemStack.getType().toString());
                    player.sendMessage("§a物品子ID： §f" + itemStack.getDurability());
                    player.sendMessage("§a物品最大堆叠数： §f" + itemStack.getMaxStackSize());
                    if (itemStack.getEnchantments().size() > 0) {
                        player.sendMessage("§a物品附魔：");
                        for (Enchantment enchantment : itemStack.getEnchantments().keySet()) {
                            player.sendMessage(" §f- " + IEnchantment.getName(enchantment) + " " + itemStack.getEnchantments().get(enchantment) + "级");
                        }
                    }
                    player.sendMessage("§a物品NBT： §f" + IItem.getItemNBT(itemStack));
                    Bukkit.getConsoleSender().sendMessage("§a物品Name： §f" + IItem.getName(itemStack));
                    Bukkit.getConsoleSender().sendMessage("§a物品Type： §f" + itemStack.getType().toString());
                    Bukkit.getConsoleSender().sendMessage("§a物品子ID： §f" + itemStack.getDurability());
                    Bukkit.getConsoleSender().sendMessage("§a物品最大堆叠数： §f" + itemStack.getMaxStackSize());
                    if (itemStack.getEnchantments().size() > 0) {
                        Bukkit.getConsoleSender().sendMessage("§a物品附魔：");
                        for (Enchantment enchantment : itemStack.getEnchantments().keySet()) {
                            Bukkit.getConsoleSender().sendMessage(" §f- " + IEnchantment.getName(enchantment) + " " + itemStack.getEnchantments().get(enchantment) + "级");
                        }
                    }
                    Bukkit.getConsoleSender().sendMessage("§a物品NBT： §f" + IItem.getItemNBT(itemStack));
                    evt.setCancelled(true);
                    return;
                }
            }
            if (evt.getAction().equals(Action.LEFT_CLICK_BLOCK) || evt.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
                Block block = evt.getClickedBlock();
                evt.setCancelled(true);
                player.sendMessage("§a方块Type： §f" + block.getType().toString());
                player.sendMessage("§a方块Data： §f" + block.getData());
                player.sendMessage("§a方块所在群系： §f" + block.getBiome().toString());
                Bukkit.getConsoleSender().sendMessage("§a方块Type： §f" + block.getType().toString());
                Bukkit.getConsoleSender().sendMessage("§a方块Data： §f" + block.getData());
                Bukkit.getConsoleSender().sendMessage("§a方块所在群系： §f" + block.getBiome().toString());
                try {
                    player.sendMessage("§a方块NBT： §f" + new NBTTileEntity(block.getState()));
                    Bukkit.getConsoleSender().sendMessage("§a方块NBT： §f" + new NBTTileEntity(block.getState()));
                } catch (Exception e) {
                    player.sendMessage("§a方块NBT： §f无");
                    Bukkit.getConsoleSender().sendMessage("§a方块NBT： §f无");
                }
            }
        }
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent evt) {
        Player player = evt.getPlayer();
        if (DebugCommands.DEBUG.contains(player.getName())) {
            Entity entity = evt.getRightClicked();
            Location location = entity.getLocation();
            Vector vector = location.getDirection();
            player.sendMessage("§a实体名称： §f" + IEntity.getName(entity));
            player.sendMessage("§a实体储存名： §f" + IEntity.getSaveName(entity));
            player.sendMessage("§a实体Type： §f" + entity.getType().toString());
            player.sendMessage("§a实体UUID： §f" + entity.getUniqueId().toString());
            player.sendMessage("§a实体坐标： §f" + location.getWorld().getName() + " : " + IDouble.shortenDouble(location.getX(), 2) + " " + IDouble.shortenDouble(location.getY(), 2) + " " + IDouble.shortenDouble(location.getYaw(), 2) + " " + IDouble.shortenDouble(location.getPitch(), 2));
            player.sendMessage("§a实体向量： §f" + IDouble.shortenDouble(vector.getX(), 2) + " " + IDouble.shortenDouble(vector.getY(), 2) + " " + IDouble.shortenDouble(vector.getZ(), 2));
//            if (entity instanceof LivingEntity) {
//                LivingEntity livingEntity = (LivingEntity) entity;
//                if (livingEntity.getActivePotionEffects().size() > 0) {
//                    player.sendMessage("§a实体Buff：");
//                    for (PotionEffect potionEffect : livingEntity.getActivePotionEffects()) {
//                        MobEffectList mobEffectList = ((CraftPotionEffectType) potionEffect.getType()).getHandle();
//                        player.sendMessage(" §f- " + mobEffectList.a());
//                    }
//                }
//            }
            player.sendMessage("§a实体NBT： §f" + new NBTEntity(entity));
            Bukkit.getConsoleSender().sendMessage("§a实体名称： §f" + IEntity.getName(entity));
            Bukkit.getConsoleSender().sendMessage("§a实体储存名： §f" + IEntity.getSaveName(entity));
            Bukkit.getConsoleSender().sendMessage("§a实体Type： §f" + entity.getType().toString());
            Bukkit.getConsoleSender().sendMessage("§a实体UUID： §f" + entity.getUniqueId().toString());
            Bukkit.getConsoleSender().sendMessage("§a实体坐标： §f" + location.getWorld().getName() + " : " + IDouble.shortenDouble(location.getX(), 2) + " " + IDouble.shortenDouble(location.getY(), 2) + " " + IDouble.shortenDouble(location.getZ(), 2) + " " + IDouble.shortenDouble(location.getYaw(), 2) + " " + IDouble.shortenDouble(location.getPitch(), 2));
            Bukkit.getConsoleSender().sendMessage("§a实体向量： §f" + IDouble.shortenDouble(vector.getX(), 2) + " " + IDouble.shortenDouble(vector.getY(), 2) + " " + IDouble.shortenDouble(vector.getZ(), 2));
//            if (entity instanceof LivingEntity) {
//                LivingEntity livingEntity = (LivingEntity) entity;
//                if (livingEntity.getActivePotionEffects().size() > 0) {
//                    player.sendMessage("§a实体Buff：");
//                    for (PotionEffect potionEffect : livingEntity.getActivePotionEffects()) {
//                        MobEffectList mobEffectList = ((CraftPotionEffectType) potionEffect.getType()).getHandle();
//                        Bukkit.getConsoleSender().sendMessage(" §f- " + mobEffectList.);
//                    }
//                }
//            }
            Bukkit.getConsoleSender().sendMessage("§a实体NBT： §f" + new NBTEntity(entity));
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent evt) {
        if (evt.getRawSlot() != -999) {
            if (evt.getWhoClicked() instanceof Player) {
                Player player = (Player) evt.getWhoClicked();
                if (DebugCommands.DEBUG.contains(player.getName())) {
                    player.sendMessage("§aa本次点击类型： §f" + evt.getClick().toString());
                    player.sendMessage("§a物品栏标题： §f" + evt.getView().getTitle());
                    player.sendMessage("§a物品栏Type： §f" + evt.getClickedInventory().getType().toString());
                    player.sendMessage("§a物品栏大小： §f" + evt.getClickedInventory().getSize());
                    player.sendMessage("§a物品栏格子坐标： §f" + evt.getSlot());
                    player.sendMessage("§a视图格子坐标： §f" + evt.getRawSlot());
                    Bukkit.getConsoleSender().sendMessage("§aa本次点击类型： §f" + evt.getClick().toString());
                    Bukkit.getConsoleSender().sendMessage("§a物品栏标题： §f" + evt.getView().getTitle());
                    Bukkit.getConsoleSender().sendMessage("§a物品栏Type： §f" + evt.getClickedInventory().getType().toString());
                    Bukkit.getConsoleSender().sendMessage("§a物品栏大小： §f" + evt.getClickedInventory().getSize());
                    Bukkit.getConsoleSender().sendMessage("§a物品栏格子坐标： §f" + evt.getSlot());
                    Bukkit.getConsoleSender().sendMessage("§a视图格子坐标： §f" + evt.getRawSlot());
                }
            }
        }
    }
}
