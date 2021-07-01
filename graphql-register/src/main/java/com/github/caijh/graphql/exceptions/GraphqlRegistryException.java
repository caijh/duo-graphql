package com.github.caijh.graphql.exceptions;

/**
 * GraphQL Registry Exception
 *
 * @author xuwenzhen
 * @date 2019/4/2
 */
public class GraphqlRegistryException extends RuntimeException {

    private static final long serialVersionUID = 5452165646193487390L;

    public GraphqlRegistryException(String message) {
        super(message);
    }

    public GraphqlRegistryException(String message, Throwable e) {
        super(message, e);
    }

}
