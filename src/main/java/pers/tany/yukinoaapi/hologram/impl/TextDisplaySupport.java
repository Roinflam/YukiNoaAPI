package pers.tany.yukinoaapi.hologram.impl;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import pers.tany.yukinoaapi.hologram.HologramStyle;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 1.19.4+ 的文字展示实体（TextDisplay）。编译依赖是 1.17.1 API，这里全走反射。
 * 一个全息图只用一个实体显示全部行；它不 tick、没有碰撞箱，比一行一个盔甲架省得多。
 */
final class TextDisplaySupport {

    private static final Class<?> TEXT_DISPLAY;
    private static final Method SET_TEXT;
    private static final Method SET_LINE_WIDTH;
    private static final Method SET_BILLBOARD;
    private static final Map<String, Object> BILLBOARDS = new HashMap<>();
    private static final EntityType TEXT_DISPLAY_TYPE;

    // 显示设置用到的，拿不到就只是这一项不生效
    private static final Method SET_TRANSFORMATION;
    private static final Constructor<?> TRANSFORMATION_CTOR;
    private static final Constructor<?> VECTOR3F_CTOR;
    private static final Constructor<?> AXIS_ANGLE_CTOR;
    private static final Method SET_BACKGROUND_COLOR;
    private static final Method SET_DEFAULT_BACKGROUND;
    private static final Method COLOR_FROM_ARGB;
    private static final Method SET_SHADOWED;

