package pers.tany.yukinoaapi.interfacepart.item;

import de.tr7zw.nbtapi.NBT;
import de.tr7zw.nbtapi.iface.ReadWriteNBT;
import org.bukkit.inventory.ItemStack;

/**
 * 1.20.5+ 物品 NBT 的 NBT-API 实现
 * <p>
 * 对 NBT-API 的引用全部收在这里：{@code IItem} 里只要出现「ReadWriteNBT 传给 ReadableNBT 参数」这类写法，
 * JVM 校验 IItem 时就得加载 NBT-API 的类，服务端没装 NBT-API 时整个 IItem 接口都用不了（连 isEmpty 也会抛
 * NoClassDefFoundError）。本类只在 {@link ModernItemNbt} 确认 NBT-API 存在后才会被加载。
 * </p>
 */
final class NbtApiItemBridge {

    private NbtApiItemBridge() {
    }

    static String toSnbt(ItemStack itemStack) {
        ReadWriteNBT nbt = NBT.itemStackToNBT(itemStack);
        return nbt.toString();
    }

    static ItemStack fromSnbt(String tag) {
        ReadWriteNBT nbt = NBT.parseNBT(tag);
        return NBT.itemStackFromNBT(nbt);
    }

    static ItemStack merge(ItemStack itemStack, String tag) {
        // 克隆一份，避免修改入参原物品
        ItemStack result = itemStack.clone();
        ReadWriteNBT customNbt = NBT.parseNBT(tag);
        // 用语句块 lambda（无返回值）明确匹配 Consumer 重载，避免与 Function 重载产生歧义
        NBT.modify(result, nbt -> {
            nbt.mergeCompound(customNbt);
        });
        return result;
    }
}
