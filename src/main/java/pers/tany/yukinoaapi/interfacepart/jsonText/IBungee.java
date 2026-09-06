package pers.tany.yukinoaapi.interfacepart.jsonText;

import net.md_5.bungee.api.chat.*;
import net.md_5.bungee.api.chat.HoverEvent.Action;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import pers.tany.yukinoaapi.interfacepart.item.IItem;
import pers.tany.yukinoaapi.interfacepart.other.IString;
import pers.tany.yukinoaapi.util.LogUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Bungee消息接口
 * 提供各种高级消息发送功能（悬停、点击、物品展示等）
 *
 * <p>
 * <b>SHOW_ITEM 跨版本说明（关键，本次修订分流判据）：</b><br>
 * 1.20.5+ 原版 {@code show_item} 悬停组件格式发生破坏性变更（DataComponents 体系），
 * 旧版 BungeeCord {@code new ComponentBuilder(itemNBT)} 拼出的 SNBT 组件在新版服务端
 * 会渲染丢失（光板物品）甚至抛出 {@code JsonParseException}。
 * </p>
 * <p>
 * 因此本接口对所有涉及物品悬停的方法做了双分支，<b>分流判据为是否处于 1.20.5+ 时代</b>
 * （复用 {@link IItem#useNbtApiForItem()}，即 {@code NMSUtil.save_ItemStack == null}）：
 * <ul>
 *   <li><b>旧版（1.20.4 及以下，{@link #ITEM_HOVER_TEXT_FALLBACK} = false）：</b>
 *       走原版 BungeeCord {@code SHOW_ITEM} 路径，由客户端原生渲染<b>真实物品图标 + 完整
 *       tooltip（名称、lore、附魔、属性）</b>。逻辑与历史版本一致。</li>
 *   <li><b>新版（1.20.5+，{@link #ITEM_HOVER_TEXT_FALLBACK} = true）：</b>
 *       降级为 {@code SHOW_TEXT} 文本悬停（见 {@link AdventureItemHover}），
 *       读取物品名 + lore 拼成多行文字，无图标但跨版本稳定不崩。</li>
 * </ul>
 * </p>
 * <p>
 * <b>修订背景：</b>旧实现用「运行时是否存在 Adventure API」作为分流判据，导致 Mohist / Paper 等
 * 自带 Adventure 的 <b>1.20.4 及以下</b>服务端（如 1.20.1）被误判为「新版」而强制走文本降级，
 * 悬停只剩物品名、丢失图标与 lore。改为按 1.20.5 这一真正的破坏性分界判定后，
 * 1.20.1 等旧版恢复 SHOW_ITEM 图标显示。注意文本降级路径底层用的是 BungeeCord 的
 * {@code SHOW_TEXT}，并不依赖 Adventure API，因此判据与「服务端是否带 Adventure」无关，
 * 只取决于物品序列化时代。
 * </p>
 *
 * @author Tany天炎
 */
public final class IBungee {

    /**
     * 工具类,禁止实例化
     *
     * @throws UnsupportedOperationException 总是抛出
     */
    private IBungee() {
        throw new UnsupportedOperationException("IBungee 是工具类,不允许实例化");
    }


    // ========================================
    // 物品悬停分流判据（1.20.5 破坏性分界）
    // ========================================

    /**
     * 是否处于 1.20.5+ DataComponents 时代，决定物品悬停走哪条路径。
     * <p>
     * 判据复用 {@link IItem#useNbtApiForItem()}（{@code NMSUtil.save_ItemStack == null}）：
     * <ul>
     *   <li>1.20.4 及以下该 NMS 方法存在 → {@code false} → 物品悬停走 {@code SHOW_ITEM}
     *       （客户端原生渲染真实图标 + 完整 tooltip）；</li>
     *   <li>1.20.5 及以上该方法被移除 → {@code true} → 物品悬停降级为 {@code SHOW_TEXT} 文本
     *       （规避新版 show_item 渲染丢失 / 崩溃）。</li>
     * </ul>
     * </p>
     * <p>
     * <b>注意：</b>文本降级路径（{@link AdventureItemHover}）底层使用 BungeeCord 的
     * {@code SHOW_TEXT}，并不依赖 Adventure API，故判据与「服务端是否带 Adventure」无关，
     * 只取决于物品序列化时代，避免自带 Adventure 的旧版服务端被误降级。
     * </p>
     */
    public static final boolean ITEM_HOVER_TEXT_FALLBACK = IItem.useNbtApiForItem();

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
    public static void sendShowMessage(@Nullable Player player, String message, String showMessage) {
        TextComponent send = new TextComponent(IString.color(message));
        send.setHoverEvent(new HoverEvent(Action.SHOW_TEXT,
                (new ComponentBuilder(IString.color(showMessage))).create()));

        sendComponent(player, send);
    }

    /**
     * 发送带物品NBT悬停的消息
     * <p>
     * 1.20.5+ 走 {@link AdventureItemHover#sendItemHover}（SHOW_TEXT 文本悬停），
     * 1.20.4 及以下走 BungeeCord SHOW_ITEM 路径（真实物品图标）。
     * </p>
     *
     * @param player  接收玩家（null表示广播）
     * @param message 显示的消息
     * @param itemNBT 物品NBT数据（SNBT 字符串）
     */
    public static void sendItemNBTMessage(@Nullable Player player, String message, String itemNBT) {
        // 新版（1.20.5+）：降级为文本悬停，规避 show_item 渲染丢失 / 崩溃
        if (ITEM_HOVER_TEXT_FALLBACK) {
            AdventureItemHover.sendItemHover(player, message, itemNBT, null, false);
            return;
        }

        // 旧版（1.20.4-）：SHOW_ITEM 显示真实物品图标 + 完整 tooltip
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
    public static void sendCommandMessage(@Nullable Player player, String message, String showMessage,
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
     * <p>
     * 1.20.5+ 走 {@link AdventureItemHover#sendItemHover}（SHOW_TEXT 文本悬停 + 点击命令），
     * 1.20.4 及以下走 BungeeCord SHOW_ITEM + ClickEvent 路径（真实物品图标）。
     * </p>
     *
     * @param player     接收玩家（null表示广播）
     * @param message    显示的消息
     * @param itemNBT    物品NBT数据（SNBT 字符串）
     * @param command    要执行的命令
     * @param useCommand true=直接执行，false=填充到聊天框
     */
    public static void sendCommandItemMessage(@Nullable Player player, String message, String itemNBT,
                                       String command, @Nonnull Boolean useCommand) {
        // 新版（1.20.5+）：降级为文本悬停 + 点击命令
        if (ITEM_HOVER_TEXT_FALLBACK) {
            AdventureItemHover.sendItemHover(player, message, itemNBT, command, useCommand);
            return;
        }

        // 旧版（1.20.4-）：SHOW_ITEM 显示真实物品图标 + 完整 tooltip
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
    public static void sendUrlMessage(@Nullable Player player, String message, String showMessage, String url) {
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
    public static void sendPartUrlMessage(@Nullable Player player, String message, String part,
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
    public static void sendPartShowMessage(@Nullable Player player, String message, String part,
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
     * <p>
     * <b>注意：</b>该方法在新版（1.20.5+）下，因 BungeeCord 的部分替换组件体系
     * 无法安全携带物品 id，会退化为「悬停展示 名字+lore 文本」而非物品图标，避免崩溃。
     * 如需新版完整物品悬停，请使用 {@link #sendItemNBTMessage} 或 {@link #sendCommandItemMessage}。
     * 1.20.4 及以下逻辑保持不变（SHOW_ITEM 真实图标）。
     * </p>
     *
     * @param player        接收玩家（null表示广播）
     * @param message       完整消息
     * @param part          需要替换的部分（占位符）
     * @param changeMessage 替换后显示的文本
     * @param itemNBT       物品NBT数据（SNBT 字符串）
     */
    public static void sendPartItemNBTMessage(@Nullable Player player, String message, String part,
                                       @Nonnull String changeMessage, String itemNBT) {
        if (!message.contains(part)) {
            sendSimpleMessage(player, message);
            return;
        }

        // 新版（1.20.5+）：部分替换无法安全携带物品 id，退化为悬停「名字+lore」文本，避免崩溃
        if (ITEM_HOVER_TEXT_FALLBACK) {
            String hoverText = AdventureItemHover.itemNbtToHoverText(itemNBT);
            TextComponent safe = buildPartMessage(message, part, changeMessage,
                    (parts) -> parts.setHoverEvent(new HoverEvent(Action.SHOW_TEXT,
                            (new ComponentBuilder(hoverText)).create())));
            sendComponent(player, safe);
            return;
        }

        // 旧版（1.20.4-）：保留 SHOW_ITEM 路径，显示真实物品图标
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
    public static void sendPartShowCommandMessage(@Nullable Player player, String message, String part,
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
     * <p>
     * <b>注意：</b>该方法在新版（1.20.5+）下，因 BungeeCord 的部分替换组件体系
     * 无法安全携带物品 id，会退化为「悬停展示 名字+lore 文本 + 点击命令」而非物品图标，避免崩溃。
     * 1.20.4 及以下逻辑保持不变（SHOW_ITEM 真实图标）。
     * </p>
     *
     * @param player        接收玩家（null表示广播）
     * @param message       完整消息
     * @param part          需要替换的部分（占位符）
     * @param changeMessage 替换后显示的文本
     * @param itemNBT       物品NBT数据（SNBT 字符串）
     * @param command       要执行的命令
     * @param useCommand    true=直接执行，false=填充到聊天框
     */
    public static void sendPartShowCommandItemMessage(@Nullable Player player, String message, String part,
                                               @Nonnull String changeMessage, String itemNBT,
                                               String command, Boolean useCommand) {
        if (!message.contains(part)) {
            sendSimpleMessage(player, message);
            return;
        }

        ClickEvent.Action action = useCommand ?
                ClickEvent.Action.RUN_COMMAND : ClickEvent.Action.SUGGEST_COMMAND;

        // 新版（1.20.5+）：退化为悬停「名字+lore」文本 + 点击命令，避免崩溃
        if (ITEM_HOVER_TEXT_FALLBACK) {
            String hoverText = AdventureItemHover.itemNbtToHoverText(itemNBT);
            TextComponent safe = buildPartMessage(message, part, changeMessage, (parts) -> {
                parts.setHoverEvent(new HoverEvent(Action.SHOW_TEXT,
                        (new ComponentBuilder(hoverText)).create()));
                parts.setClickEvent(new ClickEvent(action, command));
            });
            sendComponent(player, safe);
            return;
        }

        // 旧版（1.20.4-）：保留 SHOW_ITEM 路径，显示真实物品图标
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
    public static BaseComponent transformPartShowCommandMessage(String message, String part,
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
    public static TextComponent buildPartMessage(String message, String part,
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
    public static void sendSimpleMessage(@Nullable Player player, String message) {
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
    public static void sendComponent(@Nullable Player player, BaseComponent component) {
        if (player == null) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.spigot().sendMessage(component);
            }
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
    public interface ComponentDecorator {
        /**
         * 装饰文本组件
         *
         * @param component 要装饰的组件
         */
        void decorate(TextComponent component);
    }

    // ========================================
    // 物品悬停辅助类（文本悬停方案）
    // ========================================

    /**
     * 物品悬停构造与发送辅助类（SHOW_TEXT 文本悬停）
     * <p>
     * 由于 1.20.5+ 原版 {@code show_item} 悬停的 components 编码格式破坏性变更，
     * 反射 / Adventure 手动构造极易导致客户端渲染丢失（光板物品）或崩溃，
     * 故统一改用 {@code SHOW_TEXT} 文本悬停：读取物品的显示名与 lore，
     * 拼接为带颜色的多行文本作为悬停内容。效果为纯文字（无物品图标），
     * 但跨所有版本稳定、不崩溃，且完整展示名称与 lore（含价格等信息）。
     * </p>
     * <p>
     * <b>调用前提：</b>本辅助类仅在 {@link #ITEM_HOVER_TEXT_FALLBACK} 为 true（1.20.5+）时使用；
     * 1.20.4 及以下不会进入此类，物品悬停走 SHOW_ITEM 显示真实图标。
     * </p>
     */
    public static final class AdventureItemHover {

        /** 工具类，禁止实例化 */
        private AdventureItemHover() {
        }

        /**
         * 构造物品的文本悬停消息并发送
         * <p>
         * 流程：调用 {@link #itemNbtToHoverText} 将 SNBT 转为「名字 + lore」多行文本
         * → 通过 BungeeCord {@code SHOW_TEXT} 发送（带可选点击命令）。
         * 任意环节异常时降级为「悬停展示原始字符串」，保证不崩溃。
         * </p>
         *
         * @param player     接收玩家（null表示广播）
         * @param message    主显示文本
         * @param itemNBT    物品 SNBT 字符串
         * @param command    点击命令（可为 null，表示无点击事件）
         * @param useCommand true=直接执行命令，false=填充到聊天框
         */
        static void sendItemHover(@Nullable Player player, String message, String itemNBT,
                                  @Nullable String command, boolean useCommand) {
            try {
                // ---- 1. SNBT → 名字+lore 文本（统一走 itemNbtToHoverText，失败时内部自动回退原始字符串） ----
                String hoverText = itemNbtToHoverText(itemNBT);

                // ---- 2. 构造主组件并设置 SHOW_TEXT 悬停（1.20.5+ 下不会崩溃） ----
                TextComponent send = new TextComponent(IString.color(message));
                send.setHoverEvent(new HoverEvent(Action.SHOW_TEXT,
                        new ComponentBuilder(hoverText).create()));

                // ---- 3. 可选点击事件 ----
                if (command != null && !command.isEmpty()) {
                    ClickEvent.Action action = useCommand ?
                            ClickEvent.Action.RUN_COMMAND : ClickEvent.Action.SUGGEST_COMMAND;
                    send.setClickEvent(new ClickEvent(action, command));
                }

                // ---- 4. 发送 ----
                sendComponent(player, send);

            } catch (Throwable t) {
                LogUtil.error("[IBungee] 物品文本悬停构造失败，降级为原始字符串悬停", t instanceof Exception ? (Exception) t : new Exception(t));
                sendTextHoverFallback(player, message, itemNBT, command, useCommand);
            }
        }

        /**
         * 将物品 SNBT 字符串转换为「名字 + lore」多行悬停文本
         * <p>
         * 流程：SNBT → {@code IItem.getItemStack} 还原 ItemStack → 读取显示名与 lore 拼接。
         * 还原失败或无元数据时，回退为带颜色的原始 SNBT 字符串（保证调用方始终有可用文本）。
         * 供 {@link #sendItemHover} 及 {@code IBungee} 的 sendPart* 系列方法共用，
         * 使新版（1.20.5+）下的物品悬停统一显示为可读的名称与 lore，而非原始 NBT。
         * </p>
         *
         * @param itemNBT 物品 SNBT 字符串
         * @return 名字 + lore 多行文本；失败时回退为染色后的原始 SNBT
         */
        static String itemNbtToHoverText(String itemNBT) {
            try {
                ItemStack item = IItem.getItemStack(itemNBT);
                if (item == null) {
                    return IString.color(itemNBT);
                }
                return buildHoverText(item);
            } catch (Throwable t) {
                // 解析失败不致命，回退原始字符串
                return IString.color(itemNBT);
            }
        }

        /**
         * 由物品读取显示名与 lore，拼接为多行悬停文本
         * <p>
         * 第一行为显示名（无自定义名时使用物品类型名），其后逐行追加 lore。
         * Bukkit 的 {@code getDisplayName}/{@code getLore} 返回的字符串已带 {@code §} 颜色代码，
         * 可直接用于悬停文本，无需解析富文本 JSON。
         * </p>
         *
         * @param item 物品（非 null）
         * @return 多行悬停文本
         */
        private static String buildHoverText(@Nonnull ItemStack item) {
            StringBuilder sb = new StringBuilder();

            // 显示名：优先自定义名，否则用物品类型名
            String displayName = null;
            if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                displayName = item.getItemMeta().getDisplayName();
            }
            if (displayName == null || displayName.isEmpty()) {
                displayName = item.getType().name();
            }
            sb.append(displayName);

            // lore：逐行追加（每行前加换行符）
            if (item.hasItemMeta() && item.getItemMeta().hasLore()) {
                java.util.List<String> lore = item.getItemMeta().getLore();
                if (lore != null) {
                    for (String line : lore) {
                        sb.append('\n').append(line == null ? "" : line);
                    }
                }
            }

            return sb.toString();
        }

        /**
         * 降级方案：用 BungeeCord 发送「悬停展示原始字符串」的消息（保证不崩溃）
         *
         * @param player     接收玩家（null表示广播）
         * @param message    主显示文本
         * @param itemNBT    物品 SNBT 字符串（作为悬停文本兜底）
         * @param command    点击命令（可为 null）
         * @param useCommand true=直接执行，false=填充到聊天框
         */
        private static void sendTextHoverFallback(@Nullable Player player, String message, String itemNBT,
                                                  @Nullable String command, boolean useCommand) {
            TextComponent send = new TextComponent(IString.color(message));
            send.setHoverEvent(new HoverEvent(Action.SHOW_TEXT,
                    (new ComponentBuilder(IString.color(itemNBT))).create()));
            if (command != null && !command.isEmpty()) {
                ClickEvent.Action action = useCommand ?
                        ClickEvent.Action.RUN_COMMAND : ClickEvent.Action.SUGGEST_COMMAND;
                send.setClickEvent(new ClickEvent(action, command));
            }
            sendComponent(player, send);
        }
    }
}

