package pers.tany.yukinoaapi.interfacepart.stream;


import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.stream.Stream;

/**
 * 并行遍历容器
 */
public interface IParallel {

    /**
     * 返回平行流
     *
     * @param <T>  泛型
     * @param list 列表
     * @return 如果长度大于1，则为平行
     */
    static <T> Stream<T> parallel(Collection<T> list) {
        Stream<T> stream = list.stream();
        if (list.size() > 1) {
            return stream.parallel();
        }
        return stream;
    }

    /**
     * 返回平行流
     *
     * @param <T>    泛型
     * @param length 长度
     * @param stream 流
     * @return 如果长度大于1，则为平行
     */
    
    static <T> Stream<T> parallel(int length, Stream<T> stream) {
        if (length > 1) {
            return stream.parallel();
        }
        return stream;
    }
}
