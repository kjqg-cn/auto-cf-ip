package com.hl.opnc;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * @author kjqg-cn
 * @date 2022-08-17
 */
public class Response {

    private static final Logger log = LoggerFactory.getLogger(Response.class);

    private static final String RESPONSE_IS_NULL = "response is null";

    /**
     * 本次调用使用的 HttpClient 实例
     */
    CloseableHttpClient httpClient;

    /**
     * 本次调用响应的 HttpResponse 实例
     */
    CloseableHttpResponse response;

    public CloseableHttpClient getHttpClient() {
        return httpClient;
    }

    public void setHttpClient(CloseableHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public CloseableHttpResponse getResponse() {
        return response;
    }

    public void setResponse(CloseableHttpResponse response) {
        this.response = response;
    }

    public String getRespStr() throws IOException {
        if (response == null) {
            log.debug(RESPONSE_IS_NULL);
            return null;
        }
        HttpEntity entity = response.getEntity();
        return EntityUtils.toString(entity, StandardCharsets.UTF_8);
    }

    public InputStream getInputStream() throws IOException {
        if (response == null) {
            log.debug(RESPONSE_IS_NULL);
            return null;
        }
        HttpEntity entity = response.getEntity();
        return entity.getContent();
    }

    public void close() throws IOException {
        if (response == null) {
            log.debug(RESPONSE_IS_NULL);
            return;
        }
        response.close();
        if (httpClient == null) {
            log.debug("httpClient is null");
            return;
        }
        httpClient.close();
    }

}
