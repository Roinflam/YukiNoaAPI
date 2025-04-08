package pers.tany.yukinoaapi.interfacepart.configuration;

import javax.annotation.Nonnull;

/**
 * 文件操作相关
 */
public interface IFile {
    /**
     * 根据window或linux系统把路径对应格式化
     *
     * @param path 路径
     * @return 格式化后的路径
     */
    
    static String formattedPath(String path) {
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            return path.replace("/", "\\");
        } else {
            return path.replace("\\", "/");
        }
    }
}
