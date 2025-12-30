package pers.tany.yukinoaapi.realizationpart.server;


import pers.tany.yukinoaapi.util.NMSUtil;

public enum Version {
    /**
     * 低版本特指1.7及以下
     */
    LOW,
    /**
     * 中版本特指1.8-1.12
     */
    MEDIUM,
    /**
     * 低版本特指1.13以上
     */
    High;

    Version() {

    }

    /**
     * 获取当前版本的等级枚举
     *
     * @return 等级枚举
     */
    
    public static Version getType() {
        if (NMSUtil.getVersion().contains("v1_4") || NMSUtil.getVersion().contains("v1_5") || NMSUtil.getVersion().contains("v1_6") || NMSUtil.getVersion().contains("v1_7")) {
            return LOW;
        } else if (NMSUtil.getVersion().contains("v1_8") || NMSUtil.getVersion().contains("v1_9") || NMSUtil.getVersion().contains("v1_10") || NMSUtil.getVersion().contains("v1_11") || NMSUtil.getVersion().contains("v1_12")) {
            return MEDIUM;
        } else {
            return High;
        }
    }

    /**
     * 判断是否为1.13+版本
     *
     * @return 是则true
     */
    public boolean isHigh() {
        return this == High;
    }

    /**
     * 判断是否为1.8-1.12版本
     *
     * @return 是则true
     */
    public boolean isMedium() {
        return this == MEDIUM;
    }

    /**
     * 判断是否为1.7-版本
     *
     * @return 是则true
     */
    public boolean isLOW() {
        return this == LOW;
    }
}
