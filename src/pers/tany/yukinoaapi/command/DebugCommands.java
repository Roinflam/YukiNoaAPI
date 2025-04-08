package pers.tany.yukinoaapi.command;


import de.tr7zw.nbtapi.NBTEntity;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import pers.tany.yukinoaapi.Main;
import pers.tany.yukinoaapi.interfacepart.configuration.IConfig;
import pers.tany.yukinoaapi.interfacepart.entity.IEntity;
import pers.tany.yukinoaapi.interfacepart.other.IDouble;

import java.util.HashSet;
import java.util.Set;

public class DebugCommands implements CommandExecutor {
    
    public static final Set<String> DEBUG = new HashSet<>();

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage("§c此插件只能为管理员使用！");
            return true;
        }
        if (args.length == 1) {
            if (args[0].equalsIgnoreCase("reload")) {
                if (!sender.isOp()) {
                    sender.sendMessage("§c您没有权限执行此命令");
                    return true;
                }
                Main.config = IConfig.loadConfig(Main.plugin, "", "config");
                sender.sendMessage("§a重载成功");
                return true;
            }
        }
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("showNBT")) {
                if (Bukkit.getPlayerExact(args[1]) == null) {
                    sender.sendMessage("§c玩家未在线！");
                    return true;
                }
                Player player = Bukkit.getPlayerExact(args[1]);
                Location location = player.getLocation();
                Vector vector = location.getDirection();
                player.sendMessage("§a实体名称： §f" + IEntity.getName(player));
                player.sendMessage("§a实体储存名： §f" + IEntity.getSaveName(player));
                player.sendMessage("§a实体Type： §f" + player.getType().toString());
                player.sendMessage("§a实体UUID： §f" + player.getUniqueId().toString());
                player.sendMessage("§a实体坐标： §f" + location.getWorld().getName() + " : " + IDouble.shortenDouble(location.getX(), 2) + " " + IDouble.shortenDouble(location.getY(), 2) + " " + IDouble.shortenDouble(location.getYaw(), 2) + " " + IDouble.shortenDouble(location.getPitch(), 2));
                player.sendMessage("§a实体向量： §f" + IDouble.shortenDouble(vector.getX(), 2) + " " + IDouble.shortenDouble(vector.getY(), 2) + " " + IDouble.shortenDouble(vector.getZ(), 2));
                player.sendMessage("§a实体NBT： §f" + new NBTEntity(player));
                Bukkit.getConsoleSender().sendMessage("§a实体名称： §f" + IEntity.getName(player));
                Bukkit.getConsoleSender().sendMessage("§a实体储存名： §f" + IEntity.getSaveName(player));
                Bukkit.getConsoleSender().sendMessage("§a实体Type： §f" + player.getType().toString());
                Bukkit.getConsoleSender().sendMessage("§a实体UUID： §f" + player.getUniqueId().toString());
                Bukkit.getConsoleSender().sendMessage("§a实体坐标： §f" + location.getWorld().getName() + " : " + IDouble.shortenDouble(location.getX(), 2) + " " + IDouble.shortenDouble(location.getY(), 2) + " " + IDouble.shortenDouble(location.getYaw(), 2) + " " + IDouble.shortenDouble(location.getPitch(), 2));
                Bukkit.getConsoleSender().sendMessage("§a实体向量： §f" + IDouble.shortenDouble(vector.getX(), 2) + " " + IDouble.shortenDouble(vector.getY(), 2) + " " + IDouble.shortenDouble(vector.getZ(), 2));
                Bukkit.getConsoleSender().sendMessage("§a实体NBT： §f" + new NBTEntity(player));
                return true;
            }
            if (args[0].equalsIgnoreCase("debug")) {
                if (DEBUG.remove(args[1])) {
                    sender.sendMessage("§a成功关闭debug模式！");
                } else {
                    DEBUG.add(args[1]);
                    sender.sendMessage("§a成功开启debug模式！");
                }
                return true;
            }
        }
        if (sender instanceof ConsoleCommandSender) {
            sender.sendMessage("§f/yn debug 玩家  §7开关debug模式");
            return true;
        }
        Player player = (Player) sender;
        if (args.length == 1) {
            if (args[0].equalsIgnoreCase("debug")) {
                if (DEBUG.remove(player.getName())) {
                    player.sendMessage("§a成功关闭debug模式！");
                } else {
                    DEBUG.add(player.getName());
                    player.sendMessage("§a成功开启debug模式！");
                }
                return true;
            }
            if (args[0].equalsIgnoreCase("showNBT")) {
                Location location = player.getLocation();
                Vector vector = location.getDirection();
                player.sendMessage("§a实体名称： §f" + IEntity.getName(player));
                player.sendMessage("§a实体储存名： §f" + IEntity.getSaveName(player));
                player.sendMessage("§a实体Type： §f" + player.getType().toString());
                player.sendMessage("§a实体UUID： §f" + player.getUniqueId().toString());
                player.sendMessage("§a实体坐标： §f" + location.getWorld().getName() + " : " + IDouble.shortenDouble(location.getX(), 2) + " " + IDouble.shortenDouble(location.getY(), 2) + " " + IDouble.shortenDouble(location.getYaw(), 2) + " " + IDouble.shortenDouble(location.getPitch(), 2));
                player.sendMessage("§a实体向量： §f" + IDouble.shortenDouble(vector.getX(), 2) + " " + IDouble.shortenDouble(vector.getY(), 2) + " " + IDouble.shortenDouble(vector.getZ(), 2));
                player.sendMessage("§a实体NBT： §f" + new NBTEntity(player));
                Bukkit.getConsoleSender().sendMessage("§a实体名称： §f" + IEntity.getName(player));
                Bukkit.getConsoleSender().sendMessage("§a实体储存名： §f" + IEntity.getSaveName(player));
                Bukkit.getConsoleSender().sendMessage("§a实体Type： §f" + player.getType().toString());
                Bukkit.getConsoleSender().sendMessage("§a实体UUID： §f" + player.getUniqueId().toString());
                Bukkit.getConsoleSender().sendMessage("§a实体坐标： §f" + location.getWorld().getName() + " : " + IDouble.shortenDouble(location.getX(), 2) + " " + IDouble.shortenDouble(location.getY(), 2) + " " + IDouble.shortenDouble(location.getYaw(), 2) + " " + IDouble.shortenDouble(location.getPitch(), 2));
                Bukkit.getConsoleSender().sendMessage("§a实体向量： §f" + IDouble.shortenDouble(vector.getX(), 2) + " " + IDouble.shortenDouble(vector.getY(), 2) + " " + IDouble.shortenDouble(vector.getZ(), 2));
                Bukkit.getConsoleSender().sendMessage("§a实体NBT： §f" + new NBTEntity(player));
                return true;
            }
        }
        player.sendMessage("§f/yn debug  §7开关debug模式");
        player.sendMessage("§f/yn showNBT [玩家]  §7显示玩家nbt");
        return true;
    }
}
