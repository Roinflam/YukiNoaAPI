package pers.tany.yukinoaapi.interfacepart.item;

import org.bukkit.inventory.ItemStack;
import pers.tany.yukinoaapi.util.LogUtil;
import pers.tany.yukinoaapi.util.NMSUtil;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.Optional;

/**
 * 1.20.5+（DataComponents 时代）物品与 SNBT 之间的转换
 * <p>
 * 装了 NBT-API 时交给它（{@link NbtApiItemBridge}）；没装时用服务端自己的
 * {@code ItemStack.save(HolderLookup.Provider)} / {@code ItemStack.parse(Provider, Tag)} / {@code TagParser.parseTag}。
 * 两边产出的都是原版的 {@code {id, count, components}}，可以互相读取，装不装 NBT-API 存下的数据不会因此不兼容。
 * </p>
 * <p>
 * 单独成类：这里只引用 Bukkit 和反射，不会因为缺少 NBT-API 而加载失败。
 * </p>
 */
final class ModernItemNbt {

    /** 服务端是否装了 NBT-API */
    private static final boolean NBT_API = classExists("de.tr7zw.nbtapi.NBT");

    /** 自定义数据所在的物品组件 */
    private static final String CUSTOM_DATA = "minecraft:custom_data";

    private static volatile boolean fallbackNoticed;

    /** CompoundTag.getCompound(String) / put(String, Tag) / merge(CompoundTag)，第一次合并时查找 */
    private static Method getCompound;
    private static Method put;
    private static Method merge;

    private ModernItemNbt() {
    }

    static String toSnbt(ItemStack itemStack) throws Exception {
        if (NBT_API) {
            return NbtApiItemBridge.toSnbt(itemStack);
        }
        return saveTag(itemStack).toString();
    }

    @Nullable
    static ItemStack fromSnbt(String tag) throws Exception {
        if (NBT_API) {
            return NbtApiItemBridge.fromSnbt(tag);
        }
        return parseItem(NMSUtil.parse.invoke(null, tag));
    }

    /**
     * 把 NBT 合并进物品的自定义数据（minecraft:custom_data 组件），和 NBT-API 的 {@code NBT.modify} 一致
     */
    @Nullable
    static ItemStack merge(ItemStack itemStack, String tag) throws Exception {
        if (NBT_API) {
            return NbtApiItemBridge.merge(itemStack, tag);
        }
        Object full = saveTag(itemStack);
        Object custom = NMSUtil.parse.invoke(null, tag);
        resolveCompoundMethods(full.getClass());

        Object components = getCompound.invoke(full, "components");
        Object customData = getCompound.invoke(components, CUSTOM_DATA);
        merge.invoke(customData, custom);
        // getCompound 在键不存在时返回的是新建的空标签，不会自动挂回去，逐层放回
        put.invoke(components, CUSTOM_DATA, customData);
        put.invoke(full, "components", components);
        return parseItem(full);
    }

    private static Object saveTag(ItemStack itemStack) throws Exception {
        noticeFallback();
        Object provider = requireProvider();
        Object nmsItem = NMSUtil.asNMSCopy.invoke(null, itemStack);
        return NMSUtil.save_ItemStack_v2.invoke(nmsItem, provider);
    }

    @Nullable
    private static ItemStack parseItem(Object compound) throws Exception {
        noticeFallback();
        Object provider = requireProvider();
        Object result = NMSUtil.parse_ItemStack_static.invoke(null, provider, compound);
        if (result instanceof Optional) {
            Optional<?> optional = (Optional<?>) result;
            if (!optional.isPresent()) {
                return null;
            }
            result = optional.get();
        }
        return (ItemStack) NMSUtil.asBukkitCopy.invoke(null, result);
    }

    private static Object requireProvider() {
        if (NMSUtil.save_ItemStack_v2 == null || NMSUtil.parse_ItemStack_static == null || NMSUtil.parse == null) {
            throw new UnsupportedOperationException("当前服务端没有可用的物品序列化接口，请安装 NBT-API");
        }
        Object provider = NMSUtil.getRegistryLookup();
        if (provider == null) {
            throw new IllegalStateException("取不到服务端注册表（服务器可能还没启动完成）");
        }
        return provider;
    }

    private static void resolveCompoundMethods(Class<?> compoundClass) throws NoSuchMethodException {
        if (getCompound != null && put != null && merge != null) {
            return;
        }
        // Paper 系（含 Mohist / Youer 等混合端）运行期是 Mojang 名；纯 Spigot 是混淆名
        getCompound = findMethod(compoundClass, new String[]{"getCompound", "p"}, String.class);
        put = findMethod(compoundClass, new String[]{"put", "a"}, String.class, NMSUtil.tagClass);
        merge = findMethod(compoundClass, new String[]{"merge", "a"}, compoundClass);
    }

    private static Method findMethod(Class<?> owner, String[] names, Class<?>... params) throws NoSuchMethodException {
        for (String name : names) {
            try {
                Method method = owner.getMethod(name, params);
                if (!name.equals("getCompound") && !name.equals("p")) {
                    return method;
                }
                // getCompound 的混淆名可能撞到别的单参数方法，按返回类型核对
                if (method.getReturnType() == owner) {
                    return method;
                }
            } catch (NoSuchMethodException ignored) {
                // 试下一个名字
            }
        }
        throw new NoSuchMethodException(owner.getName() + " 上找不到 " + names[0]);
    }

    private static void noticeFallback() {
        if (!fallbackNoticed) {
            fallbackNoticed = true;
            LogUtil.info("[IItem] 未安装 NBT-API，1.20.5+ 物品 NBT 改用服务端内置接口读写");
        }
    }

    private static boolean classExists(String name) {
        try {
            Class.forName(name, false, ModernItemNbt.class.getClassLoader());
            return true;
        } catch (Throwable t) {
            return false;
        }
    }
}
