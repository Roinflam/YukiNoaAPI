package pers.tany.yukinoaapi.interfacepart.register;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import javax.annotation.Nonnull;


public final class IRegister {

    /**
     * 工具类,禁止实例化
     *
     * @throws UnsupportedOperationException 总是抛出
     */
    private IRegister() {
        throw new UnsupportedOperationException("IRegister 是工具类,不允许实例化");
    }

    /**
     * 注册指令
     *
     * @param javaPlugin      涉及插件的主类
     * @param name            指令名称
     * @param commandExecutor 指令相关的类
     */
    public static void registerCommands(JavaPlugin javaPlugin, String name, CommandExecutor commandExecutor) {
        javaPlugin.getCommand(name).setExecutor(commandExecutor);
    }

    /**
     * 注册监听
     *
     * @param plugin   主类
     * @param listener 监听对象
     */
    public static void registerEvents(Plugin plugin, Listener listener) {
        Bukkit.getPluginManager().registerEvents(listener, plugin);
    }
}


