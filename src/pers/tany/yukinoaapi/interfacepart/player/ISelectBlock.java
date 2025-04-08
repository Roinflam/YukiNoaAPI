package pers.tany.yukinoaapi.interfacepart.player;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import pers.tany.yukinoaapi.realizationpart.player.SelectBlock;

import javax.annotation.Nullable;

/**
 * 玩家选择方块相关
 */
public interface ISelectBlock {
    /**
     * 获取玩家点击的方块
     *
     * @return 方块
     */
    @Nullable
    Block getSelect();

    /**
     * 获取点击结束的原因
     *
     * @return 结束原因
     */
    @Nullable
    SelectBlock.ClickSource getClickSource();

    /**
     * 获取被要求点击方块的玩家
     *
     * @return 被要求点击方块的玩家
     */
    Player getPlayer();

    /**
     * 设置点击时事件的状态(默认为true)
     *
     * @param cancel 事件是否取消
     * @return 修改后的对象
     */
    ISelectBlock setCancel(boolean cancel);

    /**
     * 是否禁止输入指令(默认为true)
     *
     * @param ban 是否禁止
     * @return 修改后的对象
     */
    ISelectBlock setBanCommand(boolean ban);

    /**
     * 当玩家掉线了，是否关闭选择并返回null(默认为true)
     *
     * @param close 是否关闭
     * @return 修改后的对象
     */
    ISelectBlock setQuitClose(boolean close);

    /**
     * 关闭选择并返回null
     */
    void close();
}
