package pers.tany.yukinoaapi.interfacepart.player;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import pers.tany.yukinoaapi.Main;
import pers.tany.yukinoaapi.util.NMSUtil;
import pers.tany.yukinoaapi.interfacepart.other.IString;
import pers.tany.yukinoaapi.util.LangUtil;
import pers.tany.yukinoaapi.util.LogUtil;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * 玩家消息相关接口
 * 提供 ActionBar、Title 等消息发送功能
 *
 * <p>版本兼容性：</p>
 * <ul>
 *     <li>1.7.x - 不支持 ActionBar（回退到普通消息）</li>
 *     <li>1.8.x - 使用 NMS 反射发送</li>
 *     <li>1.9+  - 使用 Spigot API 发送</li>
 * </ul>
 *
 * @author tany
 * @since 1.0.0
 */
public interface IMessage {

    /**
     * 向玩家发送一条 ActionBar 消息
     * 消息会在屏幕下方显示，持续指定秒数
     *
     * @param player  目标玩家，不能为 null
     * @param message 要发送的消息，支持 & 颜色代码
     * @param time    持续时间（秒），最小为 1
     *
     * @example
     * <pre>{@code
     * // 发送持续 5 秒的 ActionBar
     * IMessage.sendActionBar(player, "&a欢迎回来！", 5);
     *
     * // 发送持续 10 秒的警告信息
     * IMessage.sendActionBar(player, "&c&l警告：区域即将关闭！", 10);
     * }</pre>
     */
    static void sendActionBar(Player player, String message, int time) {
        // 参数校验
        if (player == null || !player.isOnline()) {
            return;
        }
        if (message == null || message.isEmpty()) {
            return;
        }
        if (time <= 0) {
            time = 1;
        }

        // 处理颜色代码
        String coloredMessage = IString.color(message);
        int finalTime = time;

        // 1.7 不支持 ActionBar，直接发送普通消息
        if (!NMSUtil.isVersionAtLeast(8)) {
            player.sendMessage(coloredMessage);
            LogUtil.debug(LangUtil.get("message.actionbar-unsupported"));
            return;
        }

        new BukkitRunnable() {
            /**
             * 已执行次数
             */
            int i = 0;

            @Override
            public void run() {
                // 玩家离线则取消
                if (!player.isOnline()) {
                    cancel();
                    return;
                }

                try {
                    // 1.9+ 使用 Spigot API
                    if (NMSUtil.isVersionAtLeast(9)) {
                        sendActionBarSpigot(player, coloredMessage);
                    }
                    // 1.8 使用 NMS 反射
                    else {
                        sendActionBarNMS(player, coloredMessage);
                    }
                } catch (Exception e) {
                    LogUtil.debug(LangUtil.get("message.actionbar-fail", e.getMessage()));
                    player.sendMessage(coloredMessage);
                    cancel();
                    return;
                }

                i++;
                if (i >= finalTime) {
                    cancel();
                }
            }
        }.runTaskTimerAsynchronously(Main.plugin, 0, 20);
    }

    /**
     * 向玩家发送单次 ActionBar 消息
     * 消息显示约 2 秒后自动消失
     *
     * @param player  目标玩家
     * @param message 要发送的消息，支持 & 颜色代码
     */
    static void sendActionBarOnce(Player player, String message) {
        if (player == null || !player.isOnline()) {
            return;
        }
        if (message == null || message.isEmpty()) {
            return;
        }

        String coloredMessage = IString.color(message);

        // 1.7 不支持
        if (!NMSUtil.isVersionAtLeast(8)) {
            player.sendMessage(coloredMessage);
            return;
        }

        try {
            if (NMSUtil.isVersionAtLeast(9)) {
                sendActionBarSpigot(player, coloredMessage);
            } else {
                sendActionBarNMS(player, coloredMessage);
            }
        } catch (Exception e) {
            player.sendMessage(coloredMessage);
        }
    }

    /**
     * 使用 Spigot API 发送 ActionBar
     * 适用于 1.9+ 版本
     *
     * @param player  目标玩家
     * @param message 已处理颜色的消息
     */
    static void sendActionBarSpigot(Player player, String message) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
    }

    /**
     * 使用 NMS 反射发送 ActionBar
     * 适用于 1.8.x 版本
     *
     * @param player  目标玩家
     * @param message 已处理颜色的消息
     * @throws Exception 反射调用失败时抛出
     */
    static void sendActionBarNMS(Player player, String message) throws Exception {
        Field playerConnection = NMSUtil.playerConnection;
        Class<?> packetPlayOutChat = NMSUtil.packetPlayOutChat;
        Method send = NMSUtil.send;
        Method getHandle = NMSUtil.getHandle_Player;
        Class<?> iChatBaseComponent = NMSUtil.iChatBaseComponent;
        Class<?> chatComponentText = NMSUtil.chatComponentText;

        // 创建聊天组件
        Object chatComponent = chatComponentText
                .getConstructor(String.class)
                .newInstance(message);

        // 创建数据包，1.8 使用 byte 类型，2 表示 ACTION_BAR
        Object packet = packetPlayOutChat
                .getConstructor(iChatBaseComponent, byte.class)
                .newInstance(chatComponent, (byte) 2);

        // 发送数据包
        Object handle = getHandle.invoke(player);
        Object connection = playerConnection.get(handle);
        send.invoke(connection, packet);
    }
}