package com.github.caijh.graphql.provider.redis;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import com.github.caijh.framework.data.redis.support.Redis;
import com.github.caijh.graphql.provider.BaseProviderRegistry;
import com.github.caijh.graphql.provider.GraphqlProviderException;
import com.github.caijh.graphql.provider.dto.TpDocGraphqlProviderServiceInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * @author xuwenzhen
 * @date 2019/9/8
 */
@Component
public class RedisBaseProviderRegistry extends BaseProviderRegistry {

    private static final Logger logger = LoggerFactory.getLogger(RedisBaseProviderRegistry.class);
    private static final String STR_CLN = ":";
    private static final String STR_APPS = "apps";
    private static final String APIS = "apis";
    private static final String STR_SUB = "sub";

    /**
     * Graphql Redis注册器路径
     *
     * @demo graphql:apps
     */
    @Value("${graphql.registry.redis:graphql}")
    private String redisRegistryPath;

    /**
     * api接口文档名称
     *
     * @demo api.json
     */
    @Value("${graphql.api.doc:api.json}")
    protected String apiDocName;

    @Inject
    private Redis redis;

    @Autowired
    private RedisMessagePublisher redisMessagePublisher;

    @PostConstruct
    public void registryService() {
        this.validate();

        TpDocGraphqlProviderServiceInfo provider = this.getTpDocGraphqlProviderServiceInfo();
        if (provider == null) {
            return;
        }

        //尝试读取接口文档
        String apiDoc = this.readResourceString(this.apiDocName);
        if (StringUtils.isEmpty(apiDoc)) {
            throw new GraphqlProviderException("无法获取接口文档：" + this.apiDocName);
        }

        final String appId = provider.getAppId();
        if (StringUtils.isEmpty(appId)) {
            throw new GraphqlProviderException("无法注册成graphql provider,appId为空，请检查duo-doc配置！");
        }
        //注册接口文档
        String key = this.redisRegistryPath + STR_CLN + APIS + STR_CLN + appId;
        this.redis.getRedisTemplate().execute(new SessionCallback() {
            /**
             * Executes all the given operations inside the same session.
             *
             * @param operations Redis operations
             * @return return value
             */
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                operations.multi();
                HashOperations hashOperations = operations.opsForHash();
                hashOperations.put(key, provider.getVcsId(), apiDoc);
                hashOperations
                        .put(RedisBaseProviderRegistry.this.redisRegistryPath + RedisBaseProviderRegistry.STR_CLN + RedisBaseProviderRegistry.STR_APPS, appId, provider
                                .toString());
                return operations.exec();
            }
        });
        this.redis.getRedisTemplate().opsForHash().put(key, provider.getVcsId(), apiDoc);
        logger.info("注册服务文档：{}", key);

        //上报
        this.registerProvider(provider);
    }

    /**
     * 注册服务
     *
     * @param provider 服务信息
     */
    @Override
    protected void registerProvider(TpDocGraphqlProviderServiceInfo provider) {
        this.redisMessagePublisher.publish(this.getAppsPath(), provider.toString());
    }

    private String getAppsPath() {
        return this.redisRegistryPath + STR_CLN + STR_APPS + STR_CLN + STR_SUB;
    }

}
