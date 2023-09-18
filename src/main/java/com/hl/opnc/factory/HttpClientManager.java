package com.hl.opnc.factory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author kjqg-cn
 * @date 2022-08-17
 */
public class HttpClientManager {

    private static final Logger log = LoggerFactory.getLogger(HttpClientManager.class);

    /**
     * TODO 此处【全限定类名】配置需和 IgnoreSslHttpClientImpl.java 所在路径保持一致
     */
    private static final String HTTP_CLIENT_CLASS_NAME = "com.hl.opnc.factory.impl.IgnoreSslHttpClientImpl";

    public static HttpClientFactory getHttpClient() {

        log.debug("httpClientClassName: " + HTTP_CLIENT_CLASS_NAME);

        Object o;
        try {
            Class<?> defaultHttpClientImpl = Class.forName(HTTP_CLIENT_CLASS_NAME);
            o = defaultHttpClientImpl.newInstance();
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        return (HttpClientFactory) o;
    }

}
