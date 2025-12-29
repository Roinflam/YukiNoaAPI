package pers.tany.yukinoaapi.realizationpart;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;
import pers.tany.yukinoaapi.Main;

import javax.annotation.Nullable;

/**
 * Vault操作库
 */
public class VaultUtil {
    @Nullable
    private static final Economy economy;

    static {
        RegisteredServiceProvider<Economy> registeredServiceProvider = (RegisteredServiceProvider<Economy>) Main.plugin.getServer().getServicesManager().getRegistration((Class) Economy.class);
        if (registeredServiceProvider == null) {
            Bukkit.getConsoleSender().sendMessage("§7[§fYukiNoaAPI§7]§c服务器未拥有经济系统！");
            economy = null;
        } else {
            Bukkit.getConsoleSender().sendMessage("§7[§fYukiNoaAPI§7]§a服务器拥有经济系统！");
            economy = registeredServiceProvider.getProvider();
        }
    }

    /**
     * 获取Vault接口
     *
     * @return Vault接口
     */
    @Nullable
    public static Economy getEconomy() {
        return economy;
    }

    /**
     * 查看玩家是否有这么多游戏币
     *
     * @param name  玩家名
     * @param money 拥有的游戏币
     * @return 如果有则为true，否则为false
     */
    public static boolean hasMoney(String name, int money) {
        return economy.getBalance(name) >= money;
    }

    /**
     * 查看玩家是否有这么多游戏币
     *
     * @param name  玩家名
     * @param money 拥有的游戏币
     * @return 如果有则为true，否则为false
     */
    public static boolean hasMoney(String name, double money) {
        return economy.getBalance(name) >= money;
    }

    /**
     * 查看玩家是否有这么多游戏币
     *
     * @param offlinePlayer 玩家
     * @param money         拥有的游戏币
     * @return 如果有则为true，否则为false
     */
    public static boolean hasMoney(OfflinePlayer offlinePlayer, int money) {
        return economy.getBalance(offlinePlayer) >= money;
    }

    /**
     * 查看玩家是否有这么多游戏币
     *
     * @param offlinePlayer 玩家
     * @param money         拥有的游戏币
     * @return 如果有则为true，否则为false
     */
    public static boolean hasMoney(OfflinePlayer offlinePlayer, double money) {
        return economy.getBalance(offlinePlayer) >= money;
    }
}