    static {
        Class<?> cls = null;
        Method setText = null, setLineWidth = null, setBillboard = null;
        EntityType type = null;
        try {
            cls = Class.forName("org.bukkit.entity.TextDisplay");
            Class<?> display = Class.forName("org.bukkit.entity.Display");
            Class<?> billboard = Class.forName("org.bukkit.entity.Display$Billboard");
            setText = cls.getMethod("setText", String.class);
            setLineWidth = cls.getMethod("setLineWidth", int.class);
            setBillboard = display.getMethod("setBillboard", billboard);
            for (Object c : billboard.getEnumConstants()) {
                BILLBOARDS.put(((Enum<?>) c).name().toLowerCase(Locale.ROOT), c);
            }
            type = EntityType.valueOf("TEXT_DISPLAY");
            if (!BILLBOARDS.containsKey(HologramStyle.BILLBOARD_CENTER)) cls = null;
        } catch (Throwable ignored) {
            cls = null;
        }
        TEXT_DISPLAY = cls;
        SET_TEXT = setText;
        SET_LINE_WIDTH = setLineWidth;
        SET_BILLBOARD = setBillboard;
        TEXT_DISPLAY_TYPE = type;

        Method setTransformation = null;
        Constructor<?> transformationCtor = null, vectorCtor = null, axisCtor = null;
        try {
            Class<?> display = Class.forName("org.bukkit.entity.Display");
            Class<?> transformation = Class.forName("org.bukkit.util.Transformation");
            setTransformation = display.getMethod("setTransformation", transformation);
            // JOML 的类从 Transformation 的构造函数上取，不自己 Class.forName，混合端上插件类加载器不一定看得到
            for (Constructor<?> c : transformation.getConstructors()) {
                Class<?>[] p = c.getParameterTypes();
                if (p.length == 4 && p[1].getSimpleName().equals("AxisAngle4f")) {
                    transformationCtor = c;
                    vectorCtor = p[0].getConstructor(float.class, float.class, float.class);
                    axisCtor = p[1].getConstructor();
                }
            }
            if (transformationCtor == null) setTransformation = null;
        } catch (Throwable ignored) {
            setTransformation = null;
        }
        SET_TRANSFORMATION = setTransformation;
        TRANSFORMATION_CTOR = transformationCtor;
        VECTOR3F_CTOR = vectorCtor;
        AXIS_ANGLE_CTOR = axisCtor;

        Method setBg = null, setDefaultBg = null, fromArgb = null, setShadowed = null;
        if (cls != null) {
            try {
                Class<?> color = Class.forName("org.bukkit.Color");
                setBg = cls.getMethod("setBackgroundColor", color);
                setDefaultBg = cls.getMethod("setDefaultBackground", boolean.class);
                fromArgb = color.getMethod("fromARGB", int.class);
            } catch (Throwable ignored) {
                setBg = null;
            }
            try {
                setShadowed = cls.getMethod("setShadowed", boolean.class);
            } catch (Throwable ignored) {
            }
        }
        SET_BACKGROUND_COLOR = setBg;
        SET_DEFAULT_BACKGROUND = setDefaultBg;
        COLOR_FROM_ARGB = fromArgb;
        SET_SHADOWED = setShadowed;
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
     * 生成并设置好属性；带回调的 spawn 让实体加进世界前就有文字和缩放，客户端不会先看到一个空框或小字。
     */
    @SuppressWarnings("unchecked")
    static Entity spawn(Location location, String text, HologramStyle style, String... tags) {
        World world = location.getWorld();
        if (world == null || TEXT_DISPLAY == null) return null;

        Class<Entity> cls = (Class<Entity>) TEXT_DISPLAY;
        org.bukkit.util.Consumer<Entity> callback = e -> {
            setup(e, text, style, tags);
            e.setRotation(location.getYaw(), location.getPitch());
        };
        Entity entity;
        try {
            entity = world.spawn(location, cls, callback);
        } catch (NoSuchMethodError e) {
            // 新版 API 删掉了 org.bukkit.util.Consumer 那个重载
            entity = world.spawnEntity(location, TEXT_DISPLAY_TYPE);
            setup(entity, text, style, tags);
        }
        // 混合端上 spawn 不带生成位置里的朝向，fixed 的字会全朝南，生成后再设一次
        if (entity != null) entity.setRotation(location.getYaw(), location.getPitch());
        return entity;
    }

    static void teleport(Entity entity, Location location) {
        entity.teleport(location);
        entity.setRotation(location.getYaw(), location.getPitch());
    }

    static void setText(Entity entity, String text) {
        try {
            SET_TEXT.invoke(entity, text);
        } catch (Exception ignored) {
        }
    }

    private static void setup(Entity entity, String text, HologramStyle style, String... tags) {
        try {
            SET_TEXT.invoke(entity, text);
            // 默认 200 像素就折行，商品名长一点会被折成两行
            SET_LINE_WIDTH.invoke(entity, 4096);
        } catch (Exception ignored) {
        }
        applyStyle(entity, style);
        // 不存盘：区块卸载或崩服后不会留下孤儿，下次加载由全息图重新生成
        entity.setPersistent(false);
        for (String tag : tags) entity.addScoreboardTag(tag);
    }

    /**
     * 只给新生成的实体用（改设置时整个重新生成）。朝向角度不在这里设，由调用方放进生成位置里。
     * 默认设置只设朝向方式，和加显示设置之前生成的实体完全一样。
     */
    static void applyStyle(Entity entity, HologramStyle style) {
        if (style == null) style = HologramStyle.DEFAULT;
        try {
            Object billboard = BILLBOARDS.get(style.getBillboard());
            SET_BILLBOARD.invoke(entity, billboard != null ? billboard : BILLBOARDS.get(HologramStyle.BILLBOARD_CENTER));
        } catch (Exception ignored) {
        }
        if (style.isDefault()) return;

        if (SET_TRANSFORMATION != null) {
            try {
                float s = (float) style.getScale();
                Object transformation = TRANSFORMATION_CTOR.newInstance(
                        VECTOR3F_CTOR.newInstance(0f, 0f, 0f),
                        AXIS_ANGLE_CTOR.newInstance(),
                        VECTOR3F_CTOR.newInstance(s, s, s),
                        AXIS_ANGLE_CTOR.newInstance());
                SET_TRANSFORMATION.invoke(entity, transformation);
            } catch (Exception ignored) {
            }
        }

        if (SET_BACKGROUND_COLOR != null) {
            try {
                // default 不用设，新生成的实体本来就是游戏默认的半透明底
                Integer argb = style.backgroundArgb();
                if (argb != null) {
                    SET_DEFAULT_BACKGROUND.invoke(entity, false);
                    SET_BACKGROUND_COLOR.invoke(entity, COLOR_FROM_ARGB.invoke(null, argb));
                }
            } catch (Exception ignored) {
            }
        }

        if (SET_SHADOWED != null) {
            try {
                SET_SHADOWED.invoke(entity, style.isShadow());
            } catch (Exception ignored) {
            }
        }
    }
}
