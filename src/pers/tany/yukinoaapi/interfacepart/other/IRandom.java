package pers.tany.yukinoaapi.interfacepart.other;


import javax.annotation.Nonnull;
import java.util.Random;

/**
 * 随机数与概率
 */
public interface IRandom {
    /**
     * 创建一个长度的随机字符串
     * 范围：0-9,a-z,A-Z
     *
     * @param length 字符串的长度
     * @return 随机字符串
     */
    
    static String createRandomString(int length) {
        String number = "";
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            String str = random.nextInt(2) % 2 == 0 ? "num" : "char";
            if ("char".equalsIgnoreCase(str)) {
                int nextInt = random.nextInt(2) % 2 == 0 ? 65 : 97;
                number += (char) (nextInt + random.nextInt(26));
            } else {
                number += String.valueOf(random.nextInt(10));
            }
        }
        return number;
    }

    /**
     * 创建这个范围的随机数
     *
     * @param min 最小数值
     * @param max 最大数值
     * @return 随机数
     */
    static int randomNumber(int min, int max) {
        return new Random().nextInt(max - min + 1) + min;
    }

    /**
     * 返回百分比几率的结果
     *
     * @param probability 百分比几率
     * @return 结果
     */
    static boolean percentageChance(int probability) {
        return percentageChance(probability, 8);
    }

    /**
     * 返回百分比几率的结果
     *
     * @param probability 百分比几率
     * @return 结果
     */
    static boolean percentageChance(double probability) {
        return percentageChance(probability, 8);
    }

    /**
     * 返回百分比几率的结果
     *
     * @param probability 百分比几率
     * @return 结果
     */
    static boolean percentageChance(String probability) {
        return percentageChance(IDouble.percentageNumber(probability, false));
    }

    /**
     * 返回...分比几率的结果
     *
     * @param probability 几率
     * @param range       提高多少倍，如果为1则百分比，2为千分比，3为万分比
     * @return 结果
     */
    static boolean percentageChance(double probability, int range) {
        return randomNumber(0, 100 * (int) Math.pow(10, range - 1)) < (int) (probability * (int) Math.pow(10, range - 1));
    }
}
