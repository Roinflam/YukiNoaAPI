package pers.tany.yukinoaapi.interfacepart.other;


import javax.annotation.Nonnull;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * 时间相关
 */
public final class ITime {

    /**
     * 工具类,禁止实例化
     *
     * @throws UnsupportedOperationException 总是抛出
     */
    private ITime() {
        throw new UnsupportedOperationException("ITime 是工具类,不允许实例化");
    }

    /**
     * 获取这个月的天数
     *
     * @return 月的天数
     */
    public static int getMonthDay() {
        String timeGroup = new SimpleDateFormat("yyyy:MM").format(new Date());
        return getMonthDay(Integer.parseInt(timeGroup.split(":")[1]), Integer.parseInt(timeGroup.split(":")[0]));
    }

    /**
     * 获取今年这个月的天数
     *
     * @param nowMonth 月
     * @return 月的天数
     */
    public static int getMonthDay(int nowMonth) {
        String timeGroup = new SimpleDateFormat("yyyy:MM").format(new Date());
        return getMonthDay(nowMonth, Integer.parseInt(timeGroup.split(":")[0]));
    }

    /**
     * 获取某年某月的天数
     *
     * @param nowMonth 月
     * @param nowYear  年
     * @return 月的天数
     */
    public static int getMonthDay(int nowMonth, int nowYear) {
        if (nowMonth == 1 || nowMonth == 3 || nowMonth == 5 || nowMonth == 7 || nowMonth == 8 || nowMonth == 10 || nowMonth == 12) {
            return 31;
        } else if (nowMonth == 2) {
            return nowYear % 4 == 0 && nowYear % 100 != 0 ? 29 : 28;
        } else {
            return 30;
        }
    }

    /**
     * 获取现在的时间字符串
     *
     * @return 年:月:日:时:分
     */
    
    public static String getNowString() {
        String timeGroup = new SimpleDateFormat("yyyy:MM:dd:HH:mm").format(new Date());
        int nowYear = Integer.parseInt(timeGroup.split(":")[0]);
        int nowMonth = Integer.parseInt(timeGroup.split(":")[1]);
        int nowDay = Integer.parseInt(timeGroup.split(":")[2]);
        int nowHour = Integer.parseInt(timeGroup.split(":")[3]);
        int nowMinute = Integer.parseInt(timeGroup.split(":")[4]);
        return nowYear + ":" + nowMonth + ":" + nowDay + ":" + nowHour + ":" + nowMinute;
    }

    /**
     * 获取现在的时间字符串
     *
     * @return ...年...月...日...时...分
     */
    
    public static String getNowTimeString() {
        String timeGroup = new SimpleDateFormat("yyyy:MM:dd:HH:mm").format(new Date());
        int nowYear = Integer.parseInt(timeGroup.split(":")[0]);
        int nowMonth = Integer.parseInt(timeGroup.split(":")[1]);
        int nowDay = Integer.parseInt(timeGroup.split(":")[2]);
        int nowHour = Integer.parseInt(timeGroup.split(":")[3]);
        int nowMinute = Integer.parseInt(timeGroup.split(":")[4]);
        return nowYear + "年" + nowMonth + "月" + nowDay + "日" + nowHour + "时" + nowMinute + "分";
    }

    /**
     * 获取现在的时间+分钟后的字符串
     *
     * @param m 分钟
     * @return 年:月:日:时:分
     */
    
    public static String getTime(int m) {
        return getTime(m, 0, 0);
    }

    /**
     * 获取现在的时间+分钟,小时后的字符串
     *
     * @param m 分钟
     * @param h 小时
     * @return 年:月:日:时:分
     */
    
    public static String getTime(int m, int h) {
        return getTime(m, h, 0);
    }

    /**
     * 获取现在的时间+分钟,小时,天后的字符串
     *
     * @param m 分钟
     * @param h 小时
     * @param d 天数
     * @return 年:月:日:时:分
     */
    
