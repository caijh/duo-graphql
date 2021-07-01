package com.github.caijh.graphql.core.exceptions;

/**
 * @author xuwenzhen
 * @date 2019/10/14
 */
public class BaseServiceException extends RuntimeException {

    private static final long serialVersionUID = 6595384629122865514L;
    /**
     * 错误代码
     */
    private int code = 500;

    public BaseServiceException(int code, String msg) {
        super(msg);
        this.code = code;
    }

    public int getCode() {
        return this.code;
    }

    public void setCode(int code) {
        this.code = code;
    }

}
