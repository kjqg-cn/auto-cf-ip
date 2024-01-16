package com.cf.utils;

/**
 * 日期/时间格式化
 */
public class DateUtil {

    /**
     * 毫秒格式化
     *
     * @param milliseconds 毫秒
     * @return 格式化时间
     */
    public static String millisecondsFormat(long milliseconds) {
        // 计算各时间单位的值
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        // 计算剩余的时间
        seconds %= 60;
        minutes %= 60;
        hours %= 24;
        long millisecondsRemaining = milliseconds % 1000;

        // 构建时间间隔字符串
        StringBuilder result = new StringBuilder();
        if (days > 0) {
            result.append(days).append("天");
        }
        if (hours > 0) {
            result.append(hours).append("小时");
        }
        if (minutes > 0) {
            result.append(minutes).append("分钟");
        }
        if (seconds > 0) {
            result.append(seconds).append("秒");
        }
        result.append(millisecondsRemaining).append("毫秒");

        // 返回时间间隔
        return result.toString();
    }

}
