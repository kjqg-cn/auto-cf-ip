package com.cf;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.cf.utils.FileUtil;
import com.cf.utils.RegexUtil;
import com.hl.opnc.invoke.HttpUtil;
import org.apache.commons.exec.*;
import org.apache.commons.io.FileUtils;
import org.apache.http.message.BasicHeader;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.util.ArrayList;

import org.apache.http.Header;

import java.util.List;

/**
 * Cloudflare API
 */
public class CloudflareApiTest {

    /**
     * 注册cloudflare的邮箱
     */
    private static final String CF_EMAIL = "替换这里";//TODO

    /**
     * 登录cloudflare，点击你将要优选的域名
     * Overview > [右下角]API > Zone ID
     */
    private static final String CF_API_KEY = "替换这里";//TODO

    /**
     * Overview > [右下角]API > Get your API token
     */
    private static final String CF_ZONE_ID = "替换这里";//TODO

    /**
     * 托管到cf的域名
     * 如果是一级域名，举例【domain.com】；
     * 如果是二级域名，举例【domain.eu.org】
     */
    private static final String YOUR_DOMAIN = "替换这里";//TODO

    /**
     * 优选域名
     * 如果托管域名是一级域名，优选域名举例【fast.domain.com】；
     * 如果是二级域名，优选域名举例【fast.domain.eu.org】
     */
    private static final String PREFERRED_DOMAIN = "替换这里";//TODO

    /**
     * NOTE: 需要先从 [https://zip.baipiao.eu.org] 下载压缩包
     * IP库zip目录【文件目录+文件名】
     * 举例：D:\Download\txt.zip
     * "\"粘贴到代码的字符串中，会自动变成"\\"，是正常现象，不要手动修改成"\"
     */
    private static final String PREFERRED_IP_POOL_PATH = "替换这里";//TODO

    /**
     * CloudflareST.exe 所在路径【仅exe所在目录，不含文件名】
     * 举例：D:\优选IP
     */
    private static final String CF_ST_EXE_PATH = "替换这里";//TODO

    /**
     * 已经托管到CF优选域名里的IP的ping超时时间，单位 ms
     * ping超时时间不建议设置太小，500-2000之间即可，有时候实际ping几十毫秒，但在程序里可能会几百毫秒
     */
    private static final int PINT_TIMEOUT = 2000;

    /**
     * 优选IP超时时间，单位 ms
     * 根据IP池数量和优选数量而定，10000个IP优选10个，一般10分钟内能完成，可以根据实际情况进行调整
     */
    private static final int PREFERRED_IP_TIMEOUT = 10 * 60 * 1000;

    /**
     * 优选IP的端口
     * HTTP: 80 | 8080 | 8880 | 2052 | 2082 | 2086 | 2095
     * HTTPS: 443 | 2053 | 2083 | 2087 | 2096 | 8443
     */
    private static final int PREFERRED_IP_PORT = 443;

    /**
     * 优选IP的测速地址
     * 如果优选的IP下载速度全是0，则需要修改测速地址
     */
    private static final String PREFERRED_IP_URL = "https://vipcs.cloudflarest.link";

    /**
     * 优选IP的最小下载速度，单位 mb/s
     */
    private static final int PREFERRED_IP_DOWN_SPEED = 3;

    /**
     * 优选IP的最大延迟，单位 ms
     */
    private static final int PREFERRED_IP_DELAY = 200;

    /**
     * 优选IP的数量
     */
    private static final int PREFERRED_IP_COUNT = 10;

    /**
     * 优选IP的命令
     */
    private static final String PREFERRED_IP_CMD = CF_ST_EXE_PATH + "\\CloudflareST.exe" +
            " -f '" + CF_ST_EXE_PATH + "\\ip.txt' " +
            " -o '" + CF_ST_EXE_PATH + "\\result.csv' " +
            " -tp " + PREFERRED_IP_PORT +
            " -url " + PREFERRED_IP_URL +
            " -sl " + PREFERRED_IP_DOWN_SPEED +
            " -tl " + PREFERRED_IP_DELAY +
            " -dn " + PREFERRED_IP_COUNT;

