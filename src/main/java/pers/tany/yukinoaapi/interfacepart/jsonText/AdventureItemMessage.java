package pers.tany.yukinoaapi.interfacepart.jsonText;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEventSource;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nullable;

/**
 * 1.20.5+ 的真实物品悬停（Paper 系服务端）
 * <p>
 * Paper 的 Bukkit 物品实现了 Adventure 的 {@link HoverEventSource}，{@code asHoverEvent()} 由服务端自己按当前版本编码
 * DataComponents，客户端显示真实的物品提示框（名字、附魔、属性、lore），不需要手动拼 show_item。
 * </p>
 * <p>
 * 单独成类：只在 {@code IBungee.AdventureItemHover} 确认服务端带 Adventure 之后才会被加载，没有 Adventure 的服务端不受影响。
 * </p>
 */
final class AdventureItemMessage {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private AdventureItemMessage() {
    }

    /**
     * @return 这件物品能否用 Adventure 发送（Bukkit 物品实现了 HoverEventSource、玩家实现了 Audience）
     */
    static boolean supports(ItemStack item) {
        return Audience.class.isAssignableFrom(Player.class) && ((Object) item) instanceof HoverEventSource;
    }

    /**
     * @param player     接收玩家，null 表示全服
     * @param message    已上色的消息
     * @param item       悬停展示的物品
     * @param command    点击命令，null 表示不可点击
     * @param useCommand true=直接执行命令，false=填充到聊天框
     */
    static void send(@Nullable Player player, String message, ItemStack item, @Nullable String command, boolean useCommand) {
        Component component = LEGACY.deserialize(message)
                .hoverEvent(((HoverEventSource<?>) (Object) item).asHoverEvent());
        if (command != null && !command.isEmpty()) {
            component = component.clickEvent(useCommand ? ClickEvent.runCommand(command) : ClickEvent.suggestCommand(command));
        }
        if (player != null) {
            ((Audience) (Object) player).sendMessage(component);
        } else {
            for (Player online : Bukkit.getOnlinePlayers()) {
                ((Audience) (Object) online).sendMessage(component);
            }
        }
    }
}
