package pers.tany.yukinoaapi.interfacepart.jsonText;

import net.md_5.bungee.api.chat.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import pers.tany.yukinoaapi.interfacepart.other.IString;

/**
 * 发送json文本相关
 */
public interface IBungee {
    /**
     * 向玩家发送一条json文本，鼠标放上去后会显示字符串
     *
     * @param player      被发送的玩家，如果为null则为公告
     * @param message     发送的文本
     * @param showMessage 鼠标放上去后显示的文本
     */
    static void sendShowMessage(Player player, String message, String showMessage) {
        TextComponent send = new TextComponent(IString.color(message));
        send.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(IString.color(showMessage)).create()));
        if (player == null) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.spigot().sendMessage(send);
            }
        } else {
            player.spigot().sendMessage(send);
        }
    }

    /**
     * 向玩家发送一条json文本，鼠标放上去后会显示物品的信息
     *
     * @param player  被发送的玩家，如果为null则为公告
     * @param message 发送的文本
     * @param itemNBT 鼠标放上去后被显示物品的NBT
     */
    static void sendItemNBTMessage(Player player, String message, String itemNBT) {
        TextComponent send = new TextComponent(IString.color(message));
        Bukkit.getConsoleSender().sendMessage(itemNBT);
        send.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM, new ComponentBuilder(IString.color(itemNBT)).create()));
        if (player == null) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.spigot().sendMessage(send);
            }
        } else {
            player.spigot().sendMessage(send);
        }
    }

    /**
     * 向玩家发送一条json文本，其中一段文字鼠标放上去后会显示物品的信息
     *
     * @param player        被发送的玩家，如果为null则为公告
     * @param message       发送的文本
     * @param part          被替换的文本
     * @param changeMessage 替换后的文本
     * @param showMessage   鼠标放上去后显示的文本
     */
    static void sendPartShowMessage(Player player, String message, String part, String changeMessage, String showMessage) {
        part = part.replace("[", "%前括号%").replace("]", "%后括号%").replace("+", "%加号%").replace("-", "%减号%").replace("$", "%钱号%");
        message = message.replace("[", "%前括号%").replace("]", "%后括号%").replace("+", "%加号%").replace("-", "%减号%").replace("$", "%钱号%");
        if (message.contains(part)) {
            TextComponent all = new TextComponent("");
            boolean first = true;
            while (message.contains(part)) {
                if (first) {
                    try {
                        all.addExtra(new TextComponent(IString.color(message.split(part)[0].replace("%前括号%", "[").replace("%后括号%", "]").replace("%加号%", "+").replace("%减号%", "-").replace("%钱号%", "$"))));
                    } catch (ArrayIndexOutOfBoundsException ignored) {

                    }
                }
                TextComponent parts = new TextComponent(new TextComponent(IString.color(part.replace(part, changeMessage))));
                parts.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(IString.color(showMessage)).create()));
                all.addExtra(parts);
                try {
                    all.addExtra(new TextComponent(IString.color(message.split(part)[1].replace("%前括号%", "[").replace("%后括号%", "]").replace("%加号%", "+").replace("%减号%", "-").replace("%钱号%", "$"))));
                } catch (ArrayIndexOutOfBoundsException ignored) {

                }
                first = false;
                message = message.replaceFirst(part, "");
            }
            if (player == null) {
                for (Player p:Bukkit.getOnlinePlayers()){
                    p.spigot().sendMessage(all);
                }
                return;
            }
            player.spigot().sendMessage(all);
        } else {
            player.sendMessage(IString.color(message));
        }
    }

    /**
     * 向玩家发送一条json文本，其中一段文字鼠标放上去后会显示物品的信息
     *
     * @param player        被发送的玩家，如果为null则为公告
     * @param message       发送的文本
     * @param part          被替换的文本
     * @param changeMessage 替换后的文本
     * @param itemNBT       鼠标放上去后被显示物品的NBT
     */
    static void sendPartItemNBTMessage(Player player, String message, String part, String changeMessage, String itemNBT) {
        part = part.replace("[", "%前括号%").replace("]", "%后括号%").replace("+", "%加号%").replace("-", "%减号%").replace("$", "%钱号%");
        message = message.replace("[", "%前括号%").replace("]", "%后括号%").replace("+", "%加号%").replace("-", "%减号%").replace("$", "%钱号%");
        if (message.contains(part)) {
            TextComponent all = new TextComponent("");
            boolean first = true;
            while (message.contains(part)) {
                if (first) {
                    try {
                        all.addExtra(new TextComponent(IString.color(message.split(part)[0].replace("%前括号%", "[").replace("%后括号%", "]").replace("%加号%", "+").replace("%减号%", "-").replace("%钱号%", "$"))));
                    } catch (ArrayIndexOutOfBoundsException ignored) {

                    }
                }
                TextComponent parts = new TextComponent(new TextComponent(IString.color(part.replace(part, changeMessage))));
                parts.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM, new ComponentBuilder(IString.color(itemNBT)).create()));
                all.addExtra(parts);
                try {
                    all.addExtra(new TextComponent(IString.color(message.split(part)[1].replace("%前括号%", "[").replace("%后括号%", "]").replace("%加号%", "+").replace("%减号%", "-").replace("%钱号%", "$"))));
                } catch (ArrayIndexOutOfBoundsException ignored) {

                }
                first = false;
                message = message.replaceFirst(part, "");
            }
            if (player == null) {
                for (Player p:Bukkit.getOnlinePlayers()){
                    p.spigot().sendMessage(all);
                }
                return;
            }
            player.spigot().sendMessage(all);
        } else {
            player.sendMessage(IString.color(message));
        }
    }

    /**
     * 向玩家发送一条json文本，鼠标放上去后会显示字符串，点击后执行指令
     *
     * @param player      被发送的玩家，如果为null则为公告
     * @param message     发送的文本
     * @param showMessage 鼠标放上去后显示的文本
     * @param command     执行的指令
     * @param useCommand  是否直接执行，如果为false则只会输入到聊天框
     */
    static void sendCommandMessage(Player player, String message, String showMessage, String command, Boolean useCommand) {
        TextComponent send = new TextComponent(IString.color(message));
        String value = null;
        if (useCommand) {
            value = "RUN_COMMAND";
        } else {
            value = "SUGGEST_COMMAND";
        }
        send.setClickEvent(new ClickEvent(ClickEvent.Action.valueOf(value), command));
        send.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(IString.color(showMessage)).create()));
        if (player == null) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.spigot().sendMessage(send);
            }
        } else {
            player.spigot().sendMessage(send);
        }
    }

    /**
     * 向玩家发送一条json文本，鼠标放上去后会显示物品的信息，点击后执行指令
     *
     * @param player     被发送的玩家，如果为null则为公告
     * @param message    发送的文本
     * @param itemNBT    鼠标放上去后被显示物品的NBT
     * @param command    执行的指令
     * @param useCommand 是否直接执行，如果为false则只会输入到聊天框
     */
    static void sendCommandItemMessage(Player player, String message, String itemNBT, String command, Boolean useCommand) {
        TextComponent send = new TextComponent(IString.color(message));
        String value = null;
        if (useCommand) {
            value = "RUN_COMMAND";
        } else {
            value = "SUGGEST_COMMAND";
        }
        send.setClickEvent(new ClickEvent(ClickEvent.Action.valueOf(value), command));
        send.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM, new ComponentBuilder(IString.color(itemNBT)).create()));
        if (player == null) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.spigot().sendMessage(send);
            }
        } else {
            player.spigot().sendMessage(send);
        }
    }

    /**
     * 向玩家发送一条json文本，鼠标放上去后会显示字符串
     *
     * @param player        被发送的玩家，如果为null则为公告
     * @param message       发送的文本
     * @param part          被替换的文本
     * @param changeMessage 替换后的文本
     * @param showMessage   鼠标放上去后显示的文本
     * @param command       执行的指令
     * @param useCommand    是否直接执行，如果为false则只会输入到聊天框
     */
    static void sendPartShowCommandMessage(Player player, String message, String part, String changeMessage, String showMessage, String command, Boolean useCommand) {
        part = part.replace("[", "%前括号%").replace("]", "%后括号%").replace("+", "%加号%").replace("-", "%减号%").replace("$", "%钱号%");
        message = message.replace("[", "%前括号%").replace("]", "%后括号%").replace("+", "%加号%").replace("-", "%减号%").replace("$", "%钱号%");
        if (message.contains(part)) {
            TextComponent all = new TextComponent("");
            boolean first = true;
            while (message.contains(part)) {
                if (first) {
                    try {
                        all.addExtra(new TextComponent(IString.color(message.split(part)[0].replace("%前括号%", "[").replace("%后括号%", "]").replace("%加号%", "+").replace("%减号%", "-").replace("%钱号%", "$"))));
                    } catch (ArrayIndexOutOfBoundsException ignored) {

                    }
                }
                TextComponent parts = new TextComponent(new TextComponent(IString.color(part.replace(part, changeMessage))));
                parts.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(IString.color(showMessage)).create()));
                String value = null;
                if (useCommand) {
                    value = "RUN_COMMAND";
                } else {
                    value = "SUGGEST_COMMAND";
                }
                parts.setClickEvent(new ClickEvent(ClickEvent.Action.valueOf(value), command));
                all.addExtra(parts);
                try {
                    all.addExtra(new TextComponent(IString.color(message.split(part)[1].replace("%前括号%", "[").replace("%后括号%", "]").replace("%加号%", "+").replace("%减号%", "-").replace("%钱号%", "$"))));
                } catch (ArrayIndexOutOfBoundsException ignored) {

                }
                first = false;
                message = message.replaceFirst(part, "");
            }
            if (player == null) {
                for (Player p:Bukkit.getOnlinePlayers()){
                    p.spigot().sendMessage(all);
                }
                return;
            }
            player.spigot().sendMessage(all);
        } else {
            player.sendMessage(IString.color(message));
        }
    }

    /**
     * 向玩家发送一条json文本，鼠标放上去后会显示物品的信息，点击后执行指令
     *
     * @param player        被发送的玩家，如果为null则为公告
     * @param message       发送的文本
     * @param part          被替换的文本
     * @param changeMessage 替换后的文本
     * @param itemNBT       鼠标放上去后被显示物品的NBT
     * @param command       执行的指令
     * @param useCommand    是否直接执行，如果为false则只会输入到聊天框
     */
    static void sendPartShowCommandItemMessage(Player player, String message, String part, String changeMessage, String itemNBT, String command, Boolean useCommand) {
        part = part.replace("[", "%前括号%").replace("]", "%后括号%").replace("+", "%加号%").replace("-", "%减号%").replace("$", "%钱号%");
        message = message.replace("[", "%前括号%").replace("]", "%后括号%").replace("+", "%加号%").replace("-", "%减号%").replace("$", "%钱号%");
        if (message.contains(part)) {
            TextComponent all = new TextComponent("");
            boolean first = true;
            while (message.contains(part)) {
                if (first) {
                    try {
                        all.addExtra(new TextComponent(IString.color(message.split(part)[0].replace("%前括号%", "[").replace("%后括号%", "]").replace("%加号%", "+").replace("%减号%", "-").replace("%钱号%", "$"))));
                    } catch (ArrayIndexOutOfBoundsException ignored) {

                    }
                }
                TextComponent parts = new TextComponent(new TextComponent(IString.color(part.replace(part, changeMessage))));
                parts.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM, new ComponentBuilder(IString.color(itemNBT)).create()));
                String value = null;
                if (useCommand) {
                    value = "RUN_COMMAND";
                } else {
                    value = "SUGGEST_COMMAND";
                }
                parts.setClickEvent(new ClickEvent(ClickEvent.Action.valueOf(value), command));
                all.addExtra(parts);
                try {
                    all.addExtra(new TextComponent(IString.color(message.split(part)[1].replace("%前括号%", "[").replace("%后括号%", "]").replace("%加号%", "+").replace("%减号%", "-").replace("%钱号%", "$"))));
                } catch (ArrayIndexOutOfBoundsException ignored) {

                }
                first = false;
                message = message.replaceFirst(part, "");
            }
            if (player == null) {
                for (Player p:Bukkit.getOnlinePlayers()){
                    p.spigot().sendMessage(all);
                }
                return;
            }
            player.spigot().sendMessage(all);
        } else {
            player.sendMessage(IString.color(message));
        }
    }

    /**
     * 向玩家发送一条json文本，鼠标放上去后会显示字符串
     *
     * @param message       发送的文本
     * @param part          被替换的文本
     * @param changeMessage 替换后的文本
     * @param showMessage   鼠标放上去后显示的文本
     * @param command       执行的指令
     * @param useCommand    是否直接执行，如果为false则只会输入到聊天框
     * @return 转化后的json文本
     */
    static BaseComponent transformPartShowCommandMessage(String message, String part, String changeMessage, String showMessage, String command, Boolean useCommand) {
        part = part.replace("[", "%前括号%").replace("]", "%后括号%").replace("+", "%加号%").replace("-", "%减号%").replace("$", "%钱号%");
        message = message.replace("[", "%前括号%").replace("]", "%后括号%").replace("+", "%加号%").replace("-", "%减号%").replace("$", "%钱号%");
        if (message.contains(part)) {
            TextComponent all = new TextComponent("");
            boolean first = true;
            while (message.contains(part)) {
                if (first) {
                    try {
                        all.addExtra(new TextComponent(IString.color(message.split(part)[0].replace("%前括号%", "[").replace("%后括号%", "]").replace("%加号%", "+").replace("%减号%", "-").replace("%钱号%", "$"))));
                    } catch (ArrayIndexOutOfBoundsException ignored) {

                    }
                }
                TextComponent parts = new TextComponent(new TextComponent(IString.color(part.replace(part, changeMessage))));
                parts.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(IString.color(showMessage)).create()));
                String value = null;
                if (useCommand) {
                    value = "RUN_COMMAND";
                } else {
                    value = "SUGGEST_COMMAND";
                }
                parts.setClickEvent(new ClickEvent(ClickEvent.Action.valueOf(value), command));
                all.addExtra(parts);
                try {
                    all.addExtra(new TextComponent(IString.color(message.split(part)[1].replace("%前括号%", "[").replace("%后括号%", "]").replace("%加号%", "+").replace("%减号%", "-").replace("%钱号%", "$"))));
                } catch (ArrayIndexOutOfBoundsException ignored) {

                }
                first = false;
                message = message.replaceFirst(part, "");
            }
            return all;
        } else {
            return new TextComponent(message);
        }
    }
}

