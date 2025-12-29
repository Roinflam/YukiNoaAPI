package pers.tany.yukinoaapi;

import de.tr7zw.nbtapi.NBTEntity;
import de.tr7zw.nbtapi.NBTTileEntity;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
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
import pers.tany.yukinoaapi.util.LangUtil;
import pers.tany.yukinoaapi.util.LogUtil;

/**
 * Debug模式事件监听器
 *
 * @author tany
 * @since 1.0.0
 */
public class DebugEvent implements Listener {

    /**
     * 监听玩家交互事件
     *
     * @param evt 交互事件
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent evt) {
        Player player = evt.getPlayer();

        if (!DebugCommands.DEBUG.contains(player.getName())) {
            return;
        }

        if (evt.getAction().equals(Action.LEFT_CLICK_AIR) || evt.getAction().equals(Action.RIGHT_CLICK_AIR)) {
            ItemStack itemStack = evt.getItem();
            if (!IItem.isEmpty(itemStack)) {
                sendItemInfo(player, itemStack);
                evt.setCancelled(true);
                return;
            }
        }

        if (evt.getAction().equals(Action.LEFT_CLICK_BLOCK) || evt.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
            Block block = evt.getClickedBlock();
            if (block != null) {
                sendBlockInfo(player, block);
                evt.setCancelled(true);
            }
        }
    }

    /**
     * 监听玩家右键实体事件
     *
     * @param evt 交互实体事件
     */
    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent evt) {
        Player player = evt.getPlayer();

        if (!DebugCommands.DEBUG.contains(player.getName())) {
            return;
        }

        Entity entity = evt.getRightClicked();
        sendEntityInfo(player, entity);
    }

    /**
     * 监听物品栏点击事件
     *
     * @param evt 物品栏点击事件
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent evt) {
        if (evt.getRawSlot() == -999) {
            return;
        }

        if (!(evt.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) evt.getWhoClicked();

        if (!DebugCommands.DEBUG.contains(player.getName())) {
            return;
        }

        sendInventoryInfo(player, evt);

        ItemStack itemStack = evt.getCurrentItem();
        if (!IItem.isEmpty(itemStack)) {
            sendItemInfo(player, itemStack);
        }
    }

    /**
     * 发送物品信息到玩家和控制台
     *
     * @param player    玩家
     * @param itemStack 物品
     */
    private void sendItemInfo(Player player, ItemStack itemStack) {
        String itemName = IItem.getName(itemStack);
        String itemType = itemStack.getType().toString();
        short durability = itemStack.getDurability();
        int maxStackSize = itemStack.getMaxStackSize();
        String itemNBT = IItem.getItemNBT(itemStack);

        player.sendMessage(LangUtil.get("item.name", itemName));
        player.sendMessage(LangUtil.get("item.type", itemType));
        player.sendMessage(LangUtil.get("item.sub-id", durability));
        player.sendMessage(LangUtil.get("item.max-stack", maxStackSize));

        if (!itemStack.getEnchantments().isEmpty()) {
            player.sendMessage(LangUtil.get("item.enchantments"));
            for (Enchantment enchantment : itemStack.getEnchantments().keySet()) {
                int level = itemStack.getEnchantments().get(enchantment);
                player.sendMessage(LangUtil.get("item.enchantment-entry", IEnchantment.getName(enchantment), level));
            }
        }

        player.sendMessage(LangUtil.get("item.nbt", itemNBT));

        // 控制台输出（国际化）
        LogUtil.debug(LangUtil.get("debug.player-view-item", player.getName(), itemName));
        Bukkit.getConsoleSender().sendMessage(LangUtil.get("item.name", itemName));
        Bukkit.getConsoleSender().sendMessage(LangUtil.get("item.type", itemType));
        Bukkit.getConsoleSender().sendMessage(LangUtil.get("item.sub-id", durability));
        Bukkit.getConsoleSender().sendMessage(LangUtil.get("item.max-stack", maxStackSize));

        if (!itemStack.getEnchantments().isEmpty()) {
            Bukkit.getConsoleSender().sendMessage(LangUtil.get("item.enchantments"));
            for (Enchantment enchantment : itemStack.getEnchantments().keySet()) {
                int level = itemStack.getEnchantments().get(enchantment);
                Bukkit.getConsoleSender().sendMessage(LangUtil.get("item.enchantment-entry", IEnchantment.getName(enchantment), level));
            }
        }

        Bukkit.getConsoleSender().sendMessage(LangUtil.get("item.nbt", itemNBT));
    }

    /**
     * 发送方块信息到玩家和控制台
     *
     * @param player 玩家
     * @param block  方块
     */
    private void sendBlockInfo(Player player, Block block) {
        String blockType = block.getType().toString();
        byte blockData = block.getData();
        String biome = block.getBiome().toString();
        String blockNBT = getBlockNBTSafe(block);

        player.sendMessage(LangUtil.get("block.type", blockType));
        player.sendMessage(LangUtil.get("block.data", blockData));
        player.sendMessage(LangUtil.get("block.biome", biome));
        player.sendMessage(LangUtil.get("block.nbt", blockNBT));

        // 控制台输出（国际化）
        LogUtil.debug(LangUtil.get("debug.player-view-block", player.getName(), blockType));
        Bukkit.getConsoleSender().sendMessage(LangUtil.get("block.type", blockType));
        Bukkit.getConsoleSender().sendMessage(LangUtil.get("block.data", blockData));
        Bukkit.getConsoleSender().sendMessage(LangUtil.get("block.biome", biome));
        Bukkit.getConsoleSender().sendMessage(LangUtil.get("block.nbt", blockNBT));
    }

    /**
     * 发送实体信息到玩家和控制台
     *
     * @param player 玩家
     * @param entity 实体
     */
    private void sendEntityInfo(Player player, Entity entity) {
        Location location = entity.getLocation();
        Vector vector = location.getDirection();

        String entityName = IEntity.getName(entity);
        String saveName = IEntity.getSaveName(entity);
        String entityType = entity.getType().toString();
        String uuid = entity.getUniqueId().toString();
        String locationStr = location.getWorld().getName() + " : " +
                IDouble.shortenDouble(location.getX(), 2) + " " +
                IDouble.shortenDouble(location.getY(), 2) + " " +
                IDouble.shortenDouble(location.getZ(), 2) + " " +
                IDouble.shortenDouble(location.getYaw(), 2) + " " +
                IDouble.shortenDouble(location.getPitch(), 2);
        String vectorStr = IDouble.shortenDouble(vector.getX(), 2) + " " +
                IDouble.shortenDouble(vector.getY(), 2) + " " +
                IDouble.shortenDouble(vector.getZ(), 2);
        String entityNBT = getEntityNBTSafe(entity);

        player.sendMessage(LangUtil.get("entity.name", entityName));
        player.sendMessage(LangUtil.get("entity.save-name", saveName));
        player.sendMessage(LangUtil.get("entity.type", entityType));
        player.sendMessage(LangUtil.get("entity.uuid", uuid));
        player.sendMessage(LangUtil.get("entity.location", locationStr));
        player.sendMessage(LangUtil.get("entity.vector", vectorStr));
        player.sendMessage(LangUtil.get("entity.nbt", entityNBT));

        // 控制台输出（国际化）
        LogUtil.debug(LangUtil.get("debug.player-view-entity", player.getName(), entityName));
        Bukkit.getConsoleSender().sendMessage(LangUtil.get("entity.name", entityName));
        Bukkit.getConsoleSender().sendMessage(LangUtil.get("entity.save-name", saveName));
        Bukkit.getConsoleSender().sendMessage(LangUtil.get("entity.type", entityType));
        Bukkit.getConsoleSender().sendMessage(LangUtil.get("entity.uuid", uuid));
        Bukkit.getConsoleSender().sendMessage(LangUtil.get("entity.location", locationStr));
        Bukkit.getConsoleSender().sendMessage(LangUtil.get("entity.vector", vectorStr));
        Bukkit.getConsoleSender().sendMessage(LangUtil.get("entity.nbt", entityNBT));
    }

    /**
     * 发送物品栏信息到玩家和控制台
     *
     * @param player 玩家
     * @param evt    物品栏点击事件
     */
    private void sendInventoryInfo(Player player, InventoryClickEvent evt) {
        String clickType = evt.getClick().toString();
        String title = evt.getView().getTitle();
        String inventoryType = evt.getClickedInventory() != null ?
                evt.getClickedInventory().getType().toString() : "Unknown";
        int size = evt.getClickedInventory() != null ?
                evt.getClickedInventory().getSize() : 0;
        int slot = evt.getSlot();
        int rawSlot = evt.getRawSlot();

        player.sendMessage(LangUtil.get("inventory.click-type", clickType));
        player.sendMessage(LangUtil.get("inventory.title", title));
        player.sendMessage(LangUtil.get("inventory.type", inventoryType));
        player.sendMessage(LangUtil.get("inventory.size", size));
        player.sendMessage(LangUtil.get("inventory.slot", slot));
        player.sendMessage(LangUtil.get("inventory.raw-slot", rawSlot));

        Bukkit.getConsoleSender().sendMessage(LangUtil.get("inventory.click-type", clickType));
        Bukkit.getConsoleSender().sendMessage(LangUtil.get("inventory.title", title));
        Bukkit.getConsoleSender().sendMessage(LangUtil.get("inventory.type", inventoryType));
        Bukkit.getConsoleSender().sendMessage(LangUtil.get("inventory.size", size));
        Bukkit.getConsoleSender().sendMessage(LangUtil.get("inventory.slot", slot));
        Bukkit.getConsoleSender().sendMessage(LangUtil.get("inventory.raw-slot", rawSlot));
    }

    /**
     * 安全获取方块NBT
     *
     * @param block 方块
     * @return NBT字符串
     */
    private String getBlockNBTSafe(Block block) {
        try {
            return new NBTTileEntity(block.getState()).toString();
        } catch (Exception e) {
            return LangUtil.get("block.nbt-none");
        }
    }

    /**
     * 安全获取实体NBT
     *
     * @param entity 实体
     * @return NBT字符串
     */
    private String getEntityNBTSafe(Entity entity) {
        try {
            return new NBTEntity(entity).toString();
        } catch (Exception e) {
            return LangUtil.get("entity.nbt-fail", e.getMessage());
        }
    }
}