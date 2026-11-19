package pers.tany.yukinoaapi.hologram.impl;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

import java.lang.reflect.Method;

/**
 * 1.19.4+ 的文字展示实体（TextDisplay）。编译依赖是 1.17.1 API，这里全走反射。
 * 一个全息图只用一个实体显示全部行；它不 tick、没有碰撞箱，比一行一个盔甲架省得多。
 */
final class TextDisplaySupport {

    private static final Class<?> TEXT_DISPLAY;
    private static final Method SET_TEXT;
    private static final Method SET_LINE_WIDTH;
    private static final Method SET_BILLBOARD;
    private static final Object BILLBOARD_CENTER;
    private static final EntityType TEXT_DISPLAY_TYPE;

    static {
        Class<?> cls = null;
        Method setText = null, setLineWidth = null, setBillboard = null;
        Object center = null;
        EntityType type = null;
        try {
            cls = Class.forName("org.bukkit.entity.TextDisplay");
            Class<?> display = Class.forName("org.bukkit.entity.Display");
            Class<?> billboard = Class.forName("org.bukkit.entity.Display$Billboard");
            setText = cls.getMethod("setText", String.class);
            setLineWidth = cls.getMethod("setLineWidth", int.class);
            setBillboard = display.getMethod("setBillboard", billboard);
            for (Object c : billboard.getEnumConstants()) {
                if ("CENTER".equals(((Enum<?>) c).name())) center = c;
            }
            type = EntityType.valueOf("TEXT_DISPLAY");
            if (center == null) cls = null;
        } catch (Throwable ignored) {
            cls = null;
        }
        TEXT_DISPLAY = cls;
        SET_TEXT = setText;
        SET_LINE_WIDTH = setLineWidth;
        SET_BILLBOARD = setBillboard;
        BILLBOARD_CENTER = center;
        TEXT_DISPLAY_TYPE = type;
    }

    private TextDisplaySupport() {
    }

    static boolean isAvailable() {
        return TEXT_DISPLAY != null;
    }

    static boolean isTextDisplay(Entity entity) {
        return TEXT_DISPLAY != null && TEXT_DISPLAY.isInstance(entity);
    }

    /**
     * 生成并设置好属性；带回调的 spawn 让实体加进世界前就有文字，客户端不会先看到一个空框。
     */
    @SuppressWarnings("unchecked")
    static Entity spawn(Location location, String text, String... tags) {
        World world = location.getWorld();
        if (world == null || TEXT_DISPLAY == null) return null;

        Class<Entity> cls = (Class<Entity>) TEXT_DISPLAY;
        org.bukkit.util.Consumer<Entity> callback = e -> setup(e, text, tags);
        try {
            return world.spawn(location, cls, callback);
        } catch (NoSuchMethodError e) {
            // 新版 API 删掉了 org.bukkit.util.Consumer 那个重载
            Entity entity = world.spawnEntity(location, TEXT_DISPLAY_TYPE);
            setup(entity, text, tags);
            return entity;
        }
    }

    static void setText(Entity entity, String text) {
        try {
            SET_TEXT.invoke(entity, text);
        } catch (Exception ignored) {
        }
    }

    private static void setup(Entity entity, String text, String... tags) {
        try {
            SET_TEXT.invoke(entity, text);
            // 默认 200 像素就折行，商品名长一点会被折成两行
            SET_LINE_WIDTH.invoke(entity, 4096);
            SET_BILLBOARD.invoke(entity, BILLBOARD_CENTER);
        } catch (Exception ignored) {
        }
        // 不存盘：区块卸载或崩服后不会留下孤儿，下次加载由全息图重新生成
        entity.setPersistent(false);
        for (String tag : tags) entity.addScoreboardTag(tag);
    }
}
