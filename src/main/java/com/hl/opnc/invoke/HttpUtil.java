package com.hl.opnc.invoke;

import com.alibaba.fastjson.JSON;
import com.hl.opnc.Response;
import com.hl.opnc.factory.HttpClientManager;
import com.hl.opnc.multipart.FileMultipartParam;
import com.hl.opnc.multipart.InputStreamMultipartParam;
import com.hl.opnc.multipart.MultipartParam;
import com.hl.opnc.multipart.TextMultipartParam;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.*;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * HTTP请求
 *
 * @author kjqg-cn
 * @date 2022-11-23
 */
public class HttpUtil {

    private static final Logger log = LoggerFactory.getLogger(HttpUtil.class);

    /**
     * application/json
     */
    public static final String CONTENT_TYPE_JSON_UTF8 = "application/json; charset=utf-8";

    /**
     * @param url     请求url
     * @param headers 请求头
     * @return 响应数据
     */
    public static String sendGet(String url, List<Header> headers) {
        if (headers == null) {
            headers = new ArrayList<>();
        }

        return baseSendGet(url, headers);
    }

    /**
     * 发送 POST: application/json 请求
     *
     * @param url     请求URL
     * @param headers 请求头
     * @param body    请求体
     * @return 响应数据
     */
    public static String sendPost(String url, List<Header> headers, String body) {
        if (headers == null) {
            headers = new ArrayList<>();
        }

        // CONTENT_TYPE JSON UTF_8
        headers.add(new BasicHeader(HttpHeaders.CONTENT_TYPE, CONTENT_TYPE_JSON_UTF8));

        // 设置json请求体
        StringEntity stringEntity = null;
        if (body != null) {
            stringEntity = new StringEntity(body, StandardCharsets.UTF_8);
        }
        return baseSendPost(url, headers, stringEntity);
    }

    /**
     * 发送 PUT: application/json 请求
     *
     * @param url     请求URL
     * @param headers 请求头
     * @param body    请求体
     * @return 响应数据
     */
    public static String sendPut(String url, List<Header> headers, String body) {
        if (headers == null) {
            headers = new ArrayList<>();
        }

        // CONTENT_TYPE JSON UTF_8
        headers.add(new BasicHeader(HttpHeaders.CONTENT_TYPE, CONTENT_TYPE_JSON_UTF8));

        // 设置json请求体
        StringEntity stringEntity = null;
        if (body != null) {
            stringEntity = new StringEntity(body, StandardCharsets.UTF_8);
        }
        return baseSendPut(url, headers, stringEntity);
    }

    /**
     * 发送 POST: multipart/form-data请求
     *
     * @param url             请求地址
     * @param headers         请求头
     * @param multipartParams 复合参数列表
     * @return String
     */
    public static String sendPost(String url, List<Header> headers, List<MultipartParam> multipartParams) throws Exception {
        List<InputStream> closeList = new ArrayList<>();

        //  将 multipartParams 解析为 MultipartEntityBuilder
        MultipartEntityBuilder builder = MultipartEntityBuilder.create().setMode(HttpMultipartMode.RFC6532);

        for (MultipartParam multipartParam : multipartParams) {
            // FileMultipartParam
            if (multipartParam instanceof FileMultipartParam) {
                FileMultipartParam param = (FileMultipartParam) multipartParam;
                String name = param.getName();
                File file = param.getFile();
                String filename = param.getFilename();
                FileInputStream fileInputStream = new FileInputStream(file);
                closeList.add(fileInputStream);
                builder.addBinaryBody(name, fileInputStream, ContentType.DEFAULT_BINARY, filename != null ? filename : file.getName());
            }
            // InputStreamMultipartParam
            if (multipartParam instanceof InputStreamMultipartParam) {
                InputStreamMultipartParam param = (InputStreamMultipartParam) multipartParam;
                String name = param.getName();
                InputStream inputStream = param.getInputStream();
                String filename = param.getFilename();
                closeList.add(inputStream);
                builder.addBinaryBody(name, inputStream, ContentType.DEFAULT_BINARY, filename != null ? filename : "");
            }
            // TextMultipartParam
            if (multipartParam instanceof TextMultipartParam) {
                TextMultipartParam param = (TextMultipartParam) multipartParam;
                String name = param.getName();
                String value = param.getValue();
                builder.addTextBody(name, value, ContentType.MULTIPART_FORM_DATA.withCharset(StandardCharsets.UTF_8));
            }
        }

        // 发送post请求
        String response = baseSendPost(url, headers, builder.build());
        // 关闭资源
        closeInputStreamList(closeList);
        return response;
    }

