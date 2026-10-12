package pers.tany.yukinoaapi.interfacepart.player;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.inventory.ItemStack;
import pers.tany.yukinoaapi.interfacepart.builder.IItemBuilder;
import pers.tany.yukinoaapi.interfacepart.item.IItem;
import pers.tany.yukinoaapi.interfacepart.other.IString;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * 玩家操作相关
 */
public interface IPlayer {

    /**
     * 给予玩家经验值，如果为负数则扣除，可降级
     *
     * @param player 涉及的玩家
     * @param exp    经验值
     */
    static void giveExp(Player player, int exp) {
        int level = player.getLevel();
        int toNextLevel = getUpgradeExp(level);
        int floatExp = (int) (player.getExp() * toNextLevel) + exp;
        while (floatExp >= toNextLevel) {
            floatExp -= toNextLevel;
            toNextLevel = getUpgradeExp(++level);
        }
        while (floatExp < 0) {
            floatExp += (toNextLevel = getUpgradeExp(--level));
            if (level < 0) {
                level = floatExp = 0;
            }
        }
        player.setLevel(level);
        player.setExp((float) floatExp / toNextLevel);
        // 约 21864 级起总经验超出 int（getTotalExp 已饱和到 MAX_VALUE），int 相加会溢出成负数，
        // 1.20 的 setTotalExperience 拒收负数直接抛异常；按 long 算再夹到 MAX_VALUE，与原版 giveExperiencePoints 一致
        player.setTotalExperience((int) Math.min((long) getTotalExp(level) + floatExp, Integer.MAX_VALUE));
    }

    /**
     * 获取升级所需的经验
     *
     * @param level 等级
     * @return 升级所需的经验
     */
    static int getUpgradeExp(int level) {
        return level < 16 ? level * 2 + 7 : level < 30 ? level * 5 - 38 : level * 9 - 158;
    }

    /**
     * 获取这个等级的经验
     *
     * @param level 等级
     * @return 经验
     */
    static int getTotalExp(int level) {
        return (int) (level < 17 ? level * (level + 6) : level < 31 ? level * (level * 2.5 - 40.5) + 360 : level * (level * 4.5 - 162.5) + 2220);
    }

