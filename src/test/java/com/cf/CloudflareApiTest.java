package com.cf;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.cf.utils.FileUtil;
import com.cf.utils.RegexUtil;
import com.hl.opnc.invoke.HttpUtil;
import org.apache.commons.exec.*;
import org.apache.commons.io.FileUtils;
import org.apache.http.Header;
import org.apache.http.message.BasicHeader;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * Cloudflare API
 */
public class CloudflareApiTest {

    /**
     * 注册cloudflare的邮箱
     */
    private static final String CF_EMAIL = "替换这里";

    /**
     * 登录cloudflare，点击你将要优选的域名
     * Overview > [右下角]API > Zone ID
     */
    private static final String CF_API_KEY = "替换这里";

    /**
     * Overview > [右下角]API > Get your API token
     */
    private static final String CF_ZONE_ID = "替换这里";

    /**
     * 托管到cf的域名
     * 如果是一级域名，参数示例【domain.com】；
     * 如果是二级域名，参数示例【domain.eu.org】
     */
    private static final String YOUR_DOMAIN = "替换这里";

    /**
     * 优选域名
     * 如果托管域名是一级域名，优选域名示例【fast.domain.com】；
     * 如果是二级域名，优选域名示例【fast.domain.eu.org】
     */
    private static final String PREFERRED_DOMAIN = "替换这里";

    /**
     * CloudflareST.exe 所在路径【仅exe所在目录，不含`CloudflareST.exe`文件名】
     * 举例：D:\优选IP
     * "\"粘贴到代码的字符串中，会自动变成"\\"，是正常现象，不要手动修改成"\"
     * 请确保目录地址能让程序有权限访问
     */
    private static final String CF_ST_EXE_PATH = "替换这里";

    /**
     * IP池zip包下载地址
     */
    private static final String PREFERRED_IP_POOL_URL = "https://zip.baipiao.eu.org";

    /**
     * CF优选域名里的IP的ping超时时间，单位 毫秒
     * ping超时时间不建议设置太小，500-2000之间即可，有时候实际ping几十毫秒，但在程序里可能会几百毫秒
     */
    private static final int PINT_TIMEOUT = 2000;

    /**
     * 优选IP超时时间，单位 毫秒
     * 根据实际优选参数而定，从2000个IP中优选10个下载速率为10mb/s，延时为200ms的IP，一般60分钟内能完成【可以根据实际情况进行调整】
     */
    private static final int PREFERRED_IP_TIMEOUT = 60 * 60 * 1000;

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
     * 优选IP的最大延迟，单位 毫秒
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

    /**
     * 执行这个 只删除优选域名里的失效IP
     */
    @Test
    public void onlyDeleteInvalidIp() throws Exception {
        cfDnsList();
    }

    /**
     * 执行这个 只把优选好的IP添加到优选域名中 【在已经提前执行了 CloudflareST.exe 且生成了 result.csv 文件的情况下】
     */
    @Test
    public void onlyAddPreferIp() throws Exception {
        System.out.println("开始向优选域名中添加优选IP...");
        long start = System.currentTimeMillis();
        int count = cfDnsAdd();
        long end = System.currentTimeMillis();

        System.out.println("已成功添加[" + count + "]个优选IP至优选域名中，耗时: " + millisecondsFormat(end - start) + "\n");
    }

