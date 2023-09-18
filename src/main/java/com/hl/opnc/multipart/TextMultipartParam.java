package com.hl.opnc.multipart;

/**
 * @author kjqg-cn
 * @date 2022-08-17
 */
public class TextMultipartParam implements MultipartParam {

    /**
     * 参数名
     */
    private String name;

    /**
     * 参数值
     */
    private String value;

    public TextMultipartParam() {
    }

    public TextMultipartParam(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

}