    /**
     * 发送 DELETE 请求
     *
     * @param url     请求URL
     * @param headers 请求头
     * @return 响应数据
     */
    public static String sendDelete(String url, List<Header> headers) {
        if (headers == null) {
            headers = new ArrayList<>();
        }

        // CONTENT_TYPE JSON UTF_8
        headers.add(new BasicHeader(HttpHeaders.CONTENT_TYPE, CONTENT_TYPE_JSON_UTF8));

        return baseSendDelete(url, headers);
    }

    /**
     * 基础 GET请求
     *
     * @param url     请求地址
     * @param headers 请求头
     * @return 响应数据
     */
    private static String baseSendGet(String url, List<Header> headers) {
        CloseableHttpClient httpClient = HttpClientManager.getHttpClient().create();

        HttpGet httpGet = new HttpGet(url);
        if (headers != null && !headers.isEmpty()) {
            httpGet.setHeaders(headers.stream().filter(Objects::nonNull).toArray(Header[]::new));
        }

        return executeRequest(httpClient, httpGet);
    }

    /**
     * 关闭输入流列表
     *
     * @param closeList 输入流列表
     */
    private static void closeInputStreamList(List<InputStream> closeList) {
        log.debug("closeList: " + JSON.toJSONString(closeList));
        if (closeList.isEmpty()) {
            return;
        }

        for (InputStream inputStream : closeList) {
            if (inputStream == null) {
                continue;
            }
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 基础 POST请求
     *
     * @param url     请求地址
     * @param headers 请求头
     * @param entity  请求体
     * @return 响应数据
     */
    private static String baseSendPost(String url, List<Header> headers, HttpEntity entity) {
        CloseableHttpClient httpClient = HttpClientManager.getHttpClient().create();

        HttpPost httpPost = new HttpPost(url);
        if (headers != null && !headers.isEmpty()) {
            httpPost.setHeaders(headers.stream().filter(Objects::nonNull).toArray(Header[]::new));
        }

        if (entity != null) {
            httpPost.setEntity(entity);
        }

        return executeRequest(httpClient, httpPost);
    }

    /**
     * 基础 PUT请求
     *
     * @param url     请求地址
     * @param headers 请求头
     * @param entity  请求体
     * @return 响应数据
     */
    private static String baseSendPut(String url, List<Header> headers, HttpEntity entity) {
        CloseableHttpClient httpClient = HttpClientManager.getHttpClient().create();

        HttpPut httpPost = new HttpPut(url);
        if (headers != null && !headers.isEmpty()) {
            httpPost.setHeaders(headers.stream().filter(Objects::nonNull).toArray(Header[]::new));
        }

        if (entity != null) {
            httpPost.setEntity(entity);
        }

        return executeRequest(httpClient, httpPost);
    }

    /**
     * 基础 DELETE 请求
     *
     * @param url     请求地址
     * @param headers 请求头
     * @return 响应数据
     */
    private static String baseSendDelete(String url, List<Header> headers) {
        CloseableHttpClient httpClient = HttpClientManager.getHttpClient().create();

        HttpDelete httpDelete = new HttpDelete(url);
        if (headers != null && !headers.isEmpty()) {
            httpDelete.setHeaders(headers.stream().filter(Objects::nonNull).toArray(Header[]::new));
        }

        return executeRequest(httpClient, httpDelete);
    }

    /**
     * 执行http请求
     *
     * @param httpClient 客户端
     * @param request    请求信息
     * @return 响应数据
     */
    private static String executeRequest(CloseableHttpClient httpClient, HttpUriRequest request) {
        Response response = new Response();
        response.setHttpClient(httpClient);
        try {
            response.setResponse(httpClient.execute(request));
        } catch (IOException e) {
            e.printStackTrace();
            try {
                httpClient.close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
        log.info("responseCode: " + response.getResponse().getStatusLine().getStatusCode());

        StringBuilder result = new StringBuilder();
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(response.getInputStream(), StandardCharsets.UTF_8));
            String line;
            // 读取返回的内容
            while ((line = in.readLine()) != null) {
                result.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                    response.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return result.toString();
    }

}
