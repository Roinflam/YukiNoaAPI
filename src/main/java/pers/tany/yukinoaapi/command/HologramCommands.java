package pers.tany.yukinoaapi.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import pers.tany.yukinoaapi.Main;
import pers.tany.yukinoaapi.config.LanguageManager;
import pers.tany.yukinoaapi.hologram.HologramManager;
import pers.tany.yukinoaapi.hologram.IHologram;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 全息图命令处理
 *
 * @author Roinflam
 * @since 3.1.0
 */
public class HologramCommands implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        LanguageManager lang = ((Main) Main.plugin).getLanguageManager();

        if (!(sender instanceof Player)) {
            sender.sendMessage(lang.getMessage("common.player-only"));
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            sendHelp(player, lang);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "create":
                handleCreate(player, args, lang);
                break;
            case "delete":
                handleDelete(player, args, lang);
                break;
            case "addline":
                handleAddLine(player, args, lang);
                break;
            case "setline":
                handleSetLine(player, args, lang);
                break;
            case "removeline":
                handleRemoveLine(player, args, lang);
                break;
            case "insertline":
                handleInsertLine(player, args, lang);
                break;
            case "clear":
                handleClear(player, args, lang);
                break;
            case "list":
                handleList(player, lang);
                break;
            case "info":
                handleInfo(player, args, lang);
                break;
            case "teleport":
            case "tp":
                handleTeleport(player, args, lang);
                break;
            case "reload":
                handleReload(player, lang);
                break;
            default:
                sendHelp(player, lang);
                break;
        }

        return true;
    }

    /**
     * 处理创建命令（支持多行文本,使用 | 分隔）
     * 用法：/hologram create <id> [行1 | 行2 | 行3 ...]
     *
     * 多行创建时，每5 tick添加一行，避免一次性创建导致的显示bug
     */
    private void handleCreate(Player player, String[] args, LanguageManager lang) {
        if (args.length < 2) {
            player.sendMessage(lang.getMessage("hologram.usage-create"));
            return;
        }

        String id = args[1];

        // 检查全息图是否已存在
        if (HologramManager.getInstance().getHologram(id) != null) {
            player.sendMessage(lang.getMessage("hologram.already-exists", id));
            return;
        }

        // 使用玩家眼睛高度作为全息图起始位置
        IHologram hologram = HologramManager.getInstance().createHologram(
                id,
                player.getEyeLocation(),
                null // 先不添加文本
        );

        // 解析多行文本（如果提供）
        if (args.length > 2) {
            // 合并所有参数为一个字符串
            String fullText = String.join(" ", Arrays.copyOfRange(args, 2, args.length));

            // 使用 | 分隔多行
            String[] lines = fullText.split("\\|");

            // 过滤空行并收集所有有效行
            List<String> validLines = new ArrayList<>();
            for (String line : lines) {
                String trimmedLine = line.trim();
                if (!trimmedLine.isEmpty()) {
                    validLines.add(trimmedLine);
                }
            }

            // 如果没有有效行，创建空全息图
            if (validLines.isEmpty()) {
                hologram.show();
                player.sendMessage(lang.getMessage("hologram.created"));
                return;
            }

            // 使用定时任务逐行添加（每5 tick添加一行）
            new BukkitRunnable() {
                /** 当前添加到第几行 */
                private int currentIndex = 0;

                @Override
                public void run() {
                    // 如果全息图已被删除，取消任务
                    if (hologram.isDeleted()) {
                        cancel();
                        return;
                    }

                    // 如果所有行都已添加完成
                    if (currentIndex >= validLines.size()) {
                        // 保存数据
                        HologramManager.getInstance().saveHolograms();

                        // 发送完成消息
                        player.sendMessage(lang.getMessage("hologram.created-with-lines"));

                        cancel();
                        return;
                    }

                    // 添加当前行（会自动调用show和updateAllPositions）
                    hologram.appendLine(validLines.get(currentIndex));
                    currentIndex++;
                }
            }.runTaskTimer(Main.plugin, 0L, 5L); // 立即开始，每5 tick执行一次

        } else {
            // 没有提供初始文本，创建空全息图
            hologram.show();
            player.sendMessage(lang.getMessage("hologram.created"));
        }
    }

    /**
     * 处理删除命令
     */
    private void handleDelete(Player player, String[] args, LanguageManager lang) {
        if (args.length < 2) {
            player.sendMessage(lang.getMessage("hologram.usage-delete"));
            return;
        }

        String id = args[1];
        IHologram hologram = HologramManager.getInstance().getHologram(id);

        if (hologram == null) {
            player.sendMessage(lang.getMessage("hologram.not-found", id));
            return;
        }

        HologramManager.getInstance().deleteHologram(id);
        player.sendMessage(lang.getMessage("hologram.deleted", id));
    }

    /**
     * 处理添加行命令
     */
    private void handleAddLine(Player player, String[] args, LanguageManager lang) {
        if (args.length < 3) {
            player.sendMessage(lang.getMessage("hologram.usage-addline"));
            return;
        }

        String id = args[1];
        IHologram hologram = HologramManager.getInstance().getHologram(id);

        if (hologram == null) {
            player.sendMessage(lang.getMessage("hologram.not-found", id));
            return;
        }

        String text = String.join(" ", Arrays.copyOfRange(args, 2, args.length));

        hologram.appendLine(text);
        HologramManager.getInstance().saveHolograms();

        player.sendMessage(lang.getMessage("hologram.line-added", id, text));
    }

    /**
     * 处理设置行命令
     */
    private void handleSetLine(Player player, String[] args, LanguageManager lang) {
        if (args.length < 4) {
            player.sendMessage(lang.getMessage("hologram.usage-setline"));
            return;
        }

        String id = args[1];
        IHologram hologram = HologramManager.getInstance().getHologram(id);

        if (hologram == null) {
            player.sendMessage(lang.getMessage("hologram.not-found", id));
            return;
        }

        try {
            int index = Integer.parseInt(args[2]);
            String text = String.join(" ", Arrays.copyOfRange(args, 3, args.length));

            hologram.setLine(index, text);
            HologramManager.getInstance().saveHolograms();

            player.sendMessage(lang.getMessage("hologram.line-set", id, String.valueOf(index), text));
        } catch (NumberFormatException e) {
            player.sendMessage(lang.getMessage("hologram.invalid-number"));
        }
    }

    /**
     * 处理删除行命令
     */
    private void handleRemoveLine(Player player, String[] args, LanguageManager lang) {
        if (args.length < 3) {
            player.sendMessage(lang.getMessage("hologram.usage-removeline"));
            return;
        }

        String id = args[1];
        IHologram hologram = HologramManager.getInstance().getHologram(id);

        if (hologram == null) {
            player.sendMessage(lang.getMessage("hologram.not-found", id));
            return;
        }

        try {
            int index = Integer.parseInt(args[2]);
            hologram.removeLine(index);
            HologramManager.getInstance().saveHolograms();

            player.sendMessage(lang.getMessage("hologram.line-removed", id, String.valueOf(index)));
        } catch (NumberFormatException e) {
            player.sendMessage(lang.getMessage("hologram.invalid-number"));
        }
    }

    /**
     * 处理插入行命令
     */
    private void handleInsertLine(Player player, String[] args, LanguageManager lang) {
        if (args.length < 4) {
            player.sendMessage(lang.getMessage("hologram.usage-insertline"));
            return;
        }

        String id = args[1];
        IHologram hologram = HologramManager.getInstance().getHologram(id);

        if (hologram == null) {
            player.sendMessage(lang.getMessage("hologram.not-found", id));
            return;
        }

        try {
            int index = Integer.parseInt(args[2]);
            String text = String.join(" ", Arrays.copyOfRange(args, 3, args.length));

            hologram.insertLine(index, text);
            HologramManager.getInstance().saveHolograms();

            player.sendMessage(lang.getMessage("hologram.line-inserted", id, String.valueOf(index), text));
        } catch (NumberFormatException e) {
            player.sendMessage(lang.getMessage("hologram.invalid-number"));
        }
    }

    /**
     * 处理清空命令
     */
    private void handleClear(Player player, String[] args, LanguageManager lang) {
        if (args.length < 2) {
            player.sendMessage(lang.getMessage("hologram.usage-clear"));
            return;
        }

        String id = args[1];
        IHologram hologram = HologramManager.getInstance().getHologram(id);

        if (hologram == null) {
            player.sendMessage(lang.getMessage("hologram.not-found", id));
            return;
        }

        hologram.clearLines();
        HologramManager.getInstance().saveHolograms();

        player.sendMessage(lang.getMessage("hologram.cleared", id));
    }

    /**
     * 处理列表命令
     */
    private void handleList(Player player, LanguageManager lang) {
        player.sendMessage(lang.getMessage("hologram.list-title"));

        if (HologramManager.getInstance().getAllHolograms().isEmpty()) {
            player.sendMessage(lang.getMessage("hologram.list-empty"));
            return;
        }

        for (IHologram hologram : HologramManager.getInstance().getAllHolograms()) {
            String status = hologram.isShown() ? "§a✔" : "§c✘";
            player.sendMessage(lang.getMessage("hologram.list-entry",
                    hologram.getId(),
                    String.valueOf(hologram.size()),
                    status
            ));
        }
    }

    /**
     * 处理信息命令
     */
    private void handleInfo(Player player, String[] args, LanguageManager lang) {
        if (args.length < 2) {
            player.sendMessage(lang.getMessage("hologram.usage-info"));
            return;
        }

        String id = args[1];
        IHologram hologram = HologramManager.getInstance().getHologram(id);

        if (hologram == null) {
            player.sendMessage(lang.getMessage("hologram.not-found", id));
            return;
        }

        player.sendMessage(lang.getMessage("hologram.info-title", id));
        player.sendMessage(lang.getMessage("hologram.info-location",
                hologram.getLocation().getWorld().getName(),
                String.valueOf(hologram.getLocation().getBlockX()),
                String.valueOf(hologram.getLocation().getBlockY()),
                String.valueOf(hologram.getLocation().getBlockZ())
        ));
        player.sendMessage(lang.getMessage("hologram.info-lines", String.valueOf(hologram.size())));

        if (hologram.size() > 0) {
            player.sendMessage(lang.getMessage("hologram.info-content"));
            List<String> lines = hologram.getLines();
            for (int i = 0; i < lines.size(); i++) {
                player.sendMessage(lang.getMessage("hologram.info-line", String.valueOf(i), lines.get(i)));
            }
        }
    }

    /**
     * 处理传送命令
     */
    private void handleTeleport(Player player, String[] args, LanguageManager lang) {
        if (args.length < 2) {
            player.sendMessage(lang.getMessage("hologram.usage-teleport"));
            return;
        }

        String id = args[1];
        IHologram hologram = HologramManager.getInstance().getHologram(id);

        if (hologram == null) {
            player.sendMessage(lang.getMessage("hologram.not-found", id));
            return;
        }

        player.teleport(hologram.getLocation());
        player.sendMessage(lang.getMessage("hologram.teleported", id));
    }

    /**
     * 处理重载命令
     */
    private void handleReload(Player player, LanguageManager lang) {
        try {
            HologramManager.getInstance().reload();
            player.sendMessage(lang.getMessage("hologram.reload-success"));
        } catch (Exception e) {
            player.sendMessage(lang.getMessage("hologram.reload-fail"));
        }
    }

    /**
     * 发送帮助信息
     */
    private void sendHelp(Player player, LanguageManager lang) {
        player.sendMessage(lang.getMessage("hologram.help-title"));
        player.sendMessage(lang.getMessage("hologram.help-create"));
        player.sendMessage(lang.getMessage("hologram.help-delete"));
        player.sendMessage(lang.getMessage("hologram.help-addline"));
        player.sendMessage(lang.getMessage("hologram.help-setline"));
        player.sendMessage(lang.getMessage("hologram.help-removeline"));
        player.sendMessage(lang.getMessage("hologram.help-insertline"));
        player.sendMessage(lang.getMessage("hologram.help-clear"));
        player.sendMessage(lang.getMessage("hologram.help-list"));
        player.sendMessage(lang.getMessage("hologram.help-info"));
        player.sendMessage(lang.getMessage("hologram.help-teleport"));
        player.sendMessage(lang.getMessage("hologram.help-reload"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subs = Arrays.asList(
                    "create", "delete", "addline", "setline", "removeline",
                    "insertline", "clear", "list", "info", "teleport", "reload"
            );
            return subs.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length == 2 && !args[0].equalsIgnoreCase("create") && !args[0].equalsIgnoreCase("list")) {
            return HologramManager.getInstance().getAllHolograms().stream()
                    .map(IHologram::getId)
                    .filter(id -> id.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return completions;
    }
}