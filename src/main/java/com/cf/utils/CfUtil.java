package com.cf.utils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.cf.config.CfConfigReader;
import com.hl.opnc.invoke.HttpUtil;
import org.apache.commons.exec.*;
import org.apache.commons.io.FileUtils;
import org.apache.http.Header;
import org.apache.http.message.BasicHeader;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * Cloudflare接口调用 & 优选IP执行
 */
public class CfUtil {

    private static final String LIST_URL = "https://api.cloudflare.com/client/v4/zones/%s/dns_records?name=contains:%s";
    private static final String ADD_URL = "https://api.cloudflare.com/client/v4/zones/%s/dns_records";
    private static final String UPDATE_URL = "https://api.cloudflare.com/client/v4/zones/%s/dns_records/%s";
    private static final String DELETE_URL = "https://api.cloudflare.com/client/v4/zones/%s/dns_records/%s";

    /**
     * cf-api 查询dns
     *
     * @return 可用dns数量
     */
    public static List<String> cfDnsList() throws Exception {
        long start = System.currentTimeMillis();
        //查询DNS
        // https://api.cloudflare.com/client/v4/zones/{zone_id}/dns_records
        String formatUrl = String.format(LIST_URL, CfConfigReader.readConfig("CF_ZONE_ID"), CfConfigReader.readConfig("PREFERRED_DOMAIN"));

        List<Header> headers = new ArrayList<>();
        headers.add(new BasicHeader("X-Auth-Email", CfConfigReader.readConfig("CF_EMAIL")));
        headers.add(new BasicHeader("X-Auth-Key", CfConfigReader.readConfig("CF_API_KEY")));

        String listResp = HttpUtil.sendGet(formatUrl, headers);

        JSONObject jsonResp = JSONObject.parseObject(listResp);
        Boolean success = jsonResp.getBoolean("success");
        if (success == null || !success) {
            System.out.println("cloudflare api >>> [" + CfConfigReader.readConfig("YOUR_DOMAIN") + "] DNS查询失败");
            return null;
        }

        JSONArray resultArr = jsonResp.getJSONArray("result");
        if (resultArr.isEmpty()) {
            System.out.println("优选域名[" + CfConfigReader.readConfig("PREFERRED_DOMAIN") + "]下未查询到DNS记录");
            return Collections.emptyList();
        }

        List<String> existedIpList = new ArrayList<>();
        System.out.println("优选域名[" + CfConfigReader.readConfig("PREFERRED_DOMAIN") + "]下已存在 " + resultArr.size() + " 个优选IP");
        System.out.println("开始检测IP是否可用...");
        for (Object obj : resultArr) {
            JSONObject oneResult = JSONObject.parseObject(obj.toString());
            String ip = oneResult.getString("content");
            try {
                InetAddress inetAddress = InetAddress.getByName(ip);
                String pingTimeout = CfConfigReader.readConfig("PING_TIMEOUT");
                boolean isReachable = inetAddress.isReachable(Integer.parseInt(pingTimeout));

                if (isReachable) {
                    System.out.println(ip + " 可以ping通，继续保留 √");
                    existedIpList.add(ip);

                    String comment = oneResult.getString("comment");
                    if (StringUtils.isEmpty(comment)) {
                        String dnsId = oneResult.getString("id");
                        String updateUrl = String.format(UPDATE_URL, CfConfigReader.readConfig("CF_ZONE_ID"), dnsId);
                        BodyResult bodyResult = getBodyResult(ip);
                        HttpUtil.sendPut(updateUrl, headers, bodyResult.body);
                    }

                } else {
                    System.out.println(ip + " ping超时，准备删除 ...");
                    String dnsId = oneResult.getString("id");
                    cfDnsDelete(dnsId, ip);
                }
            } catch (IOException e) {
                System.err.println("程序异常 >> " + e.getMessage());
            }
        }

        long end = System.currentTimeMillis();
        System.out.println("IP检测完成，耗时: " + DateUtil.millisecondsFormat(end - start));
        System.out.println("优选域名[" + CfConfigReader.readConfig("PREFERRED_DOMAIN") + "]下剩余可用优选IP " + existedIpList.size() + " 个\n");
        return existedIpList;
    }

