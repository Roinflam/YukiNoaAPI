package pers.tany.yukinoaapi.realizationpart;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;
import pers.tany.yukinoaapi.Main;

import javax.annotation.Nullable;

/**
 * Vault操作库
 *
 * @author tany
 * @since 3.0.0
 */
public class VaultUtil {

    /** 经济系统实例 */
    @Nullable
    private static volatile Economy economy;

    private static boolean notFoundReported;

    static {
        lookup();
    }

    // 经济插件可能比调用方晚注册，取不到就留到下次再取，取到后缓存
    private static synchronized Economy lookup() {
        if (economy != null) {
            return economy;
        }
        RegisteredServiceProvider<Economy> registeredServiceProvider =
                Main.plugin.getServer().getServicesManager().getRegistration(Economy.class);

        if (registeredServiceProvider == null || registeredServiceProvider.getProvider() == null) {
            if (!notFoundReported) {
                notFoundReported = true;
                Bukkit.getConsoleSender().sendMessage(
                        ((Main) Main.plugin).getLanguageManager().getMessage("vault.economy-not-found"));
            }
            return null;
        }
        Bukkit.getConsoleSender().sendMessage(
                ((Main) Main.plugin).getLanguageManager().getMessage("vault.economy-found"));
        economy = registeredServiceProvider.getProvider();
        return economy;
    }

    /**
     * 获取Vault接口
     *
     * @return Vault接口
     */
    @Nullable
    public static Economy getEconomy() {
        Economy current = economy;
        return current != null ? current : lookup();
    }

    /**
     * 查看玩家是否有这么多游戏币
     *
     * @param name  玩家名
     * @param money 拥有的游戏币
     * @return 如果有则为true，否则为false
     */
    public static boolean hasMoney(String name, int money) {
        return getEconomy() != null && getEconomy().getBalance(name) >= money;
    }

    /**
     * 查看玩家是否有这么多游戏币
     *
     * @param name  玩家名
     * @param money 拥有的游戏币
     * @return 如果有则为true，否则为false
     */
    public static boolean hasMoney(String name, double money) {
        return getEconomy() != null && getEconomy().getBalance(name) >= money;
    }

    /**
     * 查看玩家是否有这么多游戏币
     *
     * @param offlinePlayer 玩家
     * @param money         拥有的游戏币
     * @return 如果有则为true，否则为false
     */
    public static boolean hasMoney(OfflinePlayer offlinePlayer, int money) {
        return getEconomy() != null && getEconomy().getBalance(offlinePlayer) >= money;
    }

    /**
     * 查看玩家是否有这么多游戏币
     *
     * @param offlinePlayer 玩家
     * @param money         拥有的游戏币
     * @return 如果有则为true，否则为false
     */
    public static boolean hasMoney(OfflinePlayer offlinePlayer, double money) {
        return getEconomy() != null && getEconomy().getBalance(offlinePlayer) >= money;
    }
}