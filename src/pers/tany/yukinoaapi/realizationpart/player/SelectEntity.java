package pers.tany.yukinoaapi.realizationpart.player;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import pers.tany.yukinoaapi.Main;
import pers.tany.yukinoaapi.interfacepart.player.ISelectEntity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;

/**
 * 让玩家点击方块，并获取点击结果
 */
public class SelectEntity implements ISelectEntity {
    private final Player player;
    private final CompletableFuture<Entity> future = new CompletableFuture<>();
    private final CompletableFuture<InteractSource> futureSource = new CompletableFuture<>();
    
    private final Select select;

    /**
     * 创建一个要求选择对象
     *
     * @param player 被要求选择的玩家
     */
    public SelectEntity(Player player) {
        this.player = player;
        select = new Select(player, future, futureSource, 0);
        Bukkit.getPluginManager().registerEvents(select, Main.plugin);
    }

    /**
     * 创建一个要求选择对象
     *
     * @param player 被要求选择的玩家
     * @param second 限时秒数
     */
    public SelectEntity(Player player, int second) {
        this.player = player;
        select = new Select(player, future, futureSource, second);
        Bukkit.getPluginManager().registerEvents(select, Main.plugin);
    }

    @Nullable
    @Override
    public Entity getSelect() {
        try {
            return future.get();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Nullable
    @Override
    public InteractSource getInteractSource() {
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
    public ISelectEntity setCancel(boolean cancel) {
        select.cancel = cancel;
        return this;
    }

    
    @Override
    public ISelectEntity setBanCommand(boolean banCommand) {
        select.banCommand = banCommand;
        return this;
    }

    
    @Override
    public ISelectEntity setQuitClose(boolean close) {
        select.close = close;
        return this;
    }

    @Override
    public void close() {
        select.close(null, InteractSource.manualControl);
    }

    public enum InteractSource {
        /**
         * 右键交互的实体
         */
        rightInteract,
        /**
         * 左键伤害的实体
         */
        leftDamage,
        /**
         * 限时交互结束
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
         * 玩家是否选择成功
         *
         * @return 成功为true
         */
        public boolean isClick() {
            return this.equals(rightInteract) || this.equals(leftDamage);
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
        private final CompletableFuture<Entity> future;
        private final CompletableFuture<InteractSource> futureSource;
        private final Player player;
        private boolean cancel = true;
        private boolean close = true;
        private boolean banCommand = true;

        private Select(Player player, CompletableFuture<Entity> future, CompletableFuture<InteractSource> futureSource, int second) {
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
                            close(null, InteractSource.when);
                        }
                    }

                }.runTaskTimer(Main.plugin, 20, 20);
            }
        }

        @EventHandler
        private void onPlayerInteract(PlayerInteractEntityEvent evt) {
            if (evt.getPlayer().getName().equals(player.getName())) {
                evt.setCancelled(cancel);
                close(evt.getRightClicked(), InteractSource.rightInteract);
            }
        }

        @EventHandler
        private void onEntityDamageByEntity(EntityDamageByEntityEvent evt) {
            if (evt.getDamager() instanceof Player) {
                evt.setCancelled(cancel);
                close(evt.getEntity(), InteractSource.leftDamage);
            }
        }

        @EventHandler
        private void onPlayerQuit(PlayerQuitEvent evt) {
            if (evt.getPlayer().getName().equals(player.getName())) {
                if (close) {
                    close(null, InteractSource.quit);
                }
            }
        }

        @EventHandler
        private void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent evt) {
            if (evt.getPlayer().getName().equals(player.getName())) {
                evt.setCancelled(cancel);
            }
        }

        private void close(Entity entity, InteractSource interactSource) {
            future.complete(entity);
            futureSource.complete(interactSource);
            HandlerList.unregisterAll(this);
        }
    }
}