    /**
     * cf-api 删除dns
     *
     * @param dnsId dns唯一标识
     * @param ip    删除的IP
     */
    public static void cfDnsDelete(String dnsId, String ip) throws Exception {
        // 删除DNS
        // https://api.cloudflare.com/client/v4/zones/{zone_id}/dns_records/{dns_id}
        String formatUrl = String.format(DELETE_URL, CfConfigReader.readConfig("CF_ZONE_ID"), dnsId);

        List<Header> headers = new ArrayList<>();
        headers.add(new BasicHeader("X-Auth-Email", CfConfigReader.readConfig("CF_EMAIL")));
        headers.add(new BasicHeader("X-Auth-Key", CfConfigReader.readConfig("CF_API_KEY")));

        String delResp = HttpUtil.sendDelete(formatUrl, headers);
        JSONObject jsonResp = JSONObject.parseObject(delResp);
        Boolean success = jsonResp.getBoolean("success");
        if (success == null || !success) {
            System.out.println("[" + CfConfigReader.readConfig("PREFERRED_DOMAIN") + "] >> " + ip + " DNS删除失败 :( ");
        } else {
            System.out.println("[" + CfConfigReader.readConfig("PREFERRED_DOMAIN") + "] >> " + ip + " DNS删除成功 :) ");
        }
    }

    /**
     * cf-api 新增dns
     */
    public static int cfDnsAdd() throws Exception {
        String projectDirectory = System.getProperty("user.dir") + "\\preferIp\\";
        List<String> list = FileUtil.readLocalFileByLines(projectDirectory + "result.csv");
        List<String> ipList = getPreferredIpList(list);
        if (ipList.isEmpty()) {
            System.out.println("没有符合条件的优选IP，建议修改优选IP的参数，重新执行");
            return 0;
        }

        // 新增DNS
        // https://api.cloudflare.com/client/v4/zones/{zone_identifier}/dns_records
        String formatUrl = String.format(ADD_URL, CfConfigReader.readConfig("CF_ZONE_ID"));

        int count = 0;
        for (String ip : ipList) {
            BodyResult bodyResult = getBodyResult(ip);

            List<Header> headers = new ArrayList<>();
            headers.add(new BasicHeader("X-Auth-Email", CfConfigReader.readConfig("CF_EMAIL")));
            headers.add(new BasicHeader("X-Auth-Key", CfConfigReader.readConfig("CF_API_KEY")));

            String resp = HttpUtil.sendPost(formatUrl, headers, bodyResult.body);
            JSONObject jsonResp = JSONObject.parseObject(resp);
            Boolean success = jsonResp.getBoolean("success");

            if (success) {
                count++;
                System.out.println("[" + CfConfigReader.readConfig("PREFERRED_DOMAIN") + "] " + ip
                        + " DNS添加成功 >> " + bodyResult.ipInfo);
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
                System.out.println("[" + CfConfigReader.readConfig("PREFERRED_DOMAIN") + "] " + ip
                        + " DNS添加失败[" + sb + "] >> " + bodyResult.ipInfo);
            }
            System.out.println("---------------------------------------------------");
        }
        return count;
    }

