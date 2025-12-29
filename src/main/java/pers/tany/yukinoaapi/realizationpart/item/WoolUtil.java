package pers.tany.yukinoaapi.realizationpart.item;


import pers.tany.yukinoaapi.interfacepart.builder.IItemBuilder;
import pers.tany.yukinoaapi.realizationpart.builder.ItemBuilder;

import javax.annotation.Nonnull;
import java.util.ArrayList;

/**
 * 兼容高版本的彩色羊毛库
 */
public class WoolUtil {
    private static final ArrayList<IItemBuilder> wool = new ArrayList<IItemBuilder>();

    static {
        try {
            for (int i = 0; i < 16; i++) {
                wool.add(new ItemBuilder("WOOL", 1, (short) i));
            }
        } catch (Exception exception) {
            wool.add(new ItemBuilder("WHITE_WOOL"));
            wool.add(new ItemBuilder("ORANGE_WOOL"));
            wool.add(new ItemBuilder("MAGENTA_WOOL"));
            wool.add(new ItemBuilder("LIGHT_BLUE_WOOL"));
            wool.add(new ItemBuilder("YELLOW_WOOL"));
            wool.add(new ItemBuilder("LIME_WOOL"));

            wool.add(new ItemBuilder("PINK_WOOL"));
            wool.add(new ItemBuilder("GRAY_WOOL"));
            wool.add(new ItemBuilder("LIGHT_GRAY_WOOL"));
            wool.add(new ItemBuilder("CYAN_WOOL"));
            wool.add(new ItemBuilder("PURPLE_WOOL"));
            wool.add(new ItemBuilder("BLUE_WOOL"));

            wool.add(new ItemBuilder("BROWN_WOOL"));
            wool.add(new ItemBuilder("GREEN_WOOL"));
            wool.add(new ItemBuilder("RED_WOOL"));
            wool.add(new ItemBuilder("BLACK_WOOL"));
        }
    }

    /**
     * 获取羊毛物品
     *
     * @param damage 羊毛的子ID
     * @return 羊毛物品
     */
    
    public static IItemBuilder getWool(int damage) {
        return new ItemBuilder(wool.get(damage).getItemStack());
    }
}
