package pers.tany.yukinoaapi.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import pers.tany.yukinoaapi.Main;
import pers.tany.yukinoaapi.util.LangUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Debug命令处理类
 *
 * @author tany
 * @since 1.0.0
 */
public class DebugCommands implements CommandExecutor, TabCompleter {

    /**
     * 存储开启Debug模式的玩家名称
     */
    public static final Set<String> DEBUG = new HashSet<>();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "debug":
                handleDebugCommand(sender);
                break;
            case "classes":
                handleClassesCommand(sender);
                break;
            case "reload":
                handleReloadCommand(sender);
                break;
            case "help":
            default:
                sendHelp(sender);
                break;
        }

        return true;
    }

    /**
     * 处理 debug 子命令
     *
     * @param sender 命令发送者
     */
    private void handleDebugCommand(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(LangUtil.get("common.player-only"));
            return;
        }

        Player player = (Player) sender;
        String playerName = player.getName();

        if (DEBUG.contains(playerName)) {
            DEBUG.remove(playerName);
            player.sendMessage(LangUtil.get("debug.disabled"));
        } else {
            DEBUG.add(playerName);
            player.sendMessage(LangUtil.get("debug.enabled"));
        }
    }

    /**
     * 处理 classes 子命令
     *
     * @param sender 命令发送者
     */
    private void handleClassesCommand(CommandSender sender) {
        sender.sendMessage(LangUtil.get("debug.scanning"));
        Main.printAllClasses();
    }

    /**
     * 处理 reload 子命令
     *
     * @param sender 命令发送者
     */
    private void handleReloadCommand(CommandSender sender) {
        try {
            Main.reload();
            sender.sendMessage(LangUtil.get("common.reload-success"));
        } catch (Exception e) {
            sender.sendMessage(LangUtil.get("common.reload-fail"));
        }
    }

    /**
     * 发送帮助信息
     *
     * @param sender 命令发送者
     */
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(LangUtil.get("debug.help-title"));
        sender.sendMessage(LangUtil.get("debug.help-debug"));
        sender.sendMessage(LangUtil.get("debug.help-classes"));
        sender.sendMessage(LangUtil.get("debug.help-reload"));
        sender.sendMessage(LangUtil.get("debug.help-footer"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("debug", "classes", "reload", "help");
            String input = args[0].toLowerCase();

            for (String sub : subCommands) {
                if (sub.startsWith(input)) {
                    completions.add(sub);
                }
            }
        }

        return completions;
    }
}