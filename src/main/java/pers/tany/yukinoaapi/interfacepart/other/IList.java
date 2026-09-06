package pers.tany.yukinoaapi.interfacepart.other;


import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * List操作相关
 */
public interface IList {
    /**
     * 把一个字符串列表里的字符串，替换为其他的
     *
     * @param list        涉及的列表
     * @param wasReplaced 被替换字符串
     * @param replace     替换后的字符串
     * @return 新的列表
     */
    
    static List<String> listReplace(Iterable<String> list, String wasReplaced, String replace) {
        ArrayList<String> newList = new ArrayList<>();
        for (String s : list) {
            newList.add(s.replace(wasReplaced, replace));
        }
        return newList;
    }

    /**
     * 打乱list的顺序
     *
     * @param list 涉及的list
     * @param <T>  泛型
     * @return list
     */
    
    static <T> List<T> upsetList(List<T> list) {
        for (int i = 0; i < list.size(); i++) {
            int random = IRandom.randomNumber(0, list.size() - 1);
            T t = list.get(random);
            list.set(random, list.get(i));
            list.set(i, t);
        }
        return list;
    }
}

