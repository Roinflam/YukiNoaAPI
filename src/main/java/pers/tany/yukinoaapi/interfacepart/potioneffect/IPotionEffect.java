package pers.tany.yukinoaapi.interfacepart.potioneffect;

import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import javax.annotation.Nonnull;


/**
 * buff相关
 */
public final class IPotionEffect {

    /**
     * 工具类,禁止实例化
     *
     * @throws UnsupportedOperationException 总是抛出
     */
    private IPotionEffect() {
        throw new UnsupportedOperationException("IPotionEffect 是工具类,不允许实例化");
    }


    /**
     * 根据类型、等级、刻获取buff
     *
     * @param potionEffectType buff类型
     * @param tick             时间（单位tick）
     * @param level            buff的等级
     * @return buff对象
     */
    
    public static PotionEffect getPotionEffect(PotionEffectType potionEffectType, int tick, int level) {
        return new PotionEffect(potionEffectType, tick, level);
    }

}


