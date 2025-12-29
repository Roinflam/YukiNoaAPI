package pers.tany.yukinoaapi.realizationpart.container;


import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * 有序的键对值容器
 *
 * @param <K> 键
 * @param <V> 值
 */
public class OrderlyMap<K, V> {
    private final ArrayList<K> keyList = new ArrayList<>();
    private final ArrayList<V> valueList = new ArrayList<>();

    /**
     * 可以这么理解，这是一个有序的HashMap容器
     * 每次遍历它的顺序和传入  顺序一样，还可以根据索引获取键
     * 它的实现非常硬核
     */
    public OrderlyMap() {

    }

    /**
     * 继承容器
     *
     * @param orderlyHashMap 被继承的容器数据
     */
    public OrderlyMap(OrderlyMap<K, V> orderlyHashMap) {
        keyList.addAll(orderlyHashMap.keySet());
        valueList.addAll(orderlyHashMap.values());
    }

    /**
     * 排序Map的结果
     *
     * @param map           涉及的Map
     * @param positiveOrder 如果为true则从小到大，否则从大到小
     * @param c             涉及到的类
     */
    public OrderlyMap(Map<K, V> map, boolean positiveOrder, Class<?> c) {
        if (c.equals(Integer.class)) {
            HashMap<K, Integer> hashMap = new HashMap<K, Integer>((Map<? extends K, ? extends Integer>) map);
            List<Map.Entry<K, Integer>> baltop = new ArrayList<Map.Entry<K, Integer>>(hashMap.entrySet());
            baltop.sort(new Comparator<Map.Entry<K, Integer>>() {
                @Override
                public synchronized int compare(Map.Entry<K, Integer> small, Map.Entry<K, Integer> big) {
                    return positiveOrder ? small.getValue().compareTo(big.getValue()) : big.getValue().compareTo(small.getValue());
                }
            });
            for (Map.Entry<K, Integer> kIntegerEntry : baltop) {
                this.put(kIntegerEntry.getKey(), (V) kIntegerEntry.getValue());
            }
        } else if (c.equals(Double.class)) {
            HashMap<K, Double> hashMap = new HashMap<K, Double>((Map<? extends K, ? extends Double>) map);
            List<Map.Entry<K, Double>> baltop = new ArrayList<Map.Entry<K, Double>>(hashMap.entrySet());
            baltop.sort(new Comparator<Map.Entry<K, Double>>() {
                @Override
                public synchronized int compare(Map.Entry<K, Double> small, Map.Entry<K, Double> big) {
                    return positiveOrder ? small.getValue().compareTo(big.getValue()) : big.getValue().compareTo(small.getValue());
                }
            });
            for (Map.Entry<K, Double> kDoubleEntry : baltop) {
                this.put(kDoubleEntry.getKey(), (V) kDoubleEntry.getValue());
            }
        }
    }

    /**
     * 将指定的值与此映射中的指定键关联。如果映射以前包含一个键的映射，旧值将被替换。
     *
     * @param key   键
     * @param value 值
     * @return 修改后的对象
     */
    
    public synchronized OrderlyMap<K, V> put(K key, V value) {
        int index = keyList.indexOf(key);
        if (index != -1) {
            valueList.set(index, value);
        } else {
            keyList.add(key);
            valueList.add(value);
        }
        return this;
    }

    /**
     * 移除这个键和对应的值
     *
     * @param key 键
     * @return 如果键存在并被移除，返回true，否则为false
     */
    public synchronized boolean remove(K key) {
        int index = keyList.indexOf(key);
        if (index != -1) {
            keyList.remove(index);
            valueList.remove(index);
            return true;
        } else {
            return false;
        }
    }

    /**
     * 通过索引获取值
     *
     * @param index 索引
     * @return 索引下对应的值
     */
    public synchronized V get(int index) {
        return valueList.get(index);
    }

    /**
     * 根据索引获取键
     *
     * @param index 索引
     * @return 索引下对应的键
     */
    public synchronized K getKey(int index) {
        return keyList.get(index);
    }

    /**
     * 通过键来获取值
     *
     * @param key 键
     * @return 键所对的值
     */
    @Nullable
    public synchronized V getValue(K key) {
        int index = keyList.indexOf(key);
        if (index == -1) {
            return null;
        }
        return valueList.get(index);
    }

    /**
     * 通过键来获取值
     * 如果键不存在则返回个默认值
     *
     * @param key   键
     * @param value 默认值
     * @return 键所对的值
     */
    public synchronized V getOrDefault(K key, V value) {
        int index = keyList.indexOf(key);
        if (index == -1) {
            return value;
        }
        return valueList.get(index);
    }

    /**
     * 以List形式返回所有键
     *
     * @return 所有键的集合
     */
    
    public synchronized ArrayList<K> keySet() {
        return keyList;
    }

    /**
     * 以List形式返回所有值
     *
     * @return 所有值的集合
     */
    
    public synchronized ArrayList<V> values() {
        return valueList;
    }

    /**
     * 获取容器的大小
     *
     * @return 容器的大小
     */
    public synchronized int size() {
        return keyList.size();
    }

    /**
     * 查看容器是否包含某个键
     *
     * @param key 键
     * @return 包含则true，否则为false
     */
    public synchronized boolean containsKey(K key) {
        return keyList.contains(key);
    }

    /**
     * 查看容器是否包含某个值
     *
     * @param value 值
     * @return 包含则true，否则为false
     */
    public synchronized boolean containsValue(V value) {
        return valueList.contains(value);
    }

    /**
     * 清空容器里所有的内容
     *
     * @return 修改后的对象
     */
    
    public synchronized OrderlyMap<K, V> clear() {
        keyList.clear();
        valueList.clear();
        return this;
    }

    
    @Override
    public synchronized OrderlyMap<K, V> clone() {
        return new OrderlyMap<K, V>(this);
    }
}
