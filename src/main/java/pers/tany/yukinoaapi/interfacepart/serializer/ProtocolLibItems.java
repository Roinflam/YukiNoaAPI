package pers.tany.yukinoaapi.interfacepart.serializer;

import com.comphenix.protocol.utility.StreamSerializer;
import org.bukkit.inventory.ItemStack;
import pers.tany.yukinoaapi.util.LogUtil;

/**
 * ProtocolLib 格式的物品读写
 * <p>
 * 服务端没装 ProtocolLib 时 {@code new StreamSerializer()} 会抛 NoClassDefFoundError（不是 Exception，原来的 catch 接不住），
 * 所以先看 ProtocolLib 在不在，不在就不走这里。
 * </p>
 */
final class ProtocolLibItems {

    private static final boolean AVAILABLE = classExists("com.comphenix.protocol.utility.StreamSerializer");

    private static volatile boolean writeNoticed;
    private static volatile boolean readNoticed;

    private ProtocolLibItems() {
    }

    /**
     * 能否用 ProtocolLib 格式写入；不能时提示一次（物品改用旧格式保存，以后装上 ProtocolLib 也照样能读）
     */
    static boolean canWrite() {
        if (!AVAILABLE && !writeNoticed) {
            writeNoticed = true;
            LogUtil.warning("配置开启了 ProtocolLibItem 但服务端没有安装 ProtocolLib，物品改用内置格式保存");
        }
        return AVAILABLE;
    }

    /**
     * 能否读取 ProtocolLib 格式；不能时提示一次（这类数据读出来为 null，不会被删除）
     */
    static boolean canRead() {
        if (!AVAILABLE && !readNoticed) {
            readNoticed = true;
            LogUtil.warning("有物品数据是 ProtocolLib 格式，但服务端没有安装 ProtocolLib，这些物品暂时读取为空");
        }
        return AVAILABLE;
    }

    static String serialize(ItemStack itemStack) throws Exception {
        return new StreamSerializer().serializeItemStack(itemStack);
    }

    static ItemStack deserialize(String data) throws Exception {
        return new StreamSerializer().deserializeItemStack(data);
    }

    private static boolean classExists(String name) {
        try {
            Class.forName(name, false, ProtocolLibItems.class.getClassLoader());
            return true;
        } catch (Throwable t) {
            return false;
        }
    }
}
