package com.cf.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 正则验证工具类
 *
 * @author kjqg-cn
 * @date 2020-04-07 11:35:29
 */
public class RegexUtil {

    /**
     * ip地址正则表达式
     */
    private static final String CHECK_IP_ADDRESS = "^(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)(\\.(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)){3}$";

    /**
     * url正则表达式
     */
    private static final String CHECK_URL = "^(http|www|ftp|)?(://)?(\\w+(-\\w+)*)(\\.(\\w+(-\\w+)*))*((:\\d+)?)(/(\\w+(-\\w+)*))*(\\.?(\\w)*)(\\?)?" +
            "(((\\w*%)*(\\w*\\?)*(\\w*:)*(\\w*\\+)*(\\w*\\.)*(\\w*&)*(\\w*-)*(\\w*=)*(\\w*%)*(\\w*\\?)*" +
            "(\\w*:)*(\\w*\\+)*(\\w*\\.)*" +
            "(\\w*&)*(\\w*-)*(\\w*=)*)*(\\w*)*)$";


////------------------------------------验证方法------------------------------------

    /**
     * 判断是否为url 符合返回true
     *
     * @param ip ip地址
     * @return boolean
     */
    public static boolean isIp(String ip) {
        return regular(ip, CHECK_IP_ADDRESS);
    }

    /**
     * 判断是否为url 符合返回true
     *
     * @param url url
     * @return boolean
     */
    public static boolean isUrl(String url) {
        return regular(url, CHECK_URL);
    }

    /**
     * 匹配是否符合正则表达式pattern 匹配返回true
     *
     * @param str     匹配的字符串
     * @param pattern 匹配模式
     * @return boolean
     */
    private static boolean regular(String str, String pattern) {
        if (null == str || str.trim().length() <= 0) {
            return false;
        }
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(str);
        return m.matches();
    }

}