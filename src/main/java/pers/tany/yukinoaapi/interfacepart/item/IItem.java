package pers.tany.yukinoaapi.interfacepart.item;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import pers.tany.yukinoaapi.util.LogUtil;
import pers.tany.yukinoaapi.util.NMSUtil;
import pers.tany.yukinoaapi.realizationpart.builder.ItemBuilder;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.Optional;

/**
 * 物品堆相关
 * <p>
 * 支持 1.7-1.21+ 全版本物品NBT操作。
 * 1.20.5+ 使用 DataComponents 新API（save/parse + HolderLookup.Provider），
 * 旧版使用传统 NBTTagCompound 方式。
 * </p>
 */
public interface IItem {

    /**
     * 检查玩家手上是否为空
     *
     * @param player 涉及玩家
     * @return 如果空则为true，否则为false
     */
    static boolean isEmptyHand(Player player) {
        return isEmpty(player.getItemInHand());
    }

    /**
     * 检查物品是否为空
     *
     * @param itemStack 涉及物品
     * @return 如果空则为true，否则为false
     */
    static boolean isEmpty(@Nullable ItemStack itemStack) {
        return itemStack == null || itemStack.getType().equals(Material.AIR);
    }

    /**
     * 返回物品名称
     * 优先自定义，其次翻译后的名称，如果没有翻译则为Material枚举名
     *
     * @param itemStack 涉及的物品
     * @return 物品名称
     */
    @Nullable
    static String getName(ItemStack itemStack) {
        if (itemStack.getItemMeta() != null && itemStack.getItemMeta().hasDisplayName()) {
            return itemStack.getItemMeta().getDisplayName();
        }
        try {
            Method asNMSCopy = NMSUtil.asNMSCopy;
            Object nmsItem = asNMSCopy.invoke(null, itemStack);
            String name;
            try {
                // 方案1: Item.getName(ItemStack) - 旧版本
                Method getItem = NMSUtil.getItem;
                Method getName_Item = NMSUtil.getName_Item;
                Method getString = NMSUtil.getString_IChatBaseComponent;
                Object item = getItem.invoke(nmsItem);
                name = getString.invoke(getName_Item.invoke(item, nmsItem)).toString();
            } catch (Exception e) {
                // 方案2: ItemStack.getName() / getHoverName()
                Method getNameMethod = NMSUtil.getName_ItemStack;
                Object result = getNameMethod.invoke(nmsItem);

                // 判断返回类型：1.13+返回IChatBaseComponent，1.12-返回String
                if (result instanceof String) {
                    // 低版本直接返回String
                    name = (String) result;
                } else {
                    // 高版本返回Component，需要调用getString()
                    Method getString = NMSUtil.getString_IChatBaseComponent;
                    name = getString.invoke(result).toString();
                }
            }
            return name.split("\\.").length > 2 ? itemStack.getType().toString() : name;
        } catch (Exception e) {
            return itemStack.getType().toString();
        }
    }

    /**
     * 获取物品的NBT（完整SNBT字符串）
     * <p>
     * 1.20.4及以下：使用 NMS ItemStack.save(NBTTagCompound) 方式<br>
     * 1.20.5+：使用 NMS ItemStack.save(HolderLookup.Provider) 方式
     * </p>
     *
     * @param itemStack 涉及物品
     * @return 物品的完整NBT字符串（SNBT格式），失败返回空字符串
     */
    @Nullable
    static String getItemNBT(ItemStack itemStack) {
        try {
            Method asNMSCopy = NMSUtil.asNMSCopy;
            Object nmsItem = asNMSCopy.invoke(null, itemStack);

            // ============ 1.20.5+ 新版路径：save(HolderLookup.Provider) → Tag ============
            if (NMSUtil.isNewItemAPI()) {
                Method save = NMSUtil.save_ItemStack_v2;
                // 获取注册表上下文（HolderLookup.Provider）
                Object provider = NMSUtil.getRegistryLookup();
                if (provider == null) {
                    LogUtil.error("[IItem] 获取注册表上下文失败，无法序列化物品NBT");
                    return "";
                }
                // 调用 nmsItemStack.save(provider)，返回 Tag 对象
                Object tag = save.invoke(nmsItem, provider);
                return tag.toString();
            }

            // ============ 旧版路径：save(NBTTagCompound) → NBTTagCompound ============
            Class<?> nbttagCompound = NMSUtil.nbttagCompoundClass;
            Method save = NMSUtil.save_ItemStack;
            Object tag = nbttagCompound.newInstance();
            return save.invoke(nmsItem, tag).toString();

        } catch (Exception e) {
            LogUtil.error("[IItem] 获取物品NBT失败", e);
            return "";
        }
    }