    /**
     * 构建 新增(POST)/修改(PUT) 请求体
     *
     * @param ip ip
     * @return BodyResult
     * @throws Exception Exception
     */
    private static BodyResult getBodyResult(String ip) throws Exception {
        String ipInfo;
        try {
            String ipApiUrl = "http://ip-api.com/json/%s";
            String ipApiFormatUrl = String.format(ipApiUrl, ip);
            String listResp = HttpUtil.sendGet(ipApiFormatUrl, null);
            JSONObject ipInfoJson = JSONObject.parseObject(listResp);
            String country = ipInfoJson.getString("country");
            String city = ipInfoJson.getString("city");
            String org = ipInfoJson.getString("org");
            ipInfo = "IP归属地：[" + country + " - " + city + "] | IP服务商：[" + org + "]";
        } catch (Exception e) {
            ipInfo = "IP信息查询失败";
        }

        // "{\n  \"content\": \"198.51.100.4\",\n  \"name\": \"example.com\",\n  \"proxied\": false,\n  \"type\": \"A\",\n  \"comment\": \"Domain verification record\",\n  \"tags\": [\n    \"owner:dns-team\"\n  ],\n  \"ttl\": 3600\n}"
        JSONObject bodyJson = new JSONObject();
        // 优选ip
        bodyJson.put("content", ip);
        // 二级域名
        bodyJson.put("name", CfConfigReader.readConfig("PREFERRED_DOMAIN"));
        // 是否代理
        bodyJson.put("proxied", false);
        // 类型
        bodyJson.put("type", "A");
        // Zone ID
        bodyJson.put("zone_id", CfConfigReader.readConfig("CF_ZONE_ID"));
        // 一级域名
        bodyJson.put("zone_name", CfConfigReader.readConfig("YOUR_DOMAIN"));
        // 描述/备注
        bodyJson.put("comment", StringUtils.substring(ipInfo, 0, 100));
        String body = bodyJson.toJSONString();
        return new BodyResult(ipInfo, body);
    }

    private static class BodyResult {
        public final String ipInfo;
        public final String body;

        public BodyResult(String ipInfo, String body) {
            this.ipInfo = ipInfo;
            this.body = body;
        }
    }

    /**
     * 下载优选IP池 zip文件
     *
     * @param zipPath 下载路径
     * @return boolean
     */
    public static boolean downloadIpPool(String zipPath) {
        // 先删除
        FileUtils.deleteQuietly(new File(zipPath));
        try {
            System.out.println("开始下载优选IP池文件...");
            long start = System.currentTimeMillis();
            FileUtil.downloadFromUrl(CfConfigReader.readConfig("PREFERRED_IP_POOL_URL"), zipPath);
            long end = System.currentTimeMillis();
            System.out.println("优选IP池文件下载完成，耗时: " + DateUtil.millisecondsFormat(end - start) + "\n");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("优选IP池文件下载失败 >> " + e.getMessage());
            return true;
        }
        return false;
    }

    /**
     * 解压优选IP池 zip文件
     *
     * @param zipPath 压缩文件路径
     * @param ipPath  解压路径
     * @return boolean
     */
    public static boolean unzipIpPool(String zipPath, String ipPath) throws IOException {
        // 先删除
        FileUtils.deleteDirectory(new File(ipPath));
        try {
            System.out.println("开始解压优选IP池文件...");
            long start = System.currentTimeMillis();
            FileUtil.unzip(zipPath, ipPath);
            long end = System.currentTimeMillis();
            System.out.println("优选IP池文件解压完成，耗时: " + DateUtil.millisecondsFormat(end - start) + "\n");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("优选IP池文件解压失败 >> " + e.getMessage());
            return true;
        }
        return false;
    }

    /**
     * 输出新的优选IP池 txt文件
     *
     * @param existedIpList 已经存在的DNS记录
     * @param ipPath        优选IP池txt文件路径
     * @return boolean
     */
    public static boolean outputIpPoolTxt(List<String> existedIpList, String ipPath) {
        List<String> fileList = new ArrayList<>();
        List<String> ipFileList = FileUtil.listFiles(new File(ipPath), fileList);
        List<String> ipList = new ArrayList<>();
        for (String ip : ipFileList) {
            String[] split = ip.split("\\\\");
            String fileName = split[split.length - 1];
            // 只获取80/443端口的ip文件
            if (fileName.contains("-80.txt") || fileName.contains("-443.txt")) {
                List<String> list = FileUtil.readLocalFileByLines(ip);
                for (String aip : list) {
                    boolean isIp = RegexUtil.isIp(aip);
                    if (isIp) {
                        ipList.add(aip);
                    }
                }
            }
        }

        // 删除已存在的记录
        ipList.removeAll(existedIpList);

        try {

            String projectDirectory = System.getProperty("user.dir") + "\\preferIp\\";
            // 使用String.join方法将List<String>转换为以换行符分隔的字符串
            String result = String.join(System.lineSeparator(), ipList);
            String ipTxtPath = projectDirectory + "ip.txt";
            // 先删除
            FileUtils.deleteQuietly(new File(ipTxtPath));

            System.out.println("开始输出ip.txt...");
            long start = System.currentTimeMillis();
            FileUtil.writeUsingFileWriter(result, ipTxtPath);
            long end = System.currentTimeMillis();
            System.out.println("ip.txt输出完成，耗时: " + DateUtil.millisecondsFormat(end - start) + "\n");
            System.out.println("共 " + ipList.size() + " 个IP参与优选\n");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("优选IP池文件输出到ip.txt失败 >> " + e.getMessage());
            return true;
        }
        return false;
    }

