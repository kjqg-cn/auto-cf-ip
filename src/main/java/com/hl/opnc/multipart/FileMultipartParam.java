package com.hl.opnc.multipart;

import java.io.File;

/**
 * @author kjqg-cn
 * @date 2022-08-17
 */
public class FileMultipartParam implements MultipartParam {

    /**
     * 参数名
     */
    private String name;

    /**
     * 文件
     */
    private File file;

    /**
     * 文件名
     */
    private String filename;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

}
