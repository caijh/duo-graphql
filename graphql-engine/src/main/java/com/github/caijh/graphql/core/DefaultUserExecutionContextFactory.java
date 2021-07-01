package com.github.caijh.graphql.core;

import java.util.Set;

import com.google.common.base.Splitter;
import com.google.common.collect.Sets;
import graphql.spring.web.servlet.GraphQLInvocationData;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.WebRequest;

/**
 * @author xuwenzhen
 * @date 2019/9/29
 */
public class DefaultUserExecutionContextFactory implements UserExecutionContextFactory<UserExecutionContext> {

    private final Set<String> headerNames;

    private final String graphqlQueryHeaderNames;

    public DefaultUserExecutionContextFactory(String graphqlQueryHeaderNames) {
        this.graphqlQueryHeaderNames = graphqlQueryHeaderNames;
        this.headerNames = Sets.newHashSet();
        if (!StringUtils.isEmpty(graphqlQueryHeaderNames)) {
            Splitter.on(GraphqlConsts.STR_COMMA)
                    .omitEmptyStrings()
                    .trimResults()
                    .split(graphqlQueryHeaderNames)
                    .forEach(this.headerNames::add);
        }
    }

    /**
     * 创建一个工厂
     *
     * @param invocationData GraphQL调用数据
     * @param request        http请求
     * @return
     */
    @Override
    public UserExecutionContext get(GraphQLInvocationData invocationData, WebRequest request) {
        UserExecutionContext userExecutionContext = new UserExecutionContext();
        userExecutionContext.setHeaderNames(this.headerNames);
        return userExecutionContext;
    }

    public String getGraphqlQueryHeaderNames() {
        return this.graphqlQueryHeaderNames;
    }

}
