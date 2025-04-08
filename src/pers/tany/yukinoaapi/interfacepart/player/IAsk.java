package pers.tany.yukinoaapi.interfacepart.player;

import org.bukkit.entity.Player;
import pers.tany.yukinoaapi.realizationpart.player.Ask;

import javax.annotation.Nullable;

/**
 * 玩家回答提问相关
 */
public interface IAsk {
    /**
     * 获取玩家回答的答案
     *
     * @return 回答
     */
    @Nullable
    String getAnswer();

    /**
     * 获取回答结束的方法
     *
     * @return 回答
     */
    @Nullable
    Ask.Reason getReason();

    /**
     * 获取被提问的玩家
     *
     * @return 被提问的玩家
     */
    Player getPlayer();

    /**
     * 设置回答时事件的状态(默认为true)
     *
     * @param cancel 事件是否取消
     * @return 修改后的对象
     */
    IAsk setCancel(boolean cancel);

    /**
     * 是否禁止输入指令(默认为true)
     *
     * @param ban 是否禁止
     * @return 修改后的对象
     */
    IAsk setBanCommand(boolean ban);

    /**
     * 当玩家掉线了，是否关闭提问并返回null(默认为true)
     *
     * @param close 是否关闭
     * @return 修改后的对象
     */
    IAsk setQuitClose(boolean close);

    /**
     * 关闭提问并返回null
     */
    void close();
}
