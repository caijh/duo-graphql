package com.github.caijh.graphql.core.config;

import com.github.caijh.graphql.core.DefaultUserExecutionContextFactory;
import com.github.caijh.graphql.core.UserExecutionContext;
import com.github.caijh.graphql.core.UserExecutionContextFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * @author xuwenzhen
 * @date 2019/7/17
 */
@Configuration
@ConditionalOnMissingBean(UserExecutionContextFactory.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class UserExecutionContextFactoryConfigure {

    /**
     * 需要透传的请求头，多个可以使用半角逗号分隔
     */
    @Value("${graphql.query.headers:}")
    private String graphqlQueryHeaderNames;

    @Bean
    public UserExecutionContextFactory<UserExecutionContext> getDefaultUserExecutionContextFactory() {
        return new DefaultUserExecutionContextFactory(this.graphqlQueryHeaderNames);
    }

}
