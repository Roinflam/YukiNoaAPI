package pers.tany.yukinoaapi.interfacepart.jsonText;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.HoverEvent.Action;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import pers.tany.yukinoaapi.interfacepart.other.IString;

/**
 * Bungee消息接口
 * 提供各种高级消息发送功能（悬停、点击、物品展示等）
 *
 * @author Tany天炎
 */
public interface IBungee {

    // ========================================
    // 基础消息发送方法
    // ========================================

    /**
     * 发送带悬停提示的消息
     *
     * @param player      接收玩家（null表示广播）
     * @param message     显示的消息
     * @param showMessage 悬停时显示的内容
     */
    static void sendShowMessage(@Nullable Player player, String message, String showMessage) {
        TextComponent send = new TextComponent(IString.color(message));
        send.setHoverEvent(new HoverEvent(Action.SHOW_TEXT,
                (new ComponentBuilder(IString.color(showMessage))).create()));

        sendComponent(player, send);
    }

    /**
     * 发送带物品NBT悬停的消息
     *
     * @param player  接收玩家（null表示广播）
     * @param message 显示的消息
     * @param itemNBT 物品NBT数据
     */
    static void sendItemNBTMessage(@Nullable Player player, String message, String itemNBT) {
        TextComponent send = new TextComponent(IString.color(message));
        send.setHoverEvent(new HoverEvent(Action.SHOW_ITEM,
                (new ComponentBuilder(IString.color(itemNBT))).create()));

        sendComponent(player, send);
    }

    // ========================================
    // 命令执行相关方法
    // ========================================

    /**
     * 发送带命令执行的消息
     *
     * @param player      接收玩家（null表示广播）
     * @param message     显示的消息
     * @param showMessage 悬停时显示的内容
     * @param command     要执行的命令
     * @param useCommand  true=直接执行，false=填充到聊天框
     */
    static void sendCommandMessage(@Nullable Player player, String message, String showMessage,
                                   String command, @Nonnull Boolean useCommand) {
        TextComponent send = new TextComponent(IString.color(message));

        ClickEvent.Action action = useCommand ?
                ClickEvent.Action.RUN_COMMAND : ClickEvent.Action.SUGGEST_COMMAND;
        send.setClickEvent(new ClickEvent(action, command));
        send.setHoverEvent(new HoverEvent(Action.SHOW_TEXT,
                (new ComponentBuilder(IString.color(showMessage))).create()));

        sendComponent(player, send);
    }

    /**
     * 发送带命令执行和物品NBT的消息
     *
     * @param player     接收玩家（null表示广播）
     * @param message    显示的消息
     * @param itemNBT    物品NBT数据
     * @param command    要执行的命令
     * @param useCommand true=直接执行，false=填充到聊天框
     */
    static void sendCommandItemMessage(@Nullable Player player, String message, String itemNBT,
                                       String command, @Nonnull Boolean useCommand) {
        TextComponent send = new TextComponent(IString.color(message));

        ClickEvent.Action action = useCommand ?
                ClickEvent.Action.RUN_COMMAND : ClickEvent.Action.SUGGEST_COMMAND;
        send.setClickEvent(new ClickEvent(action, command));
        send.setHoverEvent(new HoverEvent(Action.SHOW_ITEM,
                (new ComponentBuilder(IString.color(itemNBT))).create()));

        sendComponent(player, send);
    }

    // ========================================
    // 网址相关方法（新增）
    // ========================================