    public static void runPreferredIp(CountDownLatch latch, int i) {
        System.out.println("开始执行`CloudflareST.exe`优选IP...");

        try {
            long startExe = System.currentTimeMillis();
            DefaultExecutor executor = new DefaultExecutor();
            executor.setStreamHandler(new PumpStreamHandler(System.out, System.err));
            // 设置超时时间
            String preferredIpTimeout = CfConfigReader.readConfig("PREFERRED_IP_TIMEOUT");
            long aLong = Long.parseLong(preferredIpTimeout);
            ExecuteWatchdog watchdog = new ExecuteWatchdog(aLong);
            executor.setWatchdog(watchdog);

            String projectDirectory = System.getProperty("user.dir") + "\\preferIp\\";
            String PREFERRED_IP_CMD = projectDirectory + "CloudflareST.exe" +
                    " -f '" + projectDirectory + "ip.txt' " +
                    " -o '" + projectDirectory + "result.csv' " +
                    " -tp " + CfConfigReader.readConfig("PREFERRED_IP_PORT") +
                    " -url " + CfConfigReader.readConfig("PREFERRED_IP_URL") +
                    " -sl " + CfConfigReader.readConfig("PREFERRED_IP_DOWN_SPEED") +
                    " -tl " + CfConfigReader.readConfig("PREFERRED_IP_DELAY") +
                    " -dn " + CfConfigReader.readConfig("PREFERRED_IP_COUNT");
            executor.execute(CommandLine.parse(PREFERRED_IP_CMD), new ExecuteResultHandler() {
                @Override
                public void onProcessComplete(int exitValue) {
                    long endExe = System.currentTimeMillis();
                    System.out.println("\n\n优选IP执行完成，耗时: " + DateUtil.millisecondsFormat(endExe - startExe) + "\n");

                    //稍等3秒，生成文件
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        //一般没啥问题
                    }

                    System.out.println("\n开始向优选域名中添加优选IP...");
                    long start = System.currentTimeMillis();
                    int count;
                    try {
                        count = cfDnsAdd();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    long end = System.currentTimeMillis();
                    latch.countDown();

                    if (count == 0) {
                        return;
                    }

                    System.out.println("优选IP已成功添加至优选域名中，耗时: " + DateUtil.millisecondsFormat(end - start) + "\n");
                    try {
                        System.out.println("当前优选域名[" + CfConfigReader.readConfig("PREFERRED_DOMAIN") + "] 可用优选IP " + (i + count) + " 个");
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }

                @Override
                public void onProcessFailed(ExecuteException e) {
                    e.printStackTrace();
                    System.out.println("优选IP执行失败：" + e.getMessage());
                    latch.countDown();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            latch.countDown();
        }
    }

    /**
     * 获取优选好的IP列表
     *
     * @param list result.csv的所有行数据
     * @return IP列表
     */
    public static List<String> getPreferredIpList(List<String> list) {
        List<String> ipList = new ArrayList<>();
        for (String text : list) {
            String[] split = text.split(",");
            int length = split.length;
            String downSpeed = split[length - 1];
            try {
                BigDecimal downBd = new BigDecimal(downSpeed);
                // 只添加下载速度大于3mb/s的IP
                if (downBd.compareTo(BigDecimal.valueOf(3)) > 0) {
                    String ip = split[0];
                    if (ipList.contains(ip)) {
                        continue;
                    }
                    ipList.add(ip);
                }
            } catch (Exception e) {
                // 一般是第一行转换报错，不用管
            }
        }
        return ipList;
    }

}
