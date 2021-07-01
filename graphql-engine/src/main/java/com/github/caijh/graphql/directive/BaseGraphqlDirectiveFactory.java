package com.github.caijh.graphql.directive;

import java.util.Map;

import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLDirective;

/**
 * Base directive
 *
 * @author xuwenzhen
 * @date 2019/7/13
 */
public abstract class BaseGraphqlDirectiveFactory {

    private final GraphQLDirective graphQLDirective;

    public BaseGraphqlDirectiveFactory() {
        this.graphQLDirective = this.buildGraphQLDirective();
    }

    public GraphQLDirective getGraphQLDirective() {
        return this.graphQLDirective;
    }

    /**
     * 创建GraphQLDirective
     *
     * @return
     */
    protected abstract GraphQLDirective buildGraphQLDirective();

    /**
     * 处理
     *
     * @param environment 当前执行的上下文环境
     * @param data        当前值
     * @param args        参数
     * @return 返回处理后的结果
     */
    public Object process(DataFetchingEnvironment environment, Object data, Map<String, Object> args) {
        return data;
    }

}
