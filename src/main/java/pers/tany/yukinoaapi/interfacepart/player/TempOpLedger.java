package pers.tany.yukinoaapi.interfacepart.player;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

/**
 * useCommand 临时给的 OP 先落盘再给：服务端在命令执行中途崩掉时 finally 不会跑，
 * 下次启用时按这份名单收回。
 */
public final class TempOpLedger {

    private static File file;

    private TempOpLedger() {
    }

    public static synchronized void init(Plugin plugin) {
        file = new File(plugin.getDataFolder(), "temp-op.yml");
        List<String> left = read();
        if (left.isEmpty()) {
            return;
        }
        for (String id : left) {
            try {
                OfflinePlayer player = Bukkit.getOfflinePlayer(UUID.fromString(id));
                player.setOp(false);
                plugin.getLogger().warning("Revoked temporary OP left over from an interrupted command: " + id);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to revoke temporary OP: " + id, e);
            }
        }
        write(new ArrayList<>());
    }

    static synchronized void add(UUID uuid) {
        List<String> ids = read();
        if (!ids.contains(uuid.toString())) {
            ids.add(uuid.toString());
            write(ids);
        }
    }

    static synchronized void remove(UUID uuid) {
        List<String> ids = read();
        if (ids.remove(uuid.toString())) {
            write(ids);
        }
    }

    private static List<String> read() {
        if (file == null || !file.exists()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(YamlConfiguration.loadConfiguration(file).getStringList("players"));
    }

    private static void write(List<String> ids) {
        if (file == null) {
            return;
        }
        try {
            if (ids.isEmpty() && (!file.exists() || file.delete())) {
                return;
            }
            YamlConfiguration yml = new YamlConfiguration();
            yml.set("players", ids);
            file.getParentFile().mkdirs();
            yml.save(file);
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.WARNING, "[YukiNoaAPI] Failed to save temp-op.yml", e);
        }
    }
}
