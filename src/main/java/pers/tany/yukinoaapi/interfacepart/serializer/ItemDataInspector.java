package pers.tany.yukinoaapi.interfacepart.serializer;

import com.comphenix.protocol.utility.StreamSerializer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import pers.tany.yukinoaapi.interfacepart.configuration.IConfig;
import pers.tany.yukinoaapi.interfacepart.encrypt.IEncrypt;
import pers.tany.yukinoaapi.util.LogUtil;
import pers.tany.yukinoaapi.util.NMSUtil;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 物品序列化数据的检查：判断数据里的物品在当前服务端是否还存在，以及记录被清理掉的数据。
 * <p>ISerializer 是接口，Java 8 下放不了私有静态方法，实现集中在这里。</p>
 *
 * @since 3.5.0
 */
final class ItemDataInspector {

    private static final String ITEM_SEPARATOR = "%SerializerItem%";
    private static final String LEGACY_PREFIX = "LEGACY_";
    private static final String REMOVED_LOG = "removed-items.log";

    /** 已经提示过的缺失材质名，同一种只提示一次，免得每次打开界面都刷屏 */
    private static final Set<String> WARNED_TYPES = ConcurrentHashMap.newKeySet();

    private ItemDataInspector() {
    }

    /**
     * 解开旧格式（加密的「材质名 / 数量 / 耐久 / NBT」）
     *
     * @return 至少 3 段的字段数组；不是这种格式返回 null
     */
    @Nullable
    static String[] decodeEncrypted(@Nullable String data) {
        if (data == null || data.isEmpty()) {
            return null;
        }
        try {
            String plain = IEncrypt.decrypt(data, "serializer");
            if (plain == null || !plain.contains(ITEM_SEPARATOR)) {
                return null;
            }
            String[] parts = plain.split(ITEM_SEPARATOR, -1);
            return parts.length >= 3 ? parts : null;
        } catch (Throwable e) {
            // ProtocolLib 格式是 Base64，不是十六进制，解密时会在这里失败
            return null;
        }
    }

    /**
     * 按材质名找材质。1.13+ 上 1.12 及更早存下的旧名（WOOL 之类）改找 LEGACY_ 前缀的材质，
     * 交给 Bukkit 连同耐久值一起转换，跨版本迁移的数据不会被当成不存在
     */
    @Nullable
    static Material resolveMaterial(@Nullable String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        Material material = Material.getMaterial(name);
        if (material == null && !name.startsWith(LEGACY_PREFIX)) {
            material = Material.getMaterial(LEGACY_PREFIX + name);
        }
        return material;
    }

    /**
     * 见 {@link ISerializer#isMissingItem(String)}
     */
    static boolean isMissingItem(@Nullable String data) {
        if (data == null || data.isEmpty()) {
            return false;
        }
        String[] parts = decodeEncrypted(data);
        if (parts != null) {
            return resolveMaterial(parts[0]) == null;
        }

        // ProtocolLib 格式按网络协议写的是数字 ID。模组被移除后这个 ID 在注册表里没有了，读出来只剩空气。
        // 本来就是空物品的数据开头是空标记：1.13+ 是 0（1.13~1.20.4 的「有物品」布尔值、1.20.5+ 的数量），
        // 1.12 及更早是物品 ID -1（0xFFFF）。数据后面还会补一串 0，不能按长度判断
        byte[] raw;
        try {
            raw = Base64.getMimeDecoder().decode(data);
        } catch (IllegalArgumentException e) {
            return false;
        }
        if (NMSUtil.isVersionAtLeast(13)) {
            if (raw.length == 0 || raw[0] == 0) {
                return false;
            }
        } else if (raw.length < 2 || (raw[0] == (byte) 0xFF && raw[1] == (byte) 0xFF)) {
            return false;
        }
        try {
            ItemStack itemStack = new StreamSerializer().deserializeItemStack(data);
            return itemStack != null && itemStack.getType() == Material.AIR;
        } catch (Throwable e) {
            // 读不出来不等于物品不存在（版本不兼容、数据损坏），不能据此删除
            return false;
        }
    }

    /**
     * 旧格式里材质不存在时提示一次
     */
    static void warnMissingType(String typeName) {
        if (WARNED_TYPES.add(typeName)) {
            LogUtil.warning("物品数据里的 " + typeName + " 在当前服务端不存在（所属模组可能已被移除），该物品读取为空");
        }
    }

    /**
     * 能取到时返回数据里的材质名，取不到返回 null
     */
    @Nullable
    static String getTypeName(@Nullable String data) {
        String[] parts = decodeEncrypted(data);
        return parts != null ? parts[0] : null;
    }

    /**
     * 见 {@link ISerializer#logRemovedItem(Plugin, String, String)}
     */
    static synchronized void logRemovedItem(Plugin plugin, String where, @Nullable String data) {
        String typeName = getTypeName(data);
        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        // ProtocolLib 格式是 MIME Base64，每 76 个字符带一个换行；解码时换行会被忽略，去掉后一条数据占一行
        String oneLine = data == null ? "null" : data.replace("\r", "").replace("\n", "");
        String line = time + " | " + where + " | " + (typeName != null ? typeName : "?") + " | " + oneLine + System.lineSeparator();
        try {
            File folder = IConfig.getValidPluginDataFolder(plugin);
            if (!folder.exists() && !folder.mkdirs()) {
                throw new java.io.IOException("无法创建目录 " + folder);
            }
            try (Writer writer = new OutputStreamWriter(new FileOutputStream(new File(folder, REMOVED_LOG), true), StandardCharsets.UTF_8)) {
                writer.write(line);
            }
        } catch (Exception e) {
            LogUtil.warning("写入 " + REMOVED_LOG + " 失败：" + e.getMessage());
        }
        LogUtil.warning("[" + plugin.getName() + "] 已移除不存在的物品（" + (typeName != null ? typeName : "所属模组已移除") + "）：" + where
                + "，原始数据已记入 " + REMOVED_LOG);
    }
}
