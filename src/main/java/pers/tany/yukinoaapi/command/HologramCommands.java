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
import pers.tany.yukinoaapi.hologram.HologramStyle;
import pers.tany.yukinoaapi.hologram.IHologram;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
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

        // 权限校验：本命令组全部仅限OP使用
        if (!sender.isOp()) {
            sender.sendMessage(lang.getMessage("common.no-permission"));
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(lang.getMessage("common.player-only"));
            return true;
        }

        Player player = (Player) sender;

        // 检查全息图系统是否可用（1.7-1.11 未安装HD时不可用）
        if (!HologramManager.getInstance().isEnabled()) {
            player.sendMessage(lang.getMessage("hologram.disabled-no-hd"));
            return true;
        }

        // 无参数时直接显示帮助
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
                handleList(player, args, lang);
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
                // 无效子命令也显示帮助
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
     * 来源标记为 "command"
     *
     * @param player 玩家
     * @param args   参数
     * @param lang   语言管理器
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

        // 使用玩家眼睛高度作为全息图起始位置，来源标记为 "command"
        IHologram hologram = HologramManager.getInstance().createHologram(
                id,
                player.getEyeLocation(),
                null,
                "command"
        );

        // createHologram 在系统不可用时可能返回 null
        if (hologram == null) {
            player.sendMessage(lang.getMessage("hologram.disabled-no-hd"));
            return;
        }

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
                player.sendMessage(lang.getMessage("hologram.created", id));
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

                        // 发送完成消息（传入ID和行数）
                        player.sendMessage(lang.getMessage("hologram.created-with-lines",
                                new Object[]{id, validLines.size()}));

                        cancel();
                        return;
                    }

                    // 添加当前行（会自动调用show和updateAllPositions）
                    hologram.appendLine(validLines.get(currentIndex));
                    currentIndex++;
                }
            }.runTaskTimer(Main.plugin, 0L, 5L);

        } else {
            // 没有提供初始文本，创建空全息图
            hologram.show();
            player.sendMessage(lang.getMessage("hologram.created", id));
        }
    }

    /**
     * 处理删除命令
     *
     * @param player 玩家
     * @param args   参数
     * @param lang   语言管理器
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
     *
     * @param player 玩家
     * @param args   参数
     * @param lang   语言管理器
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

        player.sendMessage(lang.getMessage("hologram.line-added", new Object[]{id, text}));
    }

    /**
     * 处理设置行命令
     *
     * @param player 玩家
     * @param args   参数
     * @param lang   语言管理器
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
     *
     * @param player 玩家
     * @param args   参数
     * @param lang   语言管理器
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

            player.sendMessage(lang.getMessage("hologram.line-removed", new Object[]{id, index}));
        } catch (NumberFormatException e) {
            player.sendMessage(lang.getMessage("hologram.invalid-number"));
        }
    }

    /**
     * 处理插入行命令
     *
     * @param player 玩家
     * @param args   参数
     * @param lang   语言管理器
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
     *
     * @param player 玩家
     * @param args   参数
     * @param lang   语言管理器
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
     * 处理列表命令（分类显示：命令创建 / 插件创建 / 未知来源）
     * 用法：/ynhd list [command|plugin|all]
     *
     * @param player 玩家
     * @param args   参数
     * @param lang   语言管理器
     */
    private void handleList(Player player, String[] args, LanguageManager lang) {
        // 解析过滤参数
        String filter = "all";
        if (args.length >= 2) {
            filter = args[1].toLowerCase();
        }

        player.sendMessage(lang.getMessage("hologram.list-title"));

        Collection<IHologram> allHolograms = HologramManager.getInstance().getAllHolograms();

        if (allHolograms.isEmpty()) {
            player.sendMessage(lang.getMessage("hologram.list-empty"));
            return;
        }

        // 按来源分类
        List<IHologram> commandHolograms = new ArrayList<>();
        List<IHologram> pluginHolograms = new ArrayList<>();
        List<IHologram> unknownHolograms = new ArrayList<>();

        for (IHologram hologram : allHolograms) {
            String source = hologram.getSource();
            if ("command".equals(source)) {
                commandHolograms.add(hologram);
            } else if ("plugin".equals(source)) {
                pluginHolograms.add(hologram);
            } else {
                unknownHolograms.add(hologram);
            }
        }

        boolean showCommand = "all".equals(filter) || "command".equals(filter);
        boolean showPlugin = "all".equals(filter) || "plugin".equals(filter);
        boolean showUnknown = "all".equals(filter) || "unknown".equals(filter);

        // 显示命令创建的全息图
        if (showCommand && !commandHolograms.isEmpty()) {
            player.sendMessage(lang.getMessage("hologram.list-section-command",
                    String.valueOf(commandHolograms.size())));
            for (IHologram hologram : commandHolograms) {
                sendListEntry(player, hologram, lang);
            }
        }

        // 显示插件创建的全息图
        if (showPlugin && !pluginHolograms.isEmpty()) {
            player.sendMessage(lang.getMessage("hologram.list-section-plugin",
                    String.valueOf(pluginHolograms.size())));
            for (IHologram hologram : pluginHolograms) {
                sendListEntry(player, hologram, lang);
            }
        }

        // 显示未知来源的全息图（旧数据兼容）
        if (showUnknown && !unknownHolograms.isEmpty()) {
            player.sendMessage(lang.getMessage("hologram.list-section-unknown",
                    String.valueOf(unknownHolograms.size())));
            for (IHologram hologram : unknownHolograms) {
                sendListEntry(player, hologram, lang);
            }
        }

        // 如果过滤后没有结果
        boolean hasResults = (showCommand && !commandHolograms.isEmpty())
                || (showPlugin && !pluginHolograms.isEmpty())
                || (showUnknown && !unknownHolograms.isEmpty());

        if (!hasResults) {
            player.sendMessage(lang.getMessage("hologram.list-filter-empty", filter));
        }

        // 显示统计信息
        player.sendMessage(lang.getMessage("hologram.list-total",
                String.valueOf(allHolograms.size()),
                String.valueOf(commandHolograms.size()),
                String.valueOf(pluginHolograms.size()),
                String.valueOf(unknownHolograms.size())
        ));
    }

    /**
     * 发送单条全息图列表条目
     *
     * @param player   玩家
     * @param hologram 全息图
     * @param lang     语言管理器
     */
    private void sendListEntry(Player player, IHologram hologram, LanguageManager lang) {
        String status = hologram.isShown() ? "\u00a7a\u2714" : "\u00a7c\u2718";
        String valid = hologram.isValid() ? "" : " \u00a7c[\u5b9e\u4f53\u4e22\u5931]";
        player.sendMessage(lang.getMessage("hologram.list-entry",
                hologram.getId(),
                String.valueOf(hologram.size()),
                status
        ) + valid);
    }

    /**
     * 处理信息命令
     *
     * @param player 玩家
     * @param args   参数
     * @param lang   语言管理器
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

        // 显示来源信息
        player.sendMessage(lang.getMessage("hologram.info-source", hologram.getSource()));

        // 显示实体有效性
        player.sendMessage(lang.getMessage("hologram.info-valid",
                hologram.isValid() ? "\u00a7a\u6709\u6548" : "\u00a7c\u5b9e\u4f53\u4e22\u5931"));

        HologramStyle style = hologram.getStyle();
        String billboard = HologramStyle.BILLBOARD_FIXED.equals(style.getBillboard())
                ? style.getBillboard() + " " + formatNumber(style.getYaw()) + "°"
                : style.getBillboard();
        player.sendMessage(lang.getMessage("hologram.info-style",
                formatNumber(style.getScale()),
                billboard,
                style.getBackground(),
                lang.getMessage(style.isShadow() ? "hologram.info-yes" : "hologram.info-no")));

        if (hologram.size() > 0) {
            player.sendMessage(lang.getMessage("hologram.info-content"));
            List<String> lines = hologram.getLines();
            for (int i = 0; i < lines.size(); i++) {
                player.sendMessage(lang.getMessage("hologram.info-line", new Object[]{i, lines.get(i)}));
            }
        }
    }

    /**
     * 处理传送命令
     *
     * @param player 玩家
     * @param args   参数
     * @param lang   语言管理器
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
     *
     * @param player 玩家
     * @param lang   语言管理器
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
     *
     * @param player 玩家
     * @param lang   语言管理器
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

        // 非OP不提供任何补全，避免暴露子命令与全息图ID
        if (!sender.isOp()) {
            return completions;
        }

        // 系统不可用时不提供补全
        if (!HologramManager.getInstance().isEnabled()) {
            return completions;
        }

        if (args.length == 1) {
            List<String> subs = Arrays.asList(
                    "create", "delete", "addline", "setline", "removeline",
                    "insertline", "clear", "list", "info", "teleport", "reload"
            );
            return subs.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase();

            // list 命令的第二个参数：过滤类型
            if ("list".equals(sub)) {
                List<String> filters = Arrays.asList("all", "command", "plugin", "unknown");
                return filters.stream()
                        .filter(f -> f.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }

            // 其他需要全息图ID的子命令
            if (!"create".equals(sub)) {
                return HologramManager.getInstance().getAllHolograms().stream()
                        .map(IHologram::getId)
                        .filter(id -> id.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        return completions;
    }

    private static String formatNumber(double v) {
        if (v == Math.rint(v)) return String.valueOf((long) v);
        return String.valueOf(Math.round(v * 100) / 100.0);
    }
}