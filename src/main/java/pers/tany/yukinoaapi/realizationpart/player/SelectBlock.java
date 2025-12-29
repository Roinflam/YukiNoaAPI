package pers.tany.yukinoaapi.realizationpart.player;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import pers.tany.yukinoaapi.Main;
import pers.tany.yukinoaapi.interfacepart.player.ISelectBlock;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;

/**
 * 让玩家点击方块，并获取点击结果
 */
public class SelectBlock implements ISelectBlock {
    private final Player player;
    private final CompletableFuture<Block> future = new CompletableFuture<>();
    private final CompletableFuture<ClickSource> futureSource = new CompletableFuture<>();
    
    private final Select select;

    /**
     * 创建一个提问对象
     *
     * @param player 被提问的玩家
     */
    public SelectBlock(Player player) {
        this.player = player;
        select = new Select(player, future, futureSource, 0);
        Bukkit.getPluginManager().registerEvents(select, Main.plugin);
    }

    /**
     * 创建一个提问对象
     *
     * @param player 被提问的玩家
     * @param second 限时秒数
     */
    public SelectBlock(Player player, int second) {
        this.player = player;
        select = new Select(player, future, futureSource, second);
        Bukkit.getPluginManager().registerEvents(select, Main.plugin);
    }

    @Nullable
    @Override
    public Block getSelect() {
        try {
            return future.get();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Nullable
    @Override
    public ClickSource getClickSource() {
        try {
            return futureSource.get();
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
    public ISelectBlock setCancel(boolean cancel) {
        select.cancel = cancel;
        return this;
    }

    
    @Override
    public ISelectBlock setBanCommand(boolean banCommand) {
        select.banCommand = banCommand;
        return this;
    }

    
    @Override
    public ISelectBlock setQuitClose(boolean close) {
        select.close = close;
        return this;
    }

    @Override
    public void close() {
        select.close(null, ClickSource.manualControl);
    }

    public enum ClickSource {
        /**
         * 右键点击方块
         */
        rightClick,
        /**
         * 左键点击方块
         */
        leftClick,
        /**
         * 限时点击结束
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
         * 玩家是否点击成功
         *
         * @return 成功为true
         */
        public boolean isClick() {
            return this.equals(rightClick) || this.equals(leftClick);
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

    static class Select implements Listener {
        private final CompletableFuture<Block> future;
        private final CompletableFuture<ClickSource> futureSource;
        private final Player player;
        private boolean cancel = true;
        private boolean close = false;
        private boolean banCommand = true;

        private Select(Player player, CompletableFuture<Block> future, CompletableFuture<ClickSource> futureSource, int second) {
            this.player = player;
            this.future = future;
            this.futureSource = futureSource;
            if (second > 0) {
                new BukkitRunnable() {
                    private int time = 0;

                    @Override
                    public void run() {
                        if (++time > second) {
                            this.cancel();
                            close(null, ClickSource.when);
                        }
                    }

                }.runTaskTimer(Main.plugin, 20, 20);
            }
        }

        @EventHandler
        private void onPlayerInteract(PlayerInteractEvent evt) {
            if (evt.getPlayer().getName().equals(player.getName())) {
                if (evt.getAction().equals(Action.LEFT_CLICK_BLOCK)) {
                    evt.setCancelled(cancel);
                    close(evt.getClickedBlock(), ClickSource.leftClick);
                } else if (evt.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
                    evt.setCancelled(cancel);
                    close(evt.getClickedBlock(), ClickSource.rightClick);
                }
            }
        }

        @EventHandler
        private void onPlayerQuit(PlayerQuitEvent evt) {
            if (evt.getPlayer().getName().equals(player.getName())) {
                if (close) {
                    close(null, ClickSource.quit);
                }
            }
        }

        @EventHandler
        private void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent evt) {
            if (evt.getPlayer().getName().equals(player.getName())) {
                evt.setCancelled(cancel);
            }
        }

        private void close(Block block, ClickSource clickSource) {
            future.complete(block);
            futureSource.complete(clickSource);
            HandlerList.unregisterAll(this);
        }
    }
}

