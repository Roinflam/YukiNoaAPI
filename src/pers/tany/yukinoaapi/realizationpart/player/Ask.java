package pers.tany.yukinoaapi.realizationpart.player;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import pers.tany.yukinoaapi.Main;
import pers.tany.yukinoaapi.interfacepart.player.IAsk;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;

/**
 * 让玩家回答问题，并获取结果
 */
public class Ask implements IAsk {
    private final Player player;
    private final CompletableFuture<String> future = new CompletableFuture<>();
    private final CompletableFuture<Reason> futureReason = new CompletableFuture<>();
    
    private final AskMessage askMessage;

    /**
     * 创建一个提问对象
     *
     * @param player 被提问的玩家
     */
    public Ask(Player player) {
        this.player = player;
        askMessage = new AskMessage(player, future, futureReason, 0);
    }

    /**
     * 创建一个提问对象
     *
     * @param player 被提问的玩家
     * @param second 限时秒数
     */
    public Ask(Player player, int second) {
        this.player = player;
        askMessage = new AskMessage(player, future, futureReason, second);
        Bukkit.getPluginManager().registerEvents(askMessage, Main.plugin);
    }

    @Nullable
    @Override
    public String getAnswer() {
        try {
            return future.get();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Nullable
    @Override
    public Reason getReason() {
        try {
            return futureReason.get();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public Player getPlayer() {
        return player;
    }

    
    @Override
    public IAsk setCancel(boolean cancel) {
        askMessage.cancel = cancel;
        return this;
    }

    
    @Override
    public IAsk setBanCommand(boolean banCommand) {
        askMessage.banCommand = banCommand;
        return this;
    }

    
    @Override
    public IAsk setQuitClose(boolean close) {
        askMessage.close = close;
        return this;
    }

    @Override
    public void close() {
        askMessage.close(null, Reason.manualControl);
    }

    public enum Reason {
        /**
         * 回答结束
         */
        answer,
        /**
         * 限时回答结束
         */
        when,
        /**
         * 退出服务器结束
         */
        quit,
        /**
         * 手动结束
         */
        manualControl;

        /**
         * 玩家是否回答
         *
         * @return 回答为true
         */
        public boolean isAnswer() {
            return this.equals(answer);
        }

        /**
         * 玩家是否退出
         *
         * @return 退出为true
         */
        public boolean isQuit() {
            return this.equals(quit);
        }
    }

    static class AskMessage implements Listener {
        private final CompletableFuture<String> future;
        private final CompletableFuture<Reason> futureReason;
        private final Player player;
        private boolean cancel = true;
        private boolean close = true;
        private boolean banCommand = true;

        private AskMessage(Player player, CompletableFuture<String> future, CompletableFuture<Reason> futureReason, int second) {
            this.player = player;
            this.future = future;
            this.futureReason = futureReason;
            Bukkit.getPluginManager().registerEvents(this, Main.plugin);
            if (second > 0) {
                new BukkitRunnable() {
                    private int time = 0;

                    @Override
                    public void run() {
                        if (++time > second) {
                            this.cancel();
                            close(null, Reason.when);
                        }
                    }

                }.runTaskTimer(Main.plugin, 20, 20);
            }
        }

        @EventHandler(priority = EventPriority.LOWEST)
        private void onAsyncPlayerChat(AsyncPlayerChatEvent evt) {
            if (evt.getPlayer().getName().equals(player.getName())) {
                evt.setCancelled(cancel);
                close(evt.getMessage(), Reason.answer);
            }
        }

        @EventHandler
        private void onPlayerQuit(PlayerQuitEvent evt) {
            if (evt.getPlayer().getName().equals(player.getName())) {
                if (close) {
                    close(null, Reason.quit);
                }
            }
        }

        @EventHandler
        private void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent evt) {
            if (evt.getPlayer().getName().equals(player.getName())) {
                evt.setCancelled(cancel);
            }
        }

        private void close(String message, Reason reason) {
            future.complete(message);
            futureReason.complete(reason);
            HandlerList.unregisterAll(this);
        }
    }
}

