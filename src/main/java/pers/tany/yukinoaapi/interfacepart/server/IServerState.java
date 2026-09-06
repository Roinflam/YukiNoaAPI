package pers.tany.yukinoaapi.interfacepart.server;

import pers.tany.yukinoaapi.util.NMSUtil;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * 服务器状态相关
 */
public final class IServerState {

    /**
     * 工具类,禁止实例化
     *
     * @throws UnsupportedOperationException 总是抛出
     */
    private IServerState() {
        throw new UnsupportedOperationException("IServerState 是工具类,不允许实例化");
    }

    /**
     * 获取服务器当前MSPT
     *
     * @return 服务器当前延迟
     */
    public static double getMS() {
        try {
            Class<?> minecraftServer = NMSUtil.minecraftServer;
            Method getServer = NMSUtil.getServer;

            @Nullable Field h = null;
            for (Field field : getServer.invoke(minecraftServer).getClass().getFields()) {
                if (field.getType().getName().equalsIgnoreCase("[J")) {
                    h = field;
                    break;
                }
            }

            long average = 0;
            for (long b : (long[]) h.get(getServer.invoke(minecraftServer))) {
                average += b;
            }
            average /= ((long[]) h.get(getServer.invoke(minecraftServer))).length;

            return average * 1e-6;
        } catch (Exception a) {
            return 0;
        }
    }

    /**
     * 获取服务器当前的TPS
     *
     * @return 服务器当前的TPS
     */
    public static double getTPS() {
        try {
            Class<?> minecraftServer = NMSUtil.minecraftServer;
            Method getServer = NMSUtil.getServer;

            @Nullable Field h = null;
            for (Field field : getServer.invoke(minecraftServer).getClass().getFields()) {
                if (field.getType().getName().equalsIgnoreCase("[J")) {
                    h = field;
                    break;
                }
            }

            long average = 0;
            for (long b : (long[]) h.get(getServer.invoke(minecraftServer))) {
                average += b;
            }
            average /= ((long[]) h.get(getServer.invoke(minecraftServer))).length;

            return Math.min(1000 / (average * 1e-6), 20.0);
        } catch (Exception a) {
            return 0;
        }
    }
}


