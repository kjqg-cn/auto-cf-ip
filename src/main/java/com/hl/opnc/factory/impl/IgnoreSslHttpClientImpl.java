package com.hl.opnc.factory.impl;

import com.hl.opnc.factory.HttpClientFactory;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

/**
 * @author kjqg-cn
 * @date 2022-08-17
 */
public class IgnoreSslHttpClientImpl implements HttpClientFactory {

    private static final Logger log = LoggerFactory.getLogger(IgnoreSslHttpClientImpl.class);

    /**
     * 设置连接超时时间 TODO 可选，默认为空
     */
    String connectTimeout = "";

    /**
     * 设置读取超时时间 TODO 可选，默认为空
     */
    String socketTimeout = "";

    /**
     * 设置代理 TODO 可选，默认为空
     */
    String proxyHostname = "";

    /**
     * 代理端口 TODO 可选，默认为空
     */
    String proxyPort = "";

    @Override
    public CloseableHttpClient create() {
        SSLConnectionSocketFactory scsf;
        try {
            scsf = new SSLConnectionSocketFactory(SSLContexts
                    .custom()
                    .loadTrustMaterial(null, new TrustSelfSignedStrategy())
                    .build(), NoopHostnameVerifier.INSTANCE);
        } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {
            log.debug("createIgnoreVerifySsl error");
            throw new RuntimeException(e);
        }
        HttpClientBuilder builder = HttpClients.custom().setSSLSocketFactory(scsf);
        RequestConfig.Builder custom = RequestConfig.custom();

        log.debug("connectTimeout: " + connectTimeout);

        if (!"".equals(connectTimeout)) {
            custom.setConnectTimeout(Integer.parseInt(connectTimeout));
        }

        log.debug("socketTimeout: " + socketTimeout);

        if (!"".equals(socketTimeout)) {
            custom.setSocketTimeout(Integer.parseInt(socketTimeout));
        }

        log.debug("proxyHostname: " + proxyHostname);

        if (!"".equals(proxyHostname)) {
            log.debug("proxyPort: " + proxyPort);

            custom.setProxy("".equals(proxyPort)
                    ? new HttpHost(proxyHostname)
                    : new HttpHost(proxyHostname, Integer.parseInt(proxyPort)));
        }

        builder.setDefaultRequestConfig(custom.build());
        return builder.build();
    }

}