    @Test
    public void autoPreferredDomain() throws Exception {
        //查询DNS
        // https://api.cloudflare.com/client/v4/zones/{zone_id}/dns_records
        String listUrl = "https://api.cloudflare.com/client/v4/zones/%s/dns_records?name=contains:%s";
        String formatUrl = String.format(listUrl, CF_ZONE_ID, PREFERRED_DOMAIN);

        List<Header> headers = new ArrayList<>();
        headers.add(new BasicHeader("X-Auth-Email", CF_EMAIL));
        headers.add(new BasicHeader("X-Auth-Key", CF_API_KEY));

        String listResp = HttpUtil.sendGet(formatUrl, headers);

        JSONObject jsonResp = JSONObject.parseObject(listResp);
        Boolean success = jsonResp.getBoolean("success");
        if (success == null || !success) {
            System.out.println("cloudflare api >>> [" + YOUR_DOMAIN + "] DNS查询失败");
            return;
        }

        int i = 0;
        JSONArray resultArr = jsonResp.getJSONArray("result");
        for (Object obj : resultArr) {
            JSONObject oneResult = JSONObject.parseObject(obj.toString());
            String ip = oneResult.getString("content");
            try {
                InetAddress inetAddress = InetAddress.getByName(ip);
                boolean isReachable = inetAddress.isReachable(PINT_TIMEOUT);

                if (isReachable) {
                    System.out.println(ip + " 可以ping通，继续保留 √");
                    i++;
                } else {
                    System.out.println(ip + " ping超时，准备删除 ...");
                    String dnsId = oneResult.getString("id");
                    cloudflareDnsDelete(dnsId, ip);
                }
            } catch (IOException e) {
                System.err.println("cloudflare List API RESULT OPERATE EXCEPTION " + e.getMessage());
            }
        }

        if (i > 20) {
            System.err.println("优选IP已经足够多了，不需要进行优选了哦~");
            return;
        }

        /*//本来是想实现自动下载压缩包，但403问题短时间没解决掉，那就先手动下载，填入目录了[PREFERRED_IP_POOL_PATH]
        String zipPath = "D:\\Download\\txt.zip";
        // 先删除
        FileUtils.deleteDirectory(new File(zipPath));
        try {
            // 设置HTTP代理
            System.setProperty("http.proxyHost", "127.0.0.1");
            System.setProperty("http.proxyPort", "10809");

            FileUtil.downloadFromUrl(PREFERRED_IP_UPDATE_URL, zipPath);
        } catch (Exception e) {
            System.out.println("优选IP池下载失败 >> " + e.getMessage());
            return;
        }*/

        String ipPath = CF_ST_EXE_PATH + "\\ip";
        // 先删除
        FileUtils.deleteDirectory(new File(ipPath));
        try {
            FileUtil.unzip(PREFERRED_IP_POOL_PATH, ipPath);
        } catch (Exception e) {
            System.out.println("优选IP池文件解压失败 >> " + e.getMessage());
            return;
        }

        List<String> fileList = new ArrayList<>();
        List<String> ipFileList = FileUtil.listFiles(new File(ipPath), fileList);
        List<String> ipList = new ArrayList<>();
        for (String ip : ipFileList) {
            String[] split = ip.split("\\\\");
            String fileName = split[split.length - 1];
            if (fileName.contains("80") || fileName.contains("443")) {
                List<String> list = FileUtil.readLocalFileByLines(ip);
                for (String aip : list) {
                    boolean isIp = RegexUtil.isIp(aip);
                    if (isIp) {
                        ipList.add(aip);
                    }
                }
            }
        }

        try {
            // 使用String.join方法将List<String>转换为以换行符分隔的字符串
            String result = String.join(System.lineSeparator(), ipList);
            String ipTxtPath = CF_ST_EXE_PATH + "\\ip.txt";
            // 先删除
            FileUtils.deleteQuietly(new File(ipTxtPath));
            FileUtil.writeUsingFileWriter(result, ipTxtPath);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("优选IP池文件筛选到ip.txt失败 >> " + e.getMessage());
            return;
        }

        runPreferredIp();
    }

    public static void cloudflareDnsDelete(String dnsId, String ip) {
        // 删除DNS
        // https://api.cloudflare.com/client/v4/zones/{zone_id}/dns_records/{dns_id}
        String delUrl = "https://api.cloudflare.com/client/v4/zones/%s/dns_records/%s";
        String formatUrl = String.format(delUrl, CF_ZONE_ID, dnsId);

        List<Header> headers = new ArrayList<>();
        headers.add(new BasicHeader("X-Auth-Email", CF_EMAIL));
        headers.add(new BasicHeader("X-Auth-Key", CF_API_KEY));

        String delResp = HttpUtil.sendDelete(formatUrl, headers);
        JSONObject jsonResp = JSONObject.parseObject(delResp);
        Boolean success = jsonResp.getBoolean("success");
        if (success == null || !success) {
            System.out.println("cloudflare api >>> [" + PREFERRED_DOMAIN + "] " + ip + " DNS删除失败");
        } else {
            System.out.println("[" + PREFERRED_DOMAIN + "] " + ip + " DNS删除成功");
        }
    }

