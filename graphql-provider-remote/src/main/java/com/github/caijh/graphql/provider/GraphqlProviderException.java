package com.github.caijh.graphql.provider;

/**
 * GraphQL Provider Exception
 *
 * @author xuwenzhen
 * @date 2019/6/29
 */
public class GraphqlProviderException extends RuntimeException {
    public GraphqlProviderException(String msg) {
        super(msg);
    }

    public GraphqlProviderException(String msg, Throwable e) {
        super(msg, e);
    }
}
