package com.cf.config;

import java.io.InputStream;
import java.util.Properties;

/**
 * cf.conf 配置读取类
 */
public class CfConfigReader {

    public static String readConfig(String key) throws Exception {
        Properties properties = new Properties();
        // 使用 ClassLoader 获取资源流
        InputStream input = CfConfigReader.class.getClassLoader().getResourceAsStream("cf.conf");
        if (input != null) {
            // 加载配置文件
            properties.load(input);
            // 读取配置项
            String property = properties.getProperty(key);
            if (property == null || property.isEmpty()) {
                throw new Exception("[" + key + "] 配置不能为空");
            }
            return property;
        } else {
            throw new Exception("[" + key + "] 配置读取失败");
        }
    }

}
