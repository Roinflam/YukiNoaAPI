package pers.tany.yukinoaapi.realizationpart.container;

import javax.annotation.Nonnull;

/**
 * 键对值容器
 *
 * @param <K> 键
 * @param <V> 值
 */
public class KeyToValue<K, V> {
    private K key;
    private V value;

    /**
     * 一个键对值容器
     *
     * @param k 继承的键
     * @param v 继承的值
     */
    public KeyToValue(K k, V v) {
        key = k;
        value = v;
    }

    /**
     * 继承容器
     *
     * @param keyToValue 被继承的容器数据
     */
    public KeyToValue(KeyToValue<K, V> keyToValue) {
        this.put(keyToValue.getKey(), keyToValue.getValue());
    }

    /**
     * 替换现有的键对值
     *
     * @param key   键
     * @param value 值
     * @return 修改后的对象
     */
    
    public synchronized KeyToValue<K, V> put(K key, V value) {
        this.key = key;
        this.value = value;
        return this;
    }

    /**
     * 替换现有的键
     *
     * @param key 键
     * @return 修改后的对象
     */
    
    public synchronized KeyToValue<K, V> putKey(K key) {
        this.key = key;
        return this;
    }

    /**
     * 替换现有的值
     *
     * @param value 值
     * @return 修改后的对象
     */
    
    public synchronized KeyToValue<K, V> putValue(V value) {
        this.value = value;
        return this;
    }

    /**
     * 获取键
     *
     * @return 键
     */
    public synchronized K getKey() {
        return key;
    }

    /**
     * 获取键
     *
     * @param k 如果为null，则返回此键
     * @return 键
     */
    public synchronized K getKeyOrDeafult(K k) {
        return key == null ? k : key;
    }

    /**
     * 获取值
     *
     * @return 值
     */
    public synchronized V getValue() {
        return value;
    }

    /**
     * 获取值
     *
     * @param v 如果为null，则返回此值
     * @return 值
     */
    public synchronized V getValueOrDeafult(V v) {
        return value == null ? v : value;
    }

    
    @Override
    public synchronized KeyToValue<K, V> clone() {
        return new KeyToValue<K, V>(this);
    }
}