    public static void runPreferredIp() {
        try {
            DefaultExecutor executor = new DefaultExecutor();
            executor.setStreamHandler(new PumpStreamHandler(System.out, System.err));
            // 设置超时时间
            ExecuteWatchdog watchdog = new ExecuteWatchdog(PREFERRED_IP_TIMEOUT);
            executor.setWatchdog(watchdog);

            executor.execute(CommandLine.parse(PREFERRED_IP_CMD), new ExecuteResultHandler() {
                @Override
                public void onProcessComplete(int exitValue) {
                    System.out.println("优选IP执行完成，退出码：" + exitValue);

                    //稍等3秒，生成文件
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        //一般没啥问题
                    }

                    System.out.println("开始向cloudflare优选域名中添加优选IP...");
                    cloudflareDnsAdd();
                    System.out.println("优选IP执行结束");
                }

                @Override
                public void onProcessFailed(ExecuteException e) {
                    System.out.println("优选IP执行失败：" + e.getMessage());
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void cloudflareDnsAdd() {
        List<String> list = FileUtil.readLocalFileByLines(CF_ST_EXE_PATH + "\\result.csv");
        List<String> ipList = new ArrayList<>();
        for (String text : list) {
            String[] split = text.split(",");
            int length = split.length;
            String downSpeed = split[length - 1];
            try {
                BigDecimal downBd = new BigDecimal(downSpeed);
                if (downBd.compareTo(BigDecimal.valueOf(3)) > 0) {
                    String ip = split[0];
                    ipList.add(ip);
                }
            } catch (Exception e) {
                // 一般是第一行转换报错，不用管
            }
        }
        if (ipList.size() <= 0) {
            System.out.println("没有符合条件的优选IP，建议修改优选IP的参数，重新执行");
            return;
        }

        // 新增DNS
        // https://api.cloudflare.com/client/v4/zones/{zone_identifier}/dns_records
        String addUrl = "https://api.cloudflare.com/client/v4/zones/%s/dns_records";
        String formatUrl = String.format(addUrl, CF_ZONE_ID);

        for (String ip : ipList) {
            // "{\n  \"content\": \"198.51.100.4\",\n  \"name\": \"example.com\",\n  \"proxied\": false,\n  \"type\": \"A\",\n  \"comment\": \"Domain verification record\",\n  \"tags\": [\n    \"owner:dns-team\"\n  ],\n  \"ttl\": 3600\n}"
            JSONObject bodyJson = new JSONObject();
            // 优选ip
            bodyJson.put("content", ip);
            // 二级域名
            bodyJson.put("name", PREFERRED_DOMAIN);
            // 是否代理
            bodyJson.put("proxied", false);
            // 类型
            bodyJson.put("type", "A");
            // Zone ID
            bodyJson.put("zone_id", CF_ZONE_ID);
            // 一级域名
            bodyJson.put("zone_name", YOUR_DOMAIN);
            String body = bodyJson.toJSONString();

            List<Header> headers = new ArrayList<>();
            headers.add(new BasicHeader("X-Auth-Email", CF_EMAIL));
            headers.add(new BasicHeader("X-Auth-Key", CF_API_KEY));

            String resp = HttpUtil.sendPost(formatUrl, headers, body);
            JSONObject jsonResp = JSONObject.parseObject(resp);
            Boolean success = jsonResp.getBoolean("success");
            if (success) {
                System.out.println("[" + PREFERRED_DOMAIN + "] " + ip + " DNS添加成功");
            } else {
                JSONArray errors = jsonResp.getJSONArray("errors");
                StringBuilder sb = new StringBuilder();
                int size = errors.size();
                int i = 0;
                for (Object errorObj : errors) {
                    i++;
                    JSONObject errorJSon = JSONObject.parseObject(errorObj.toString());
                    String message = errorJSon.getString("message");
                    sb.append(message);
                    if (i < size) {
                        sb.append(" && ");
                    }
                }
                System.out.println("cloudflare api >>> [" + PREFERRED_DOMAIN + "] " + ip + " DNS添加失败 >> " + sb);
            }
            System.out.println("---------------------------------------------------------------------");
        }
    }

}