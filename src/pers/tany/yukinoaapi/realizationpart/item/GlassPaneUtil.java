package pers.tany.yukinoaapi.realizationpart.item;


import pers.tany.yukinoaapi.interfacepart.builder.IItemBuilder;
import pers.tany.yukinoaapi.realizationpart.builder.ItemBuilder;

import javax.annotation.Nonnull;
import java.util.ArrayList;

/**
 * 兼容高版本的彩色玻璃库
 */
public class GlassPaneUtil {
    private static final ArrayList<IItemBuilder> stainedGlass = new ArrayList<IItemBuilder>();

    static {
        try {
            for (int i = 0; i < 16; i++) {
                stainedGlass.add(new ItemBuilder("STAINED_GLASS_PANE", 1, (short) i));
            }
        } catch (Exception exception) {
            stainedGlass.add(new ItemBuilder("WHITE_STAINED_GLASS_PANE"));
            stainedGlass.add(new ItemBuilder("ORANGE_STAINED_GLASS_PANE"));
            stainedGlass.add(new ItemBuilder("MAGENTA_STAINED_GLASS_PANE"));
            stainedGlass.add(new ItemBuilder("LIGHT_BLUE_STAINED_GLASS_PANE"));
            stainedGlass.add(new ItemBuilder("YELLOW_STAINED_GLASS_PANE"));
            stainedGlass.add(new ItemBuilder("LIME_STAINED_GLASS_PANE"));

            stainedGlass.add(new ItemBuilder("PINK_STAINED_GLASS_PANE"));
            stainedGlass.add(new ItemBuilder("GRAY_STAINED_GLASS_PANE"));
            stainedGlass.add(new ItemBuilder("LIGHT_GRAY_STAINED_GLASS_PANE"));
            stainedGlass.add(new ItemBuilder("CYAN_STAINED_GLASS_PANE"));
            stainedGlass.add(new ItemBuilder("PURPLE_STAINED_GLASS_PANE"));
            stainedGlass.add(new ItemBuilder("BLUE_STAINED_GLASS_PANE"));

            stainedGlass.add(new ItemBuilder("BROWN_STAINED_GLASS_PANE"));
            stainedGlass.add(new ItemBuilder("GREEN_STAINED_GLASS_PANE"));
            stainedGlass.add(new ItemBuilder("RED_STAINED_GLASS_PANE"));
            stainedGlass.add(new ItemBuilder("BLACK_STAINED_GLASS_PANE"));
        }
    }

    /**
     * 获取玻璃板物品
     *
     * @param damage 玻璃板的子ID
     * @return 玻璃板物品
     */
    
    public static IItemBuilder getStainedGlass(int damage) {
        return new ItemBuilder(stainedGlass.get(damage).getItemStack());
    }
}