    /**
     * 执行这个 从执行`CloudflareST.exe`开始，直至结束 【适用于优选超时异常的情况】
     */
    @Test
    public void onlyExecuteExeAndAddPreferIp() throws Exception {
        System.out.println("--------------------- 程序开始执行 ---------------------");

        List<String> existedIpList = cfDnsList();

        if (existedIpList == null) {
            return;
        }

        if (existedIpList.size() > 20) {
            System.err.println("优选IP已经足够多了，不需要进行优选了哦~");
            return;
        }

        // 初始化计数为1
        CountDownLatch latch = new CountDownLatch(1);
        runPreferredIp(latch, existedIpList.size());
        try {
            // 等待计数减少到0
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("--------------------- 程序执行结束 ---------------------");
    }

    /**
     * 执行这个 从解压IP池开始优选 【适用于自动下载IP池失败或长时间无响应，且已经手动下载了IP池zip文件的情况】
     */
    @Test
    public void autoAddPreferIp() throws Exception {
        List<String> existedIpList = cfDnsList();

        if (existedIpList == null) {
            return;
        }

        if (existedIpList.size() > 20) {
            System.err.println("优选IP已经足够多了，不需要进行优选了哦~");
            return;
        }

        String zipPath = CF_ST_EXE_PATH + "\\txt.zip";
        String ipPath = CF_ST_EXE_PATH + "\\ip";
        if (unzipIpPool(zipPath, ipPath)) return;

        if (outputIpPoolTxt(existedIpList, ipPath)) return;

        // 初始化计数为1
        CountDownLatch latch = new CountDownLatch(1);
        runPreferredIp(latch, existedIpList.size());
        try {
            // 等待计数减少到0
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("--------------------- 程序执行结束 ---------------------");
    }

    /**
     * 执行这个 全自动优选
     */
    @Test
    public void autoPreferredDomain() throws Exception {
        System.out.println("--------------------- 程序开始执行 ---------------------");

        List<String> existedIpList = cfDnsList();
        if (existedIpList == null) {
            return;
        }

        if (existedIpList.size() > 20) {
            System.err.println("优选IP已经足够多了，不需要进行优选了哦~");
            return;
        }

        String zipPath = CF_ST_EXE_PATH + "\\txt.zip";
        if (downloadIpPool(zipPath)) return;

        String ipPath = CF_ST_EXE_PATH + "\\ip";
        if (unzipIpPool(zipPath, ipPath)) return;

        if (outputIpPoolTxt(existedIpList, ipPath)) return;

        // 初始化计数为1
        CountDownLatch latch = new CountDownLatch(1);
        runPreferredIp(latch, existedIpList.size());
        try {
            // 等待计数减少到0
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("--------------------- 程序执行结束 ---------------------");
    }

    /**
     * cf-api 查询dns
     *
     * @return 可用dns数量
     */
    private List<String> cfDnsList() {
        long start = System.currentTimeMillis();
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
            return null;
        }

        JSONArray resultArr = jsonResp.getJSONArray("result");
        if (resultArr.size() == 0) {
            System.out.println("优选域名[" + PREFERRED_DOMAIN + "]下未查询到DNS记录");
            return Collections.emptyList();
        }

        List<String> existedIpList = new ArrayList<>();
        System.out.println("优选域名[" + PREFERRED_DOMAIN + "]下已存在 " + resultArr.size() + " 个优选IP");
        System.out.println("开始检测IP是否可用...");
        for (Object obj : resultArr) {
            JSONObject oneResult = JSONObject.parseObject(obj.toString());
            String ip = oneResult.getString("content");
            try {
                InetAddress inetAddress = InetAddress.getByName(ip);
                boolean isReachable = inetAddress.isReachable(PINT_TIMEOUT);

                if (isReachable) {
                    System.out.println(ip + " 可以ping通，继续保留 √");
                    existedIpList.add(ip);
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
        System.out.println("IP检测完成，耗时: " + millisecondsFormat(end - start));
        System.out.println("优选域名[" + PREFERRED_DOMAIN + "]下剩余可用优选IP " + existedIpList.size() + " 个\n");
        return existedIpList;
    }

    /**
     * cf-api 删除dns
     *
     * @param dnsId dns唯一标识
     * @param ip    删除的IP
     */
    public static void cfDnsDelete(String dnsId, String ip) {
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
            System.out.println("[" + PREFERRED_DOMAIN + "] >> " + ip + " DNS删除失败 :( ");
        } else {
            System.out.println("[" + PREFERRED_DOMAIN + "] >> " + ip + " DNS删除成功 :) ");
        }
    }

    /**
     * cf-api 新增dns
     */
    public static int cfDnsAdd() {
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
            return 0;
        }

        // 新增DNS
        // https://api.cloudflare.com/client/v4/zones/{zone_identifier}/dns_records
        String addUrl = "https://api.cloudflare.com/client/v4/zones/%s/dns_records";
        String formatUrl = String.format(addUrl, CF_ZONE_ID);

        int count = 0;
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

            if (success) {
                count++;
                System.out.println("[" + PREFERRED_DOMAIN + "] " + ip
                        + " DNS添加成功 >> " + ipInfo);
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
                System.out.println("[" + PREFERRED_DOMAIN + "] " + ip
                        + " DNS添加失败[" + sb + "] >> " + ipInfo);
            }
            System.out.println("---------------------------------------------------");
        }
        return count;
    }

    /**
     * 下载优选IP池 zip文件
     *
     * @param zipPath 下载路径
     * @return boolean
     */
    private boolean downloadIpPool(String zipPath) {
        // 先删除
        FileUtils.deleteQuietly(new File(zipPath));
        try {
            System.out.println("开始下载优选IP池文件...");
            long start = System.currentTimeMillis();
            FileUtil.downloadFromUrl(PREFERRED_IP_POOL_URL, zipPath);
            long end = System.currentTimeMillis();
            System.out.println("优选IP池文件下载完成，耗时: " + millisecondsFormat(end - start) + "\n");
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
    private boolean unzipIpPool(String zipPath, String ipPath) throws IOException {
        // 先删除
        FileUtils.deleteDirectory(new File(ipPath));
        try {
            System.out.println("开始解压优选IP池文件...");
            long start = System.currentTimeMillis();
            FileUtil.unzip(zipPath, ipPath);
            long end = System.currentTimeMillis();
            System.out.println("优选IP池文件解压完成，耗时: " + millisecondsFormat(end - start) + "\n");
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
    private boolean outputIpPoolTxt(List<String> existedIpList, String ipPath) {
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
            // 使用String.join方法将List<String>转换为以换行符分隔的字符串
            String result = String.join(System.lineSeparator(), ipList);
            String ipTxtPath = CF_ST_EXE_PATH + "\\ip.txt";
            // 先删除
            FileUtils.deleteQuietly(new File(ipTxtPath));

            System.out.println("开始输出ip.txt...");
            long start = System.currentTimeMillis();
            FileUtil.writeUsingFileWriter(result, ipTxtPath);
            long end = System.currentTimeMillis();
            System.out.println("ip.txt输出完成，耗时: " + millisecondsFormat(end - start) + "\n");
            System.out.println("共 " + ipList.size() + " 个IP参与优选\n");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("优选IP池文件输出到ip.txt失败 >> " + e.getMessage());
            return true;
        }
        return false;
    }

    private static void runPreferredIp(CountDownLatch latch, int i) {
        System.out.println("开始执行`CloudflareST.exe`优选IP...");

        try {
            long startExe = System.currentTimeMillis();
            DefaultExecutor executor = new DefaultExecutor();
            executor.setStreamHandler(new PumpStreamHandler(System.out, System.err));
            // 设置超时时间
            ExecuteWatchdog watchdog = new ExecuteWatchdog(PREFERRED_IP_TIMEOUT);
            executor.setWatchdog(watchdog);

            executor.execute(CommandLine.parse(PREFERRED_IP_CMD), new ExecuteResultHandler() {
                @Override
                public void onProcessComplete(int exitValue) {
                    long endExe = System.currentTimeMillis();
                    System.out.println("\n\n优选IP执行完成，耗时: " + millisecondsFormat(endExe - startExe) + "\n");

                    //稍等3秒，生成文件
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        //一般没啥问题
                    }

                    System.out.println("\n开始向优选域名中添加优选IP...");
                    long start = System.currentTimeMillis();
                    int count = cfDnsAdd();
                    long end = System.currentTimeMillis();
                    latch.countDown();

                    if (count == 0) {
                        return;
                    }

                    System.out.println("优选IP已成功添加至优选域名中，耗时: " + millisecondsFormat(end - start) + "\n");
                    System.out.println("当前优选域名[" + PREFERRED_DOMAIN + "] 可用优选IP " + (i + count) + " 个");
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
