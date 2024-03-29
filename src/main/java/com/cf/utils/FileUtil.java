package com.cf.utils;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 读取文本文件工具类
 *
 * @author kjqg-cn
 * @date 2022-05-18
 */
public class FileUtil {

    private static final Logger logger = LoggerFactory.getLogger(FileUtil.class);

    public static final int BUFFER_SIZE = 4096;

    /**
     * 读取本地文件
     * 以行为单位读取文件，常用于读面向行的格式化文件
     */
    public static List<String> readLocalFileByLines(String fileName) {
        List<String> lineList = new ArrayList<>();
        BufferedReader br = null;
        try {
            InputStream is = new FileInputStream(fileName);
            Reader reader = new InputStreamReader(is);
            br = new BufferedReader(reader);
            String tempString;
            // 一次读入一行，直到读入null为文件结束
            while ((tempString = br.readLine()) != null) {
                lineList.add(tempString);
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
        return lineList;
    }

    /**
     * 当写操作次数较少时使用 FileWriter
     *
     * @param data 要写入的数据
     * @param path 要写入的路径
     */
    public static void writeUsingFileWriter(String data, String path) {
        File file = new File(path);
        FileWriter fr = null;
        try {
            fr = new FileWriter(file);
            fr.write(data);
        } catch (IOException e) {
            logger.error("error：", e);
        } finally {
            //close resources
            IOUtils.closeQuietly(fr);
        }
    }

    /**
     * 通过url下载文件到指定目录
     *
     * @param url          文件url地址
     * @param fileFullPath 下载后的文件路径【绝对路径+文件名】
     */
    public static void downloadFromUrl(String url, String fileFullPath) throws Exception {
        URL resourceUrl = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) resourceUrl.openConnection();

        // 设置User-Agent头部，模拟浏览器请求
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/99.0.9999.999 Safari/537.36");

        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            InputStream inputStream = connection.getInputStream();

            // 创建文件输出流，保存下载的文件
            FileOutputStream outputStream = new FileOutputStream(fileFullPath);

            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            inputStream.close();
            outputStream.close();
            logger.info("下载完成");
        } else {
            logger.error("文件下载失败，响应码：" + responseCode);
        }
    }

    /**
     * zip解压
     *
     * @param inputFile   待解压文件全路径
     * @param destDirPath 解压路径
     */
    public static void unzip(String inputFile, String destDirPath) throws Exception {
        //获取当前压缩文件
        File srcFile = new File(inputFile);
        // 判断源文件是否存在
        if (!srcFile.exists()) {
            throw new Exception(srcFile.getPath() + " file not exists");
        }
        //开始解压
        //构建解压输入流
        ZipInputStream zIn = new ZipInputStream(new FileInputStream(srcFile));
        ZipEntry entry;
        File file;
        while ((entry = zIn.getNextEntry()) != null) {
            if (!entry.isDirectory()) {
                file = new File(destDirPath, entry.getName());
                if (!file.exists()) {
                    //创建此文件的上级目录
                    new File(file.getParent()).mkdirs();
                }
                OutputStream out = new FileOutputStream(file);
                BufferedOutputStream bos = new BufferedOutputStream(out);
                int len;
                byte[] buf = new byte[BUFFER_SIZE];
                while ((len = zIn.read(buf)) != -1) {
                    bos.write(buf, 0, len);
                }
                // 关流顺序，先打开的后关闭
                bos.close();
                out.close();
            }
        }
    }

    /**
     * 读取目录下的所有文件
     *
     * @param directory 文件目录对象
     */
    public static List<String> listFiles(File directory, List<String> list) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    // 如果是子目录，递归调用listFiles方法
                    listFiles(file, list);
                } else {
                    list.add(file.getAbsolutePath());
                }
            }
        }
        return list;
    }

}