package com.hl.opnc.factory;

import org.apache.http.impl.client.CloseableHttpClient;

/**
 * @author kjqg-cn
 * @date 2022-08-17
 */
public interface HttpClientFactory {

    /**
     * 创建 CloseableHttpClient
     *
     * @return CloseableHttpClient
     */
    CloseableHttpClient create();

}
