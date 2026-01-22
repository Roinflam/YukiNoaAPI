package pers.tany.yukinoaapi.command;

import com.google.common.reflect.ClassPath;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import pers.tany.yukinoaapi.Main;
import pers.tany.yukinoaapi.config.LanguageManager;
import pers.tany.yukinoaapi.interfacepart.player.IMessage;
import pers.tany.yukinoaapi.util.LogUtil;
import pers.tany.yukinoaapi.util.NMSUtil;

import java.util.*;

/**
 * Debug命令处理类
 *
 * @author tany
 * @since 3.0.0
 */
public class DebugCommands implements CommandExecutor, TabCompleter {

    /**
     * 存储开启Debug模式的玩家名称
     */
    public static final Set<String> DEBUG = new HashSet<>();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        LanguageManager lang = ((Main) Main.plugin).getLanguageManager();

        if (args.length == 0) {
            sendHelp(sender, lang);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "debug":
                handleDebugCommand(sender, lang);
                break;
            case "classes":
                handleClassesCommand(sender, lang);
                break;
            case "reload":
                handleReloadCommand(sender, lang);
                break;
            case "help":
            default:
                sendHelp(sender, lang);
                break;
        }

        return true;
    }

    /**
     * 处理 debug 子命令
     *
     * @param sender 命令发送者
     * @param lang   语言管理器
     */
    private void handleDebugCommand(CommandSender sender, LanguageManager lang) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(lang.getMessage("common.player-only"));
            return;
        }

        Player player = (Player) sender;
        String playerName = player.getName();

        IMessage.sendActionBar(player, "§6666", 5);

        if (DEBUG.contains(playerName)) {
            DEBUG.remove(playerName);
            player.sendMessage(lang.getMessage("debug.disabled"));
        } else {
            DEBUG.add(playerName);
            player.sendMessage(lang.getMessage("debug.enabled"));
        }
    }

    /**
     * 处理 classes 子命令
     *
     * @param sender 命令发送者
     * @param lang   语言管理器
     */
    private void handleClassesCommand(CommandSender sender, LanguageManager lang) {
        sender.sendMessage(lang.getMessage("debug.scanning"));
        LogUtil.info(lang.getMessage("main.scan-start"));

        String[] basePaths = {
                "net.minecraft",
                "net.minecraft.server",
                "net.minecraft.world",
                "net.minecraft.world.item",
                "org.bukkit.craftbukkit"
        };

        for (String basePath : basePaths) {
            try {
                LogUtil.info(lang.getMessage("main.scan-package", basePath));
                ClassLoader classLoader = NMSUtil.class.getClassLoader();
                ClassPath classPath = ClassPath.from(classLoader);

                for (ClassPath.ClassInfo classInfo : classPath.getTopLevelClasses()) {
                    if (classInfo.getName().startsWith(basePath)) {
                        try {
                            LogUtil.debug(lang.getMessage("main.class-found", classInfo.getName()));
                            Class.forName(classInfo.getName());
                            LogUtil.debug(lang.getMessage("main.class-load-success"));
                        } catch (Throwable e) {
                            LogUtil.debug(lang.getMessage("main.class-load-fail", e.getMessage()));
                        }
                    }
                }
            } catch (Exception e) {
                LogUtil.error(lang.getMessage("main.scan-error", basePath), e);
            }
        }
    }

    /**
     * 处理 reload 子命令
     *
     * @param sender 命令发送者
     * @param lang   语言管理器
     */
    private void handleReloadCommand(CommandSender sender, LanguageManager lang) {
        try {
            Main.reload();
            sender.sendMessage(lang.getMessage("common.reload-success"));
        } catch (Exception e) {
            sender.sendMessage(lang.getMessage("common.reload-fail"));
            LogUtil.error(lang.getMessage("common.reload-fail"), e);
        }
    }

    /**
     * 发送帮助信息
     *
     * @param sender 命令发送者
     * @param lang   语言管理器
     */
    private void sendHelp(CommandSender sender, LanguageManager lang) {
        sender.sendMessage(lang.getMessage("debug.help-title"));
        sender.sendMessage(lang.getMessage("debug.help-debug"));
        sender.sendMessage(lang.getMessage("debug.help-classes"));
        sender.sendMessage(lang.getMessage("debug.help-reload"));
        sender.sendMessage(lang.getMessage("debug.help-footer"));
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