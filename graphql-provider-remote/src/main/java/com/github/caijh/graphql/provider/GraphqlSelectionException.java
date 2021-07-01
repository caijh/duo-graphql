package com.github.caijh.graphql.provider;

/**
 * GraphQL Selection Exception
 *
 * @author xuwenzhen
 * @since 2019/6/29
 */
public class GraphqlSelectionException extends RuntimeException {

    private static final long serialVersionUID = -8383352260792972153L;

    public GraphqlSelectionException(String msg) {
        super(msg);
    }

}
