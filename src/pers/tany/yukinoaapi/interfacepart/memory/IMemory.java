package pers.tany.yukinoaapi.interfacepart.memory;

public interface IMemory {

    /**
     * 获取当前java虚拟机中空闲内存
     *
     * @return 空闲内存(mb)
     */
    static long getFreeMemory() {
        return Runtime.getRuntime().freeMemory() / 1048576;
    }

    /**
     * 获取当前java虚拟机中已使用的内存
     *
     * @return 已使用的内存(mb)
     */
    static long getTotalMemory() {
        return Runtime.getRuntime().totalMemory() / 1048576;
    }

    /**
     * 获取当前java虚拟机中未使用的内存
     *
     * @return 已使用的内存(mb)
     */
    static long getUnusedMemory() {
        return (Runtime.getRuntime().maxMemory() - Runtime.getRuntime().totalMemory()) / 1048576;
    }

    /**
     * 获取当前java虚拟机中最大的内存
     *
     * @return 最大内存(mb)
     */
    static long getMaxMemory() {
        return Runtime.getRuntime().maxMemory();
    }

    /**
     * 回收当前jvm虚拟机内存
     *
     * @return 回收的内存(mb)
     */
    static long gc() {
        long before = Runtime.getRuntime().freeMemory();
        System.gc();
        return (Runtime.getRuntime().freeMemory() - before) / 1048576;
    }
}
