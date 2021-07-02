package com.github.caijh.graphql.provider;

/**
 * GraphQL Provider Exception
 *
 * @author xuwenzhen
 * @since 2019/6/29
 */
public class GraphqlProviderException extends RuntimeException {

    private static final long serialVersionUID = -5961390361868940865L;

    public GraphqlProviderException(String msg) {
        super(msg);
    }

    public GraphqlProviderException(String msg, Throwable e) {
        super(msg, e);
    }

}
