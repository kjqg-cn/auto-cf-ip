package com.cf;

import com.cf.config.CfConfigReader;
import com.cf.utils.CfUtil;
import com.cf.utils.DateUtil;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * Cloudflare API
 */
public class CloudflareApiTest {

    /**
     * 执行这个 只删除优选域名里的失效IP
     */
    @Test
    public void onlyDeleteInvalidIp() throws Exception {
        CfUtil.cfDnsList();
    }

    /**
     * 执行这个 只把优选好的IP添加到优选域名中 【在已经提前执行了 CloudflareST.exe 且生成了 result.csv 文件的情况下】
     */
    @Test
    public void onlyAddPreferIp() throws Exception {
        System.out.println("开始向优选域名中添加优选IP...");
        long start = System.currentTimeMillis();
        int count = CfUtil.cfDnsAdd();
        long end = System.currentTimeMillis();

        System.out.println("已成功添加[" + count + "]个优选IP至优选域名中，耗时: " + DateUtil.millisecondsFormat(end - start) + "\n");
    }

    /**
     * 执行这个 从执行`CloudflareST.exe`开始，直至结束 【适用于优选超时异常的情况】
     */
    @Test
    public void onlyExecuteExeAndAddPreferIp() throws Exception {
        System.out.println("--------------------- 程序开始执行 ---------------------");

        List<String> existedIpList = CfUtil.cfDnsList();

        if (existedIpList == null) {
            return;
        }

        String count = CfConfigReader.readConfig("PREFERRED_DOMAIN_COUNT");
        if (existedIpList.size() > Integer.parseInt(count)) {
            System.err.println("优选IP已经足够多了，不需要进行优选了哦~");
            return;
        }

        // 初始化计数为1
        CountDownLatch latch = new CountDownLatch(1);
        CfUtil.runPreferredIp(latch, existedIpList.size());
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
        List<String> existedIpList = CfUtil.cfDnsList();

        if (existedIpList == null) {
            return;
        }

        String count = CfConfigReader.readConfig("PREFERRED_DOMAIN_COUNT");
        if (existedIpList.size() > Integer.parseInt(count)) {
            System.err.println("优选IP已经足够多了，不需要进行优选了哦~");
            return;
        }

        String projectDirectory = System.getProperty("user.dir") + "\\preferIp\\";
        String zipPath = projectDirectory + "txt.zip";
        String ipPath = projectDirectory + "ip";
        if (CfUtil.unzipIpPool(zipPath, ipPath)) return;

        if (CfUtil.outputIpPoolTxt(existedIpList, ipPath)) return;

        // 初始化计数为1
        CountDownLatch latch = new CountDownLatch(1);
        CfUtil.runPreferredIp(latch, existedIpList.size());
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

        List<String> existedIpList = CfUtil.cfDnsList();
        if (existedIpList == null) {
            return;
        }

        String count = CfConfigReader.readConfig("PREFERRED_DOMAIN_COUNT");
        if (existedIpList.size() > Integer.parseInt(count)) {
            System.err.println("优选IP已经足够多了，不需要进行优选了哦~");
            return;
        }

        String projectDirectory = System.getProperty("user.dir") + "\\preferIp\\";
        String zipPath = projectDirectory + "txt.zip";
        if (CfUtil.downloadIpPool(zipPath)) return;

        String ipPath = projectDirectory + "ip";
        if (CfUtil.unzipIpPool(zipPath, ipPath)) return;

        if (CfUtil.outputIpPoolTxt(existedIpList, ipPath)) return;

        // 初始化计数为1
        CountDownLatch latch = new CountDownLatch(1);
        CfUtil.runPreferredIp(latch, existedIpList.size());
        try {
            // 等待计数减少到0
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("--------------------- 程序执行结束 ---------------------");
    }

}