    /**
     * 给予玩家物品，如果溢出，则在脚下生成掉落物
     *
     * @param player    涉及的玩家
     * @param itemStack 物品
     */
    static void giveItem(Player player, ItemStack itemStack) {
        if (!IItem.isEmpty(itemStack)) {
            for (ItemStack i : player.getInventory().addItem(itemStack).values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), itemStack);
            }
        }
    }

    /**
     * 给予玩家物品，如果溢出，则在脚下生成掉落物
     *
     * @param player      涉及的玩家
     * @param itemBuilder 物品
     */
    static void giveItem(Player player, IItemBuilder itemBuilder) {
        giveItem(player, itemBuilder.getItemStack());
    }

    /**
     * 给予玩家物品，如果溢出，则在脚下生成掉落物
     *
     * @param player       涉及的玩家
     * @param itemBuilders 物品
     */
    static void giveItem(Player player, IItemBuilder... itemBuilders) {
        for (IItemBuilder itemBuilder : itemBuilders) {
            giveItem(player, itemBuilder.getItemStack());
        }
    }

    /**
     * 给予玩家物品，如果溢出，则在脚下生成掉落物
     *
     * @param player     涉及的玩家
     * @param itemStacks 物品
     */
    static void giveItem(Player player, ItemStack... itemStacks) {
        for (ItemStack itemStack : itemStacks) {
            if (!IItem.isEmpty(itemStack)) {
                giveItem(player, itemStack);
            }
        }
    }

    /**
     * 把玩家传送到某个世界的某个区块
     *
     * @param player 涉及的玩家
     * @param world  涉及的世界
     * @param chunk  涉及的区块
     */
    static void telePortChunk(Player player, World world, Chunk chunk) {
        Location location = player.getLocation();
        location.setWorld(world);
        double x = chunk.getX() * 16;
        double z = chunk.getZ() * 16;
        location.setX(x);
        location.setZ(z);
        location.setY(world.getHighestBlockAt(location).getY());
        player.teleport(location);
    }

    /**
     * 获取所有离线玩家的OfflinePlayer对象
     *
     * @return 离线玩家组
     */
    
    static HashSet<OfflinePlayer> getOfflinePlayers() {
        HashSet<OfflinePlayer> offlinePlayers = new HashSet<>();
        for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
            try {
                Objects.requireNonNull(offlinePlayer.getName());
                if (!offlinePlayer.isOnline()) {
                    offlinePlayers.add(offlinePlayer);
                }
            } catch (Exception ignored) {

            }
        }
        return offlinePlayers;
    }

    /**
     * 检测此名称/UUID的玩家是否存在过服务器
     *
     * @param mark 玩家名称或UUID
     * @return 如果玩家存在，则返回true，否则false
     */
    static boolean hasPlayer(String mark) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getName().equals(mark)) {
                return true;
            }
        }
        for (OfflinePlayer offlinePlayers : IPlayer.getOfflinePlayers()) {
            if (offlinePlayers.getName().equals(mark)) {
                return true;
            }
        }
        try {
            UUID uuid = UUID.fromString(mark);
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getUniqueId().equals(uuid)) {
                    return true;
                }
            }
            for (OfflinePlayer offlinePlayers : IPlayer.getOfflinePlayers()) {
                if (offlinePlayers.getUniqueId().equals(uuid)) {
                    return true;
                }
            }
        } catch (IllegalArgumentException ignored) {

        }
        return false;
    }

    /**
     * 检测此名称/UUID的玩家是否在线
     *
     * @param mark 玩家名称或UUID
     * @return 如果玩家在线，则返回true，否则false
     */
    static boolean hasOnlinePlayer(String mark) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getName().equals(mark)) {
                return true;
            }
        }
        try {
            UUID uuid = UUID.fromString(mark);
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getUniqueId().equals(uuid)) {
                    return true;
                }
            }
        } catch (IllegalArgumentException ignored) {

        }
        return false;
    }

    /**
     * 检测此名称/UUID的玩家是否离线
     *
     * @param mark 玩家名称或UUID
     * @return 如果玩家离线，则返回true，否则false
     */
    static boolean hasOfflinePlayer(String mark) {
        for (OfflinePlayer offlinePlayers : IPlayer.getOfflinePlayers()) {
            if (offlinePlayers.getName().equals(mark)) {
                return true;
            }
        }
        try {
            UUID uuid = UUID.fromString(mark);
            for (OfflinePlayer offlinePlayers : IPlayer.getOfflinePlayers()) {
                if (offlinePlayers.getUniqueId().equals(uuid)) {
                    return true;
                }
            }
        } catch (IllegalArgumentException ignored) {

        }
        return false;
    }

    /**
     * 使玩家执行命令
     *
     * @param player  执行命令的玩家
     * @param command 执行的命令
     * @return 是否执行成功
     */
    static boolean useCommand(Player player, String command) {
        return useCommand(player, command, false);
    }

    /**
     * 使玩家执行命令
     *
     * @param player  执行命令的玩家
     * @param command 执行的命令
     * @param op      是否以OP身份执行
     * @return 是否执行成功
     */
    static boolean useCommand(Player player, String command, boolean op) {
        if (op) {
            boolean isOp = player.isOp();
            player.setOp(true);
            boolean use = Bukkit.dispatchCommand(player, command);
            player.setOp(isOp);
            return use;
        } else {
            return Bukkit.dispatchCommand(player, command);
        }
    }

    /**
     * 踢出玩家，并显示信息
     *
     * @param player      执行命令的玩家
     * @param kickMessage 踢出的信息
     */
    static void kickPlayer(Player player, String... kickMessage) {
        @Nullable String message = "";
        for (int i = 0; i < kickMessage.length; i++) {
            message += IString.color(kickMessage[i]);
            if (i < kickMessage.length - 1) {
                message += "\n";
            }
        }
        player.kickPlayer(message);
    }

    /**
     * 踢出玩家，并显示信息
     *
     * @param player      执行命令的玩家
     * @param kickMessage 踢出的信息
     */
    static void kickPlayer(Player player, List<String> kickMessage) {
        @Nullable String message = "";
        for (int i = 0; i < kickMessage.size(); i++) {
            message += IString.color(kickMessage.get(i));
            if (i < kickMessage.size() - 1) {
                message += "\n";
            }
        }
        player.kickPlayer(message);
    }

    /**
     * 踢出玩家并显示信息
     *
     * @param evt         涉及事件
     * @param kickMessage 踢出的信息
     */
    static void kickPlayer(AsyncPlayerPreLoginEvent evt, String... kickMessage) {
        evt.setLoginResult(AsyncPlayerPreLoginEvent.Result.KICK_OTHER);
        @Nullable String message = "";
        for (int i = 0; i < kickMessage.length; i++) {
            message += IString.color(kickMessage[i]);
            if (i < kickMessage.length - 1) {
                message += "\n";
            }
        }
        evt.setKickMessage(message);
    }

    /**
     * 踢出玩家并显示信息
     *
     * @param evt         涉及事件
     * @param kickMessage 踢出的信息
     */
    static void kickPlayer(AsyncPlayerPreLoginEvent evt, List<String> kickMessage) {
        evt.setLoginResult(AsyncPlayerPreLoginEvent.Result.KICK_OTHER);
        @Nullable String message = "";
        for (int i = 0; i < kickMessage.size(); i++) {
            message += IString.color(kickMessage.get(i));
            if (i < kickMessage.size() - 1) {
                message += "\n";
            }
        }
        evt.setKickMessage(message);
    }
}

