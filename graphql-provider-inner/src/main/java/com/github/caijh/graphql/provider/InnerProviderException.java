package com.github.caijh.graphql.provider;

/**
 * @author xuwenzhen
 * @date 2019/5/22
 */
public class InnerProviderException extends RuntimeException {

    private static final long serialVersionUID = -504198829738110291L;

    public InnerProviderException(String message, Exception e) {
        super(message, e);
    }

}