    public static String getTime(int m, int h, int d) {
        String timeGroup = new SimpleDateFormat("yyyy:MM:dd:HH:mm").format(new Date());
        int nowYear = Integer.parseInt(timeGroup.split(":")[0]);
        int nowMonth = Integer.parseInt(timeGroup.split(":")[1]);
        int nowDay = Integer.parseInt(timeGroup.split(":")[2]);
        int nowHour = Integer.parseInt(timeGroup.split(":")[3]);
        int nowMinute = Integer.parseInt(timeGroup.split(":")[4]);

        int monthday = getMonthDay();

        int minute = m + nowMinute;
        int hour = h + nowHour;
        int day = d + nowDay;
        int month = nowMonth;
        int year = nowYear;

        if (minute >= 60) {
            hour += minute / 60;
            minute %= 60;
        }
        if (hour >= 24) {
            day += hour / 24;
            hour %= 24;
        }
        if (day > monthday) {
            int t_d = day;
            int t_m = month;
            int t_y = year;
            boolean first = true;
            while (true) {
                if (t_d - monthday <= 0) {
                    if (t_d - monthday != 0) {
                        if (first) {
                            month++;
                        }
                    }
                    day = t_d;
                    break;
                } else {
                    first = false;
                    month++;
                    t_d -= monthday;
                    if (++t_m > 12) {
                        t_m = 1;
                        t_y += 1;
                    }
                    monthday = getMonthDay(t_m, t_y);
                }
            }
        }
        if (month > 12) {
            year += month / 12;
            month %= 12;
        }
        return year + ":" + month + ":" + day + ":" + hour + ":" + minute;
    }

    /**
     * 获取这个分钟后换算的时间
     *
     * @param m 分钟
     * @return 日:时:分
     */
    
    public static String getDay(int m) {
        return getDay(m, 0, 0);
    }

    /**
     * 获取这个分钟,小时后换算的时间
     *
     * @param m 分钟
     * @param h 小时
     * @return 日:时:分
     */
    
    public static String getDay(int m, int h) {
        return getDay(m, h, 0);
    }

    /**
     * 获取这个分钟,小时,天数后换算的时间
     *
     * @param m 分钟
     * @param h 小时
     * @param d 天数
     * @return 日:时:分
     */
    
    public static String getDay(int m, int h, int d) {
        int minute = m;
        int hour = h;
        int day = d;

        if (minute >= 60) {
            hour += minute / 60;
            minute %= 60;
        }
        if (hour >= 24) {
            day += hour / 24;
            hour %= 24;
        }
        return day + ":" + hour + ":" + minute;
    }

    /**
     * 序列化这个时间的字符串
     * 配合反序列化可查看是否到/超过这个时间
     *
     * @param second ...秒后
     * @return 时间字符串
     */
    
    public static String serializationTime(long second) {
        return serializationTime(second, 0, 0, 0);
    }

    /**
     * 序列化这个时间的字符串
     * 配合反序列化可查看是否到/超过这个时间
     *
     * @param second ...秒后
     * @param minute ...分钟后
     * @return 时间字符串
     */
    
    public static String serializationTime(long second, long minute) {
        return serializationTime(second, minute, 0, 0);
    }

    /**
     * 序列化这个时间的字符串
     * 配合反序列化可查看是否到/超过这个时间
     *
     * @param second ...秒后
     * @param minute ...分钟后
     * @param hour   ...小时后
     * @return 时间字符串
     */
    
    public static String serializationTime(long second, long minute, long hour) {
        return serializationTime(second, minute, hour, 0);
    }

    /**
     * 序列化这个时间的字符串
     * 配合反序列化可查看是否到/超过这个时间
     *
     * @param second ...秒后
     * @param minute ...分钟后
     * @param hour   ...小时后
     * @param day    ...天后
     * @return 时间字符串
     */
    
    public static String serializationTime(long second, long minute, long hour, long day) {
        return LocalDateTime.now().plusSeconds(second).plusMinutes(minute).plusHours(hour).plusDays(day).toString();
    }

    /**
     * 反序列化时间字符串并进行判断
     * 查看时间是否到，如果到了或超过了为true
     * 否则为false
     *
     * @param timeString 时间字符串
     * @return 是否到了这个时间
     */
    public static boolean deserializationTime(String timeString) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime next = LocalDateTime.parse(timeString);
        return Duration.between(now, next).getSeconds() >= 0;
    }
}


