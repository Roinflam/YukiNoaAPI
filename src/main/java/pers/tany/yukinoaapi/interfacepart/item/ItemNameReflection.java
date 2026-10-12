package pers.tany.yukinoaapi.interfacepart.item;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * {@link IItem#getName} 方案1（{@code Item.getName(ItemStack)}）反射链的静态检查。
 *
 * <p>为什么需要：方案1按候选方法名依次反射，混淆名在不同版本里会撞到别的方法。
 * 例如 1.20.1 的 Spigot 映射里 {@code Item.h(ItemStack)} 是 {@code getTooltipImage}（返回 Optional），
 * 而 {@code getName} 是 {@code m}；候选名 {@code "h"} 排在前面，于是拿到的是 getTooltipImage。
 * 它的返回值不可能是 IChatBaseComponent，下一步 {@code getString.invoke} 必然抛
 * IllegalArgumentException（接收者类型不对），再由 catch 落到方案2。
 * 结果没错，但每取一次名字都要多调一次方法、构造一次异常并填充调用栈，频繁调用时开销很可观。</p>
 *
 * <p>这里只做「从声明的类型上就能断定必然失败」的判断：断定失败时直接走方案2，
 * 结果与原来抛异常后落到方案2完全相同；判断不了的一律照旧走方案1。</p>
 *
 * @author Roinflam
 */
final class ItemNameReflection {

    private ItemNameReflection() {
    }

    /**
     * 方案1的三步反射能否走通（只排除从类型上就能断定必然失败的情况）。
     *
     * @param getItem     ItemStack.getItem
     * @param getNameItem Item.getName(ItemStack)
     * @param getString   IChatBaseComponent.getString
     * @return false 表示方案1必然抛异常，可以直接走方案2
     */
    static boolean nameByItemUsable(Method getItem, Method getNameItem, Method getString) {
        if (getItem == null || getNameItem == null || getString == null) {
            // 原写法在这里会抛 NullPointerException，同样落到方案2
            return false;
        }
        return mayFeed(getItem, getNameItem) && mayFeed(getNameItem, getString);
    }

    /**
     * producer 的返回值有没有可能作为 consumer 的调用对象（接收者）。
     *
     * <p>consumer 是实例方法时，Method.invoke 要求接收者是其声明类的实例，否则抛 IllegalArgumentException；
     * 接收者为 null 时抛 NullPointerException。下面几种情况可以断定返回值不可能是声明类的实例：</p>
     * <ul>
     *   <li>返回 void：invoke 得到 null；</li>
     *   <li>返回基本类型：得到的是包装类对象，而声明类不是包装类的父类型；</li>
     *   <li>返回 final 类（不含数组）且不是声明类的子类型：运行期对象的类型就是它本身。</li>
     * </ul>
     * 其余情况（接口、非 final 类、数组、consumer 是静态方法）都可能成立，返回 true。
     */
    private static boolean mayFeed(Method producer, Method consumer) {
        if (Modifier.isStatic(consumer.getModifiers())) {
            // 静态方法不检查接收者
            return true;
        }
        Class<?> receiverType = consumer.getDeclaringClass();
        Class<?> returnType = producer.getReturnType();
        if (returnType == void.class) {
            return false;
        }
        if (returnType.isPrimitive()) {
            return receiverType.isAssignableFrom(wrapperOf(returnType));
        }
        if (receiverType.isAssignableFrom(returnType)) {
            return true;
        }
        if (returnType.isArray()) {
            return true;
        }
        return !Modifier.isFinal(returnType.getModifiers());
    }

    /**
     * 基本类型对应的包装类（invoke 返回的就是它）。
     */
    private static Class<?> wrapperOf(Class<?> primitive) {
        if (primitive == boolean.class) {
            return Boolean.class;
        }
        if (primitive == byte.class) {
            return Byte.class;
        }
        if (primitive == char.class) {
            return Character.class;
        }
        if (primitive == short.class) {
            return Short.class;
        }
        if (primitive == int.class) {
            return Integer.class;
        }
        if (primitive == long.class) {
            return Long.class;
        }
        if (primitive == float.class) {
            return Float.class;
        }
        return Double.class;
    }
}
