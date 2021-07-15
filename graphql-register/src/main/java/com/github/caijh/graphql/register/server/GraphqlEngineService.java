package com.github.caijh.graphql.register.server;

import java.util.List;

/**
 * graphql provider registry service
 *
 * @param <T> graphql provider info
 * @author xuwenzhen
 * @since 2019/4/9
 */
public interface GraphqlEngineService<T> {

    /**
     * registry graphql provider to registry center, eg. zookeeper or redis
     *
     * @param key                     key for graphql provider
     * @param providerServiceDataList graphql provider info list
     */
    void registry(String key, List<T> providerServiceDataList);

    /**
     * emit the changed graphQL provider info list. so that, every graphql engine can get this change
     *
     * @param providerList changed graphQL provider info list
     */
    void emitProviderList(List<T> providerList);

}