    /**
     * 根据完整NBT字符串创建物品
     * <p>
     * 1.20.4及以下：解析NBT → 加载到虚拟物品 → 转为Bukkit物品<br>
     * 1.20.5+：解析NBT → ItemStack.parse(Provider, Tag) → 转为Bukkit物品
     * </p>
     *
     * @param tag 物品完整NBT字符串（SNBT格式）
     * @return 反序列化后的物品，失败返回 null
     */
    @Nullable
    static ItemStack getItemStack(String tag) {
        try {
            final Method parseSNBT = NMSUtil.parse;
            final Method asBukkitCopy = NMSUtil.asBukkitCopy;

            // 先将SNBT字符串解析为NBT对象（CompoundTag / NBTTagCompound）
            final Object nbttagCompound = parseSNBT.invoke(null, tag);

            // ============ 1.20.5+ 新版路径：ItemStack.parse(Provider, Tag) → Optional ============
            if (NMSUtil.isNewItemAPI()) {
                Method parseMethod = NMSUtil.parse_ItemStack_static;
                // 获取注册表上下文
                Object provider = NMSUtil.getRegistryLookup();
                if (provider == null) {
                    LogUtil.error("[IItem] 获取注册表上下文失败，无法反序列化物品");
                    return null;
                }
                // 调用静态方法 ItemStack.parse(provider, nbtTag)，返回 Optional<ItemStack>
                Object optionalResult = parseMethod.invoke(null, provider, nbttagCompound);
                // 处理 Optional 返回值
                if (optionalResult instanceof Optional) {
                    Optional<?> optional = (Optional<?>) optionalResult;
                    if (optional.isPresent()) {
                        Object nmsItem = optional.get();
                        return (ItemStack) asBukkitCopy.invoke(null, nmsItem);
                    }
                }
                LogUtil.warning("[IItem] 从NBT解析物品失败，NBT可能格式不正确: " + tag.substring(0, Math.min(tag.length(), 100)));
                return null;
            }

            // ============ 旧版路径：load(NBTTagCompound) 实例方法 ============
            final Method asNMSCopy = NMSUtil.asNMSCopy;
            final Method load = NMSUtil.load_ItemStack;
            // 创建一个虚拟物品作为载体
            final Object nmsItem = asNMSCopy.invoke(null, new ItemBuilder("STONE").getItemStack());
            // 将解析的NBT加载到虚拟物品上
            load.invoke(nmsItem, nbttagCompound);
            return (ItemStack) asBukkitCopy.invoke(null, nmsItem);

        } catch (Exception e) {
            LogUtil.error("[IItem] 从NBT字符串创建物品失败", e);
            return null;
        }
    }

