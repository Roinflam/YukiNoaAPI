package pers.tany.yukinoaapi.interfacepart.other;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 随机数与概率工具接口
 * 提供随机字符串生成、随机数生成、概率判断等功能
 */
public final class IRandom {

    /**
     * 工具类,禁止实例化
     *
     * @throws UnsupportedOperationException 总是抛出
     */
    private IRandom() {
        throw new UnsupportedOperationException("IRandom 是工具类,不允许实例化");
    }


    /**
     * 创建指定长度的随机字符串
     * 范围：0-9, a-z, A-Z
     *
     * @param length 字符串的长度
     * @return 随机字符串
     */
    public static String createRandomString(int length) {
        if (length <= 0) {
            return "";
        }

        StringBuilder sb = new StringBuilder(length);
        ThreadLocalRandom random = ThreadLocalRandom.current();

        for (int i = 0; i < length; i++) {
            int type = random.nextInt(3);
            switch (type) {
                case 0:
                    // 数字 0-9
                    sb.append(random.nextInt(10));
                    break;
                case 1:
                    // 小写字母 a-z
                    sb.append((char) ('a' + random.nextInt(26)));
                    break;
                case 2:
                    // 大写字母 A-Z
                    sb.append((char) ('A' + random.nextInt(26)));
                    break;
            }
        }
        return sb.toString();
    }

    /**
     * 创建指定范围的随机整数（包含min和max）
     *
     * @param min 最小数值
     * @param max 最大数值
     * @return 随机数
     */
    public static int randomNumber(int min, int max) {
        if (min > max) {
            int temp = min;
            min = max;
            max = temp;
        }
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    /**
     * 创建指定范围的随机小数（包含min，不包含max）
     *
     * @param min 最小数值
     * @param max 最大数值
     * @return 随机数
     */
    public static double randomDouble(double min, double max) {
        if (min > max) {
            double temp = min;
            min = max;
            max = temp;
        }
        return ThreadLocalRandom.current().nextDouble(min, max);
    }

    /**
     * 返回百分比几率的结果
     * 默认精度为8位（亿分比）
     *
     * @param probability 百分比几率（如：50表示50%）
     * @return 是否命中
     */
    public static boolean percentageChance(int probability) {
        return percentageChance((double) probability, 8);
    }

    /**
     * 返回百分比几率的结果
     * 默认精度为8位（亿分比）
     *
     * @param probability 百分比几率（如：50.5表示50.5%）
     * @return 是否命中
     */
    public static boolean percentageChance(double probability) {
        return percentageChance(probability, 8);
    }

    /**
     * 返回百分比几率的结果
     * 支持字符串格式（如："50%"或"50.5%"）
     *
     * @param probability 百分比几率字符串
     * @return 是否命中
     */
    public static boolean percentageChance(String probability) {
        return percentageChance(IDouble.percentageNumber(probability, false));
    }

    /**
     * 返回指定精度的几率结果
     *
     * @param probability 几率（如：50表示50%）
     * @param precision   精度（1=百分比，2=千分比，3=万分比，以此类推）
     * @return 是否命中
     */
    public static boolean percentageChance(double probability, int precision) {
        if (probability <= 0) {
            return false;
        }
        if (probability >= 100) {
            return true;
        }

        // 计算最大随机值
        int maxValue = (int) Math.pow(10, precision);
        // 计算阈值
        int threshold = (int) (probability * Math.pow(10, precision - 2));
        // 生成随机数并判断
        return ThreadLocalRandom.current().nextInt(maxValue) < threshold;
    }

    /**
     * 从数组中随机获取一个元素
     *
     * @param array 数组
     * @param <T>   元素类型
     * @return 随机元素，数组为空返回null
     */
    @SuppressWarnings("unchecked")
    public static <T> T randomElement(T... array) {
        if (array == null || array.length == 0) {
            return null;
        }
        return array[ThreadLocalRandom.current().nextInt(array.length)];
    }

    /**
     * 从列表中随机获取一个元素
     *
     * @param list 列表
     * @param <T>  元素类型
     * @return 随机元素，列表为空返回null
     */
    public static <T> T randomElement(java.util.List<T> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list.get(ThreadLocalRandom.current().nextInt(list.size()));
    }
}

