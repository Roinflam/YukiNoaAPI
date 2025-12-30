package pers.tany.yukinoaapi.interfacepart.player;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import pers.tany.yukinoaapi.Main;
import pers.tany.yukinoaapi.util.NMSUtil;
import pers.tany.yukinoaapi.interfacepart.other.IString;
import pers.tany.yukinoaapi.util.LangUtil;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * 玩家信息相关接口
 *
 * @author tany
 * @since 1.0.0
 */
public interface IMessage {

    /**
     * 向玩家发送一条ActionBar
     *
     * @param player 被发送的玩家
     * @param m      信息
     * @param time   时间/秒
     */
    static void sendActionBar(Player player, String m, int time) {
        new BukkitRunnable() {
            int i = 0;

            @Override
            public void run() {
                try {
                    Field playerConnection = NMSUtil.playerConnection;
                    Class<?> packetPlayOutChat = NMSUtil.packetPlayOutChat;
                    Method send = NMSUtil.send;
                    Method getHandle = NMSUtil.getHandle_Player;
                    Class<?> iChatBaseComponent = NMSUtil.iChatBaseComponent;
                    Class<?> chatComponentText = NMSUtil.chatComponentText;
                    Class<?> chatMessageType = NMSUtil.chatMessageType;

                    @Nullable Enum e = null;
                    for (Enum inside_enum : (Enum[]) chatMessageType.getEnumConstants()) {
                        if (inside_enum.name().equalsIgnoreCase("GAME_INFO")) {
                            e = inside_enum;
                            break;
                        }
                    }
                    Object message = chatComponentText.getConstructor(String.class).newInstance(IString.color(m));
                    Object packet;
                    try {
                        packet = packetPlayOutChat.getConstructor(iChatBaseComponent, chatMessageType).newInstance(message, e);
                    } catch (Exception a) {
                        packet = packetPlayOutChat.getConstructor(iChatBaseComponent, chatMessageType, player.getUniqueId().getClass()).newInstance(message, e, player.getUniqueId());
                    }
                    send.invoke(playerConnection.get(getHandle.invoke(player)), packet);

                } catch (Exception e) {
                    e.printStackTrace();
                    player.sendMessage(LangUtil.get("message.version-incompatible"));
                }
                i++;
                if (i >= time - 2) {
                    cancel();
                }
            }

        }.runTaskTimerAsynchronously(Main.plugin, 0, 20);
    }
}