    /**
     * 设置物品的自定义NBT标签
     * <p>
     * 1.20.4及以下：解析tag → setTag(nmsItem, compound) → 转为Bukkit物品<br>
     * 1.20.5+：先获取物品完整NBT → 合并自定义tag → 重新解析创建物品<br>
     * <b>注意：1.20.5+ 的 setTag 已移除，本方法通过 序列化→合并→反序列化 实现等效功能。
     * 传入的 tag 参数在旧版中是纯tag复合标签（如 {CustomModelData:1}），
     * 在新版中也支持此格式，但建议使用完整SNBT格式以确保兼容性。</b>
     * </p>
     *
     * @param itemStack 涉及物品
     * @param tag       物品NBT字符串
     * @return 设置NBT后的物品，失败返回 null
     */
    @Nullable
    static ItemStack getItemStack(ItemStack itemStack, String tag) {
        try {
            Method asNMSCopy = NMSUtil.asNMSCopy;
            Method parseSNBT = NMSUtil.parse;
            Method asBukkitCopy = NMSUtil.asBukkitCopy;

            // ============ 1.20.5+ 新版路径：序列化→合并→反序列化 ============
            if (NMSUtil.isNewItemAPI()) {
                Object provider = NMSUtil.getRegistryLookup();
                if (provider == null) {
                    LogUtil.error("[IItem] 获取注册表上下文失败，无法设置物品NBT");
                    return null;
                }

                // 步骤1：将当前物品序列化为完整NBT
                Object nmsItem = asNMSCopy.invoke(null, itemStack);
                Object itemTag = NMSUtil.save_ItemStack_v2.invoke(nmsItem, provider);

                // 步骤2：解析用户传入的tag字符串
                Object customTag = parseSNBT.invoke(null, tag);

                // 步骤3：将自定义tag合并到物品NBT中
                // 使用 CompoundTag.merge(CompoundTag) 或 putAll 方法合并
                Method mergeMethod = findMergeMethod(itemTag.getClass());
                if (mergeMethod != null) {
                    mergeMethod.invoke(itemTag, customTag);
                } else {
                    // 如果找不到 merge 方法，尝试直接用自定义tag创建物品
                    // 这种情况下 tag 应该是完整的物品SNBT
                    LogUtil.warning("[IItem] 未找到NBT合并方法，尝试直接用tag创建物品");
                    itemTag = customTag;
                }

                // 步骤4：从合并后的NBT重新创建物品
                Object optionalResult = NMSUtil.parse_ItemStack_static.invoke(null, provider, itemTag);
                if (optionalResult instanceof Optional) {
                    Optional<?> optional = (Optional<?>) optionalResult;
                    if (optional.isPresent()) {
                        return (ItemStack) asBukkitCopy.invoke(null, optional.get());
                    }
                }
                LogUtil.warning("[IItem] 合并NBT后创建物品失败");
                return null;
            }

            // ============ 旧版路径：setTag(NBTTagCompound) ============
            Method setTag = NMSUtil.setTag;
            Object nbttagCompound = parseSNBT.invoke(null, tag);
            Object nmsItem = asNMSCopy.invoke(null, itemStack);
            setTag.invoke(nmsItem, nbttagCompound);
            return (ItemStack) asBukkitCopy.invoke(null, nmsItem);

        } catch (Exception e) {
            LogUtil.error("[IItem] 设置物品NBT失败", e);
        }
        return null;
    }

    /**
     * 查找 CompoundTag/NBTTagCompound 的 merge 方法
     * <p>
     * 不同版本方法名不同：
     * <ul>
     *   <li>Mojang映射（1.20.5+）: merge(CompoundTag)</li>
     *   <li>Spigot映射（旧版）: a(NBTTagCompound)</li>
     * </ul>
     * </p>
     *
     * @param compoundTagClass CompoundTag 的运行时类
     * @return merge 方法，未找到返回 null
     */
    @Nullable
    static Method findMergeMethod(Class<?> compoundTagClass) {
        // 候选方法名列表
        String[] candidates = {"merge", "put", "a"};
        for (String name : candidates) {
            try {
                Method method = compoundTagClass.getDeclaredMethod(name, compoundTagClass);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException ignored) {
                // 继续尝试下一个候选名
            }
        }

        // 备选：尝试用父类/接口类型作为参数
        if (NMSUtil.nbttagCompoundClass != null && !compoundTagClass.equals(NMSUtil.nbttagCompoundClass)) {
            for (String name : candidates) {
                try {
                    Method method = compoundTagClass.getDeclaredMethod(name, NMSUtil.nbttagCompoundClass);
                    method.setAccessible(true);
                    return method;
                } catch (NoSuchMethodException ignored) {
                    // 继续尝试
                }
            }
        }

        return null;
    }
}