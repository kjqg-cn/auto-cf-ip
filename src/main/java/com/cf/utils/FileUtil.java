package com.cf.utils;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
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
     * 在处理原始数据时使用 Streams
     * Use Streams when you are dealing with raw data
     *
     * @param data 要写入的数据
     * @param path 要写入的路径
     */
    public static void writeUsingOutputStream(String data, String path) {
        OutputStream os = null;
        try {
            os = new FileOutputStream(path);
            os.write(data.getBytes(), 0, data.length());
        } catch (IOException e) {
            logger.error("error：", e);
        } finally {
            IOUtils.closeQuietly(os);
        }
    }

    /**
     * 使用Java 1.7的Files类写文件，内部使用OutputStream
     * Use Files class from Java 1.7 to write files, internally uses OutputStream
     *
     * @param data 要写入的数据
     * @param path 要写入的路径
     */
    public static void writeUsingFiles(String data, String path) {
        try {
            Files.write(Paths.get(path), data.getBytes());
        } catch (IOException e) {
            logger.error("error：", e);
        }
    }

    /**
     * 当写操作次数较多时使用BufferedWriter
     * 它使用内部缓冲区来减少真正的IO操作并节省时间
     * Use BufferedWriter when number of write operations are more
     * It uses internal buffer to reduce real IO operations and saves time
     *
     * @param data      要写入的数据
     * @param path      要写入的路径
     * @param noOfLines
     */
    private static void writeUsingBufferedWriter(String data, String path, int noOfLines) {
        File file = new File(path);
        FileWriter fr = null;
        BufferedWriter br = null;
        String dataWithNewLine = data + System.getProperty("line.separator");
        try {
            fr = new FileWriter(file);
            br = new BufferedWriter(fr);
            for (int i = noOfLines; i > 0; i--) {
                br.write(dataWithNewLine);
            }
        } catch (IOException e) {
            logger.error("error：", e);
        } finally {
            IOUtils.closeQuietly(br);
            IOUtils.closeQuietly(fr);
        }
    }

    /**
     * 当写操作次数较少时使用 FileWriter
     * Use FileWriter when number of write operations are less
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
        URL httpUrl = new URL(url);
        File f = new File(fileFullPath);
        FileUtils.copyURLToFile(httpUrl, f);
    }

    public static final int BUFFER_SIZE = 4096;

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