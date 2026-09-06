package pers.tany.yukinoaapi.interfacepart.other;


import javax.annotation.Nonnull;
import java.math.BigDecimal;
import java.text.DecimalFormat;

/**
 * 小数相关
 */
public final class IDouble {

    /**
     * 工具类,禁止实例化
     *
     * @throws UnsupportedOperationException 总是抛出
     */
    private IDouble() {
        throw new UnsupportedOperationException("IDouble 是工具类,不允许实例化");
    }

    /**
     * 保留double小数点后的位数
     *
     * @param number 涉及的double
     * @param lenght 保留几位
     * @return 修改后的数字
     */
    public static double shortenDouble(double number, int lenght) {
        String format = "#.";
        for (int i = 0; i < lenght; i++) {
            format += "#";
        }
        DecimalFormat decimalFormat = new DecimalFormat(format);
        return Double.parseDouble(decimalFormat.format(number));
    }


    /**
     * 把百分比字符串转化为小数形式
     * 如：10%则为0.1
     *
     * @param percentage 字符串形式的百分比几率
     * @return 百分比的小数形式
     */
    public static double percentageNumber(String percentage) {
        return percentageNumber(percentage, true);
    }

    /**
     * 把百分比字符串转化为小数形式
     * 如：10%则为0.1或10
     *
     * @param percentage 字符串形式的百分比几率
     * @param except     如果为true，则会/100
     * @return 百分比的小数形式
     */
    public static double percentageNumber(String percentage, boolean except) {
        return except ? BigDecimal.valueOf(Double.parseDouble(percentage.replace("%", ""))).divide(new BigDecimal(100)).doubleValue() : Double.parseDouble(percentage.replace("%", ""));
    }
}


