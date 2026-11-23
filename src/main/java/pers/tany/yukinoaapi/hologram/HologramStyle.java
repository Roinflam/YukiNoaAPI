package pers.tany.yukinoaapi.hologram;

import org.bukkit.configuration.ConfigurationSection;

import java.util.Locale;

/**
 * 全息图的显示设置：放大倍数、朝向、底色、阴影。
 * 只在 1.19.4+ 的文字展示模式下生效，盔甲架和 HolographicDisplays 模式忽略。
 * 不可变对象，改设置就换一个新的。
 *
 * @since 3.5.0
 */
public final class HologramStyle {

    public static final String BILLBOARD_CENTER = "center";
    public static final String BILLBOARD_VERTICAL = "vertical";
    public static final String BILLBOARD_HORIZONTAL = "horizontal";
    public static final String BILLBOARD_FIXED = "fixed";

    public static final String BACKGROUND_DEFAULT = "default";
    public static final String BACKGROUND_NONE = "none";

    public static final double MIN_SCALE = 0.1;
    public static final double MAX_SCALE = 64;

    public static final HologramStyle DEFAULT = new HologramStyle(1.0, BILLBOARD_CENTER, 0f, BACKGROUND_DEFAULT, false);

    private final double scale;
    private final String billboard;
    private final float yaw;
    private final String background;
    private final boolean shadow;

    /**
     * @param scale      放大倍数，超出 0.1～64 的会被夹到范围内，非正数按 1
     * @param billboard  center / vertical / horizontal / fixed，认不出的按 center
     * @param yaw        fixed 时文字正面朝向，和实体朝向同一套角度：0 南、90 西、180 北、270 东
     * @param background default / none / #RRGGBB / #AARRGGBB，认不出的按 default
     * @param shadow     文字阴影
     */
    public HologramStyle(double scale, String billboard, float yaw, String background, boolean shadow) {
        this.scale = normalizeScale(scale);
        this.billboard = normalizeBillboard(billboard);
        this.yaw = normalizeYaw(yaw);
        this.background = normalizeBackground(background);
        this.shadow = shadow;
    }

    public double getScale() { return scale; }
    public String getBillboard() { return billboard; }
    public float getYaw() { return yaw; }
    public String getBackground() { return background; }
    public boolean isShadow() { return shadow; }

    public boolean isDefault() {
        return equals(DEFAULT);
    }

    public HologramStyle withScale(double scale) {
        return new HologramStyle(scale, billboard, yaw, background, shadow);
    }

    public HologramStyle withBillboard(String billboard, float yaw) {
        return new HologramStyle(scale, billboard, yaw, background, shadow);
    }

    public HologramStyle withBackground(String background) {
        return new HologramStyle(scale, billboard, yaw, background, shadow);
    }

    public HologramStyle withShadow(boolean shadow) {
        return new HologramStyle(scale, billboard, yaw, background, shadow);
    }

    /**
     * 背景色的 ARGB 值；default 返回 null（用游戏默认的半透明底），none 返回 0。
     */
    public Integer backgroundArgb() {
        if (BACKGROUND_DEFAULT.equals(background)) return null;
        if (BACKGROUND_NONE.equals(background)) return 0;
        String hex = background.substring(1);
        long v = Long.parseLong(hex, 16);
        // #RRGGBB 当作不透明
        if (hex.length() == 6) v |= 0xFF000000L;
        return (int) v;
    }

    /** 没写的键取默认值，旧条目读出来就是 DEFAULT */
    public static HologramStyle read(ConfigurationSection section) {
        if (section == null) return DEFAULT;
        if (!section.contains("scale") && !section.contains("billboard") && !section.contains("yaw")
                && !section.contains("background") && !section.contains("shadow")) {
            return DEFAULT;
        }
        return new HologramStyle(
                section.getDouble("scale", 1.0),
                section.getString("billboard", BILLBOARD_CENTER),
                (float) section.getDouble("yaw", 0),
                section.getString("background", BACKGROUND_DEFAULT),
                section.getBoolean("shadow", false));
    }

    /** 只写和默认不同的键，没设过显示设置的条目写出来和以前一样 */
    public void write(ConfigurationSection section) {
        if (scale != DEFAULT.scale) section.set("scale", scale);
        if (!billboard.equals(DEFAULT.billboard)) section.set("billboard", billboard);
        if (yaw != DEFAULT.yaw) section.set("yaw", (double) yaw);
        if (!background.equals(DEFAULT.background)) section.set("background", background);
        if (shadow != DEFAULT.shadow) section.set("shadow", shadow);
    }

    private static double normalizeScale(double scale) {
        if (Double.isNaN(scale) || scale <= 0) return 1.0;
        return Math.max(MIN_SCALE, Math.min(MAX_SCALE, scale));
    }

    private static String normalizeBillboard(String billboard) {
        if (billboard == null) return BILLBOARD_CENTER;
        String b = billboard.trim().toLowerCase(Locale.ROOT);
        switch (b) {
            case BILLBOARD_VERTICAL:
            case BILLBOARD_HORIZONTAL:
            case BILLBOARD_FIXED:
                return b;
            default:
                return BILLBOARD_CENTER;
        }
    }

    private static float normalizeYaw(float yaw) {
        if (Float.isNaN(yaw) || Float.isInfinite(yaw)) return 0f;
        float y = yaw % 360f;
        if (y < 0) y += 360f;
        return y;
    }

    private static String normalizeBackground(String background) {
        if (background == null) return BACKGROUND_DEFAULT;
        String b = background.trim().toLowerCase(Locale.ROOT);
        if (BACKGROUND_NONE.equals(b)) return BACKGROUND_NONE;
        if (b.matches("#[0-9a-f]{6}") || b.matches("#[0-9a-f]{8}")) return b.toUpperCase(Locale.ROOT);
        return BACKGROUND_DEFAULT;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HologramStyle)) return false;
        HologramStyle s = (HologramStyle) o;
        return Double.compare(scale, s.scale) == 0 && Float.compare(yaw, s.yaw) == 0 && shadow == s.shadow
                && billboard.equals(s.billboard) && background.equals(s.background);
    }

    @Override
    public int hashCode() {
        int h = Double.hashCode(scale);
        h = 31 * h + billboard.hashCode();
        h = 31 * h + Float.hashCode(yaw);
        h = 31 * h + background.hashCode();
        return 31 * h + (shadow ? 1 : 0);
    }

    @Override
    public String toString() {
        return "scale=" + scale + ", billboard=" + billboard + ", yaw=" + yaw
                + ", background=" + background + ", shadow=" + shadow;
    }
}
