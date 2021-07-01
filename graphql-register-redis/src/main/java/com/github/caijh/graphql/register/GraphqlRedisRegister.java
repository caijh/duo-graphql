package com.github.caijh.graphql.register;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.inject.Inject;

import com.github.caijh.framework.data.redis.support.Redis;
import com.github.caijh.graphql.provider.dto.TpDocGraphqlProviderServiceInfo;
import com.github.caijh.graphql.register.config.GraphqlRegisterConfigure;
import com.github.caijh.graphql.register.server.GraphqlEngineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

/**
 * GraphQL Register Redis 实现
 *
 * @author xuwenzhen
 * @since 2019/8/9
 */
@Service
public class GraphqlRedisRegister implements GraphqlRegister<TpDocGraphqlProviderServiceInfo> {

    private static final String PATH_SPLITTER = ":";
    private static final String APPS = "apps";
    private static final String SUB = "sub";

    @Inject
    private Redis redis;

    @Autowired
    private GraphqlRegisterConfigure graphQLRegisterConfigure;

    @Autowired
    private MessagePublisher messagePublisher;

    @Autowired
    private JsonService jsonService;

    @Autowired
    private GraphqlEngineService<TpDocGraphqlProviderServiceInfo> graphqlEngineService;

    @PostConstruct
    public void init() {
        //拉取所有服务
        Map<Object, Object> providerMap = this.redis.getRedisTemplate().opsForHash().entries(this.getAppRootPath());
        if (CollectionUtils.isEmpty(providerMap)) {
            return;
        }
        List<TpDocGraphqlProviderServiceInfo> providerList = providerMap.entrySet()
                                                                        .stream()
                                                                        .map(entry -> {
                                                                            String valueStr = (String) entry.getValue();
                                                                            return this.jsonService.toObject(valueStr, TpDocGraphqlProviderServiceInfo.class);
                                                                        })
                                                                        .collect(Collectors.toList());
        this.emitProviderList(providerList);
    }

    /**
     * 向注册中心注册供应端服务的信息
     *
     * @param provider 供应端服务的信息数据
     */
    @Override
    public void register(TpDocGraphqlProviderServiceInfo provider) {
        String key = this.getAppRootPath();
        String appId = provider.getAppId();
        //注册服务
        String providerDoc = provider.toString();
        this.redis.getRedisTemplate().opsForHash().put(key, appId, providerDoc);
        this.messagePublisher.publish(key + PATH_SPLITTER + SUB, providerDoc);
    }

    /**
     * emit exists provider info list
     *
     * @param providerList exists provider info list
     */
    @Override
    public void emitProviderList(List<TpDocGraphqlProviderServiceInfo> providerList) {
        this.graphqlEngineService.emitProviderList(providerList);
    }

    private String getAppRootPath() {
        return this.graphQLRegisterConfigure.getRoot() + PATH_SPLITTER + APPS;
    }

}
