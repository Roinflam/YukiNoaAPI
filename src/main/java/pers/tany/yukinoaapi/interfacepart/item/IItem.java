package pers.tany.yukinoaapi.interfacepart.item;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
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
 * </p>
 * <p>
 * <b>版本分流策略（关键）：</b>
 * 物品序列化方法在不同 Minecraft 时代差异巨大：
 * <ul>
 *   <li>1.20.4 及以下（NBTTagCompound 时代）：存在 NMS {@code ItemStack.save(NBTTagCompound)} 方法，
 *       走传统反射路径（{@link NMSUtil#save_ItemStack} 等），逻辑完全保留不变。</li>
 *   <li>1.20.5 及以上（DataComponents / Codec 时代）：原版已彻底移除 {@code save(NBTTagCompound)}，
 *       1.20.5 初期改为 {@code save(Provider)}，1.21.x 又进一步迁移到 Codec 体系（CODEC/MAP_CODEC 字段），
 *       NMS 反射既不稳定也难以追随版本。此时优先委托 NBT-API 处理，由其内部自适配各版本序列化机制；
 *       服务端没装 NBT-API 时改用服务端自己的 save(Provider) / parse(Provider, Tag)（见 {@code ModernItemNbt}），
 *       产出格式相同。</li>
 * </ul>
 * <b>分流判据：</b>{@code NMSUtil.save_ItemStack == null}。
 * 旧版该方法必定能反射到（不为 null），新版必定为 null，以此精确区分两个时代，
 * 无需依赖小版本号解析，且不修改 NMSUtil。
 * </p>
 * <p>
 * 本接口不直接引用 NBT-API 的类：接口被加载时 JVM 会校验全部方法，引用到不存在的类会让整个接口不可用。
 * </p>
 */
public interface IItem {

    /**
     * 判断当前是否应使用 NBT-API 进行物品序列化（即处于 DataComponents / Codec 时代）
     * <p>
     * 判据：旧版 NMS {@code ItemStack.save(NBTTagCompound)} 方法不存在。
     * 旧版（1.20.4-）该方法必定能反射到；新版（1.20.5+）原版已移除该方法，反射结果为 null。
     * </p>
     *
     * @return true 表示走 NBT-API（新版），false 表示走传统 NMS 反射（旧版）
     */
    static boolean useNbtApiForItem() {
        return NMSUtil.save_ItemStack == null;
    }

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
        // getItemMeta() 每次都会新建一份 meta，取一次复用（内容相同）
        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta != null && itemMeta.hasDisplayName()) {
            return itemMeta.getDisplayName();
        }
        try {
            Method asNMSCopy = NMSUtil.asNMSCopy;
            Object nmsItem = asNMSCopy.invoke(null, itemStack);
            String name = null;
            boolean resolved = false;
            // 方案1: Item.getName(ItemStack) - 旧版本
            Method getItem = NMSUtil.getItem;
            Method getName_Item = NMSUtil.getName_Item;
            Method getString = NMSUtil.getString_IChatBaseComponent;
            // 反射到的方法从类型上就接不起来时（如 1.20.1 的 "h" 撞到 getTooltipImage），
            // 方案1 必然抛异常落到方案2，这里直接走方案2，省掉每次构造异常
            if (ItemNameReflection.nameByItemUsable(getItem, getName_Item, getString)) {
                try {
                    Object item = getItem.invoke(nmsItem);
                    name = getString.invoke(getName_Item.invoke(item, nmsItem)).toString();
                    resolved = true;
                } catch (Exception e) {
                    // 落到方案2
                }
            }
            if (!resolved) {
                // 方案2: ItemStack.getName() / getHoverName()
                Method getNameMethod = NMSUtil.getName_ItemStack;
                Object result = getNameMethod.invoke(nmsItem);

                // 判断返回类型：1.13+返回IChatBaseComponent，1.12-返回String
                if (result instanceof String) {
                    // 低版本直接返回String
                    name = (String) result;
                } else {
                    // 高版本返回Component，需要调用getString()
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
     * 1.20.4及以下：使用 NMS ItemStack.save(NBTTagCompound) 方式（传统反射，逻辑保留不变）<br>
     * 1.20.5及以上：序列化为 SNBT 字符串（{id, count, components}），有 NBT-API 用它，没有用服务端内置接口
     * </p>
     *
     * @param itemStack 涉及物品
     * @return 物品的完整NBT字符串（SNBT格式），失败返回空字符串
     */
    @Nullable
    static String getItemNBT(ItemStack itemStack) {
        // ============ 新版路径（1.20.5+）：NBT-API 或服务端内置接口 ============
        if (useNbtApiForItem()) {
            try {
                return ModernItemNbt.toSnbt(itemStack);
            } catch (Exception e) {
                LogUtil.error("[IItem] 序列化物品失败", e);
                return "";
            }
        }

        // ============ 旧版路径（1.20.4-）：传统 NMS 反射，逻辑保留不变 ============
        try {
            Method asNMSCopy = NMSUtil.asNMSCopy;
            Object nmsItem = asNMSCopy.invoke(null, itemStack);

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
     * 1.20.4及以下：解析NBT → 加载到虚拟物品 → 转为Bukkit物品（传统反射，逻辑保留不变）<br>
     * 1.20.5及以上：从 SNBT 反序列化，有 NBT-API 用它，没有用服务端内置接口
     * </p>
     *
     * @param tag 物品完整NBT字符串（SNBT格式）
     * @return 反序列化后的物品，失败返回 null
     */
    @Nullable
    static ItemStack getItemStack(String tag) {
        // ============ 新版路径（1.20.5+）：NBT-API 或服务端内置接口 ============
        if (useNbtApiForItem()) {
            try {
                ItemStack item = ModernItemNbt.fromSnbt(tag);
                if (item == null) {
                    LogUtil.warning("[IItem] 从NBT解析物品返回空，NBT可能格式不正确: "
                            + tag.substring(0, Math.min(tag.length(), 100)));
                }
                return item;
            } catch (Exception e) {
                LogUtil.error("[IItem] 从NBT字符串创建物品失败", e);
                return null;
            }
        }

        // ============ 旧版路径（1.20.4-）：传统 NMS 反射，逻辑保留不变 ============
        try {
            final Method parseSNBT = NMSUtil.parse;
            final Method asBukkitCopy = NMSUtil.asBukkitCopy;

            // 先将SNBT字符串解析为NBT对象（NBTTagCompound）
            final Object nbttagCompound = parseSNBT.invoke(null, tag);

            final Method asNMSCopy = NMSUtil.asNMSCopy;
            final Method load = NMSUtil.load_ItemStack;
            // 创建一个虚拟物品作为载体
            final Object nmsItem = asNMSCopy.invoke(null, new ItemBuilder("STONE").getItemStack());
            // 将解析的NBT加载到虚拟物品上
            load.invoke(nmsItem, nbttagCompound);
            ItemStack loaded = (ItemStack) asBukkitCopy.invoke(null, nmsItem);
            // Mohist 1.12.2 的 load 只读 tag 不换物品，占位的石头会留下来，改用 NBT 构造函数再读一次
            if (loaded != null && "STONE".equals(loaded.getType().name())) {
                try {
                    java.lang.reflect.Constructor<?> ctor = NMSUtil.nmsItemStackClass.getDeclaredConstructor(NMSUtil.nbttagCompoundClass);
                    ctor.setAccessible(true);
                    ItemStack byCtor = (ItemStack) asBukkitCopy.invoke(null, ctor.newInstance(nbttagCompound));
                    if (byCtor != null && !"STONE".equals(byCtor.getType().name()) && !"AIR".equals(byCtor.getType().name())) {
                        return byCtor;
                    }
                } catch (Exception ignored) {
                    // 没有这个构造函数的版本，load 本来就是对的
                }
            }
            return loaded;

        } catch (Exception e) {
            LogUtil.error("[IItem] 从NBT字符串创建物品失败", e);
            return null;
        }
    }

    /**
     * 设置物品的自定义NBT标签
     * <p>
     * 1.20.4及以下：解析tag → setTag(nmsItem, compound) → 转为Bukkit物品（传统反射，逻辑保留不变）<br>
     * 1.20.5及以上：将传入的 NBT 合并到物品的自定义数据（minecraft:custom_data）上，有 NBT-API 用它，没有用服务端内置接口
     * </p>
     *
     * @param itemStack 涉及物品
     * @param tag       物品NBT字符串
     * @return 设置NBT后的物品，失败返回 null
     */
    @Nullable
    static ItemStack getItemStack(ItemStack itemStack, String tag) {
        // ============ 新版路径（1.20.5+）：NBT-API 或服务端内置接口 ============
        if (useNbtApiForItem()) {
            try {
                // 合并进物品的自定义数据（minecraft:custom_data），不修改入参原物品
                return ModernItemNbt.merge(itemStack, tag);
            } catch (Exception e) {
                LogUtil.error("[IItem] 设置物品NBT失败", e);
                return null;
            }
        }

        // ============ 旧版路径（1.20.4-）：setTag(NBTTagCompound)，逻辑保留不变 ============
        try {
            Method asNMSCopy = NMSUtil.asNMSCopy;
            Method parseSNBT = NMSUtil.parse;
            Method asBukkitCopy = NMSUtil.asBukkitCopy;

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
}

