package pers.tany.yukinoaapi.interfacepart.enchantment;

import org.bukkit.enchantments.Enchantment;
import pers.tany.yukinoaapi.util.NMSUtil;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * 附魔操作相关
 */
public interface IEnchantment {

    /**
     * 获取附魔的名称
     *
     * @param enchantment 涉及的附魔
     * @return 翻译后的名称，如果没有翻译则为Material枚举名
     */
    @Nullable
    static String getName(Enchantment enchantment) {
        Method getName = NMSUtil.getName_Enchantment;
        String name;
        try {
            Method getHandle = NMSUtil.getHandle_Enchantment;
            Object e = getHandle.invoke(enchantment);
            try {
                Method getString = NMSUtil.getString_IChatBaseComponent;
                name = getString.invoke(getName.invoke(e, 0)).toString().split(" enchantment.level.")[0];
            } catch (Exception exception) {
                name = getName.invoke(e, 0).toString().split(" enchantment.level.")[0];
            }
        } catch (Exception exception) {
            try {
                Field target = NMSUtil.target;
                target.setAccessible(true);
                Object e = target.get(enchantment);
                name = getName.invoke(e, 0).toString().split(" enchantment.level.")[0];
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        return name.split("\\.").length > 2 ? enchantment.getName() : name.replace(" ", "");
    }


    /**
     * 根据翻译名称获取附魔
     *
     * @param name 附魔名称
     * @return 附魔
     */
    @Nullable
    static Enchantment getEnchantment(String name) {
        for (Enchantment enchantment : Enchantment.values()) {
            if (getName(enchantment).equalsIgnoreCase(name)) {
                return enchantment;
            }
        }
        return null;
    }
}