    /**
     * 发送点击打开网址的消息（带悬停提示）
     *
     * @param player      接收玩家（null表示广播）
     * @param message     显示的消息
     * @param showMessage 悬停时显示的内容
     * @param url         要打开的网址
     */
    static void sendUrlMessage(@Nullable Player player, String message, String showMessage, String url) {
        TextComponent send = new TextComponent(IString.color(message));
        send.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url));
        send.setHoverEvent(new HoverEvent(Action.SHOW_TEXT,
                (new ComponentBuilder(IString.color(showMessage))).create()));

        sendComponent(player, send);
    }

    /**
     * 发送部分文本点击打开网址的消息
     *
     * @param player        接收玩家（null表示广播）
     * @param message       完整消息
     * @param part          需要替换的部分（占位符）
     * @param changeMessage 替换后显示的文本
     * @param showMessage   悬停时显示的内容
     * @param url           要打开的网址
     */
    static void sendPartUrlMessage(@Nullable Player player, String message, String part,
                                   @Nonnull String changeMessage, String showMessage, String url) {
        if (!message.contains(part)) {
            sendSimpleMessage(player, message);
            return;
        }

        TextComponent all = buildPartMessage(message, part, changeMessage, (parts) -> {
            parts.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url));
            parts.setHoverEvent(new HoverEvent(Action.SHOW_TEXT,
                    (new ComponentBuilder(IString.color(showMessage))).create()));
        });

        sendComponent(player, all);
    }

    // ========================================
    // 复制文本相关方法（新增）
    // ========================================

    /**
     * 发送点击复制文本的消息（带悬停提示）
     *
     * @param player      接收玩家（null表示广播）
     * @param message     显示的消息
     * @param showMessage 悬停时显示的内容
     * @param copyText    要复制的文本
     */
    static void sendCopyMessage(@Nullable Player player, String message, String showMessage, String copyText) {
        TextComponent send = new TextComponent(IString.color(message));
        send.setClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, copyText));
        send.setHoverEvent(new HoverEvent(Action.SHOW_TEXT,
                (new ComponentBuilder(IString.color(showMessage))).create()));

        sendComponent(player, send);
    }

    /**
     * 发送部分文本点击复制的消息
     *
     * @param player        接收玩家（null表示广播）
     * @param message       完整消息
     * @param part          需要替换的部分（占位符）
     * @param changeMessage 替换后显示的文本
     * @param showMessage   悬停时显示的内容
     * @param copyText      要复制的文本
     */
    static void sendPartCopyMessage(@Nullable Player player, String message, String part,
                                    @Nonnull String changeMessage, String showMessage, String copyText) {
        if (!message.contains(part)) {
            sendSimpleMessage(player, message);
            return;
        }

        TextComponent all = buildPartMessage(message, part, changeMessage, (parts) -> {
            parts.setClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, copyText));
            parts.setHoverEvent(new HoverEvent(Action.SHOW_TEXT,
                    (new ComponentBuilder(IString.color(showMessage))).create()));
        });

        sendComponent(player, all);
    }

    // ========================================
    // 部分替换 - 悬停提示
    // ========================================

    /**
     * 发送部分文本带悬停提示的消息
     *
     * @param player        接收玩家（null表示广播）
     * @param message       完整消息
     * @param part          需要替换的部分（占位符）
     * @param changeMessage 替换后显示的文本
     * @param showMessage   悬停时显示的内容
     */
    static void sendPartShowMessage(@Nullable Player player, String message, String part,
                                    @Nonnull String changeMessage, String showMessage) {
        if (!message.contains(part)) {
            sendSimpleMessage(player, message);
            return;
        }

        TextComponent all = buildPartMessage(message, part, changeMessage,
                (parts) -> parts.setHoverEvent(new HoverEvent(Action.SHOW_TEXT,
                        (new ComponentBuilder(IString.color(showMessage))).create())));

        sendComponent(player, all);
    }

    /**
     * 发送部分文本带物品NBT悬停的消息
     *
     * @param player        接收玩家（null表示广播）
     * @param message       完整消息
     * @param part          需要替换的部分（占位符）
     * @param changeMessage 替换后显示的文本
     * @param itemNBT       物品NBT数据
     */
    static void sendPartItemNBTMessage(@Nullable Player player, String message, String part,
                                       @Nonnull String changeMessage, String itemNBT) {
        if (!message.contains(part)) {
            sendSimpleMessage(player, message);
            return;
        }

        TextComponent all = buildPartMessage(message, part, changeMessage,
                (parts) -> parts.setHoverEvent(new HoverEvent(Action.SHOW_ITEM,
                        (new ComponentBuilder(IString.color(itemNBT))).create())));

        sendComponent(player, all);
    }

    // ========================================
    // 部分替换 - 悬停 + 命令
    // ========================================

    /**
     * 发送部分文本带悬停和命令的消息
     *
     * @param player        接收玩家（null表示广播）
     * @param message       完整消息
     * @param part          需要替换的部分（占位符）
     * @param changeMessage 替换后显示的文本
     * @param showMessage   悬停时显示的内容
     * @param command       要执行的命令
     * @param useCommand    true=直接执行，false=填充到聊天框
     */
    static void sendPartShowCommandMessage(@Nullable Player player, String message, String part,
                                           @Nonnull String changeMessage, String showMessage,
                                           String command, Boolean useCommand) {
        if (!message.contains(part)) {
            sendSimpleMessage(player, message);
            return;
        }

        ClickEvent.Action action = useCommand ?
                ClickEvent.Action.RUN_COMMAND : ClickEvent.Action.SUGGEST_COMMAND;

        TextComponent all = buildPartMessage(message, part, changeMessage, (parts) -> {
            parts.setHoverEvent(new HoverEvent(Action.SHOW_TEXT,
                    (new ComponentBuilder(IString.color(showMessage))).create()));
            parts.setClickEvent(new ClickEvent(action, command));
        });

        sendComponent(player, all);
    }

    /**
     * 发送部分文本带物品NBT和命令的消息
     *
     * @param player        接收玩家（null表示广播）
     * @param message       完整消息
     * @param part          需要替换的部分（占位符）
     * @param changeMessage 替换后显示的文本
     * @param itemNBT       物品NBT数据
     * @param command       要执行的命令
     * @param useCommand    true=直接执行，false=填充到聊天框
     */
    static void sendPartShowCommandItemMessage(@Nullable Player player, String message, String part,
                                               @Nonnull String changeMessage, String itemNBT,
                                               String command, Boolean useCommand) {
        if (!message.contains(part)) {
            sendSimpleMessage(player, message);
            return;
        }

        ClickEvent.Action action = useCommand ?
                ClickEvent.Action.RUN_COMMAND : ClickEvent.Action.SUGGEST_COMMAND;

        TextComponent all = buildPartMessage(message, part, changeMessage, (parts) -> {
            parts.setHoverEvent(new HoverEvent(Action.SHOW_ITEM,
                    (new ComponentBuilder(IString.color(itemNBT))).create()));
            parts.setClickEvent(new ClickEvent(action, command));
        });

        sendComponent(player, all);
    }

    // ========================================
    // 组件转换方法（不发送）
    // ========================================

    /**
     * 转换为带悬停和命令的组件（不发送）
     *
     * @param message       完整消息
     * @param part          需要替换的部分（占位符）
     * @param changeMessage 替换后显示的文本
     * @param showMessage   悬停时显示的内容
     * @param command       要执行的命令
     * @param useCommand    true=直接执行，false=填充到聊天框
     * @return 处理后的组件
     */
    @Nonnull
    static BaseComponent transformPartShowCommandMessage(String message, String part,
                                                         @Nonnull String changeMessage,
                                                         String showMessage, String command,
                                                         Boolean useCommand) {
        if (!message.contains(part)) {
            return new TextComponent(IString.color(message));
        }

        ClickEvent.Action action = useCommand ?
                ClickEvent.Action.RUN_COMMAND : ClickEvent.Action.SUGGEST_COMMAND;

        return buildPartMessage(message, part, changeMessage, (parts) -> {
            parts.setHoverEvent(new HoverEvent(Action.SHOW_TEXT,
                    (new ComponentBuilder(IString.color(showMessage))).create()));
            parts.setClickEvent(new ClickEvent(action, command));
        });
    }

    // ========================================
    // 核心方法 - 消息构建和发送
    // ========================================

    /**
     * 构建部分替换的消息组件（核心逻辑）
     * 使用字面量匹配，无需转义特殊字符
     *
     * @param message       完整消息
     * @param part          需要替换的部分（占位符）
     * @param changeMessage 替换后显示的文本
     * @param decorator     装饰器（为替换部分添加悬停、点击等效果）
     * @return 构建好的文本组件
     */
    static TextComponent buildPartMessage(String message, String part,
                                          String changeMessage,
                                          ComponentDecorator decorator) {
        // 防御性检查：part 不能为空或 null
        if (part == null || part.isEmpty()) {
            return new TextComponent(IString.color(message));
        }

        TextComponent all = new TextComponent("");
        int lastIndex = 0;

        // 使用 indexOf 进行字面量匹配（无需转义）
        while (true) {
            int index = message.indexOf(part, lastIndex);

            // 没有找到更多占位符
            if (index == -1) {
                // 添加剩余文本
                if (lastIndex < message.length()) {
                    all.addExtra(new TextComponent(IString.color(message.substring(lastIndex))));
                }
                break;
            }

            // 添加占位符前的文本
            if (index > lastIndex) {
                all.addExtra(new TextComponent(IString.color(message.substring(lastIndex, index))));
            }

            // 添加替换部分（应用装饰器）
            TextComponent parts = new TextComponent(IString.color(changeMessage));
            decorator.decorate(parts);
            all.addExtra(parts);

            // 更新搜索位置
            lastIndex = index + part.length();
        }

        return all;
    }

    /**
     * 发送普通消息（无特效）
     *
     * @param player  接收玩家（null表示广播）
     * @param message 消息内容
     */
    static void sendSimpleMessage(@Nullable Player player, String message) {
        if (player == null) {
            Bukkit.broadcastMessage(IString.color(message));
        } else {
            player.sendMessage(IString.color(message));
        }
    }

    /**
     * 发送组件消息（统一发送接口）
     *
     * @param player    接收玩家（null表示广播）
     * @param component 要发送的组件
     */
    static void sendComponent(@Nullable Player player, BaseComponent component) {
        if (player == null) {
            Bukkit.getServer().spigot().broadcast(component);
        } else {
            player.spigot().sendMessage(component);
        }
    }

    // ========================================
    // 函数式接口 - 组件装饰器
    // ========================================

    /**
     * 组件装饰器函数式接口
     * 用于为文本组件添加悬停、点击等效果
     */
    @FunctionalInterface
    interface ComponentDecorator {
        /**
         * 装饰文本组件
         *
         * @param component 要装饰的组件
         */
        void decorate(TextComponent component);
    }
}