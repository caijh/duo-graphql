package com.github.caijh.graphql.register;

import java.util.List;

/**
 * GraphQL注册中心注册器
 *
 * @author xuwenzhen
 * @since 2019/4/2
 */
public interface GraphqlRegister<T> {

    /**
     * 向注册中心注册供应端服务的信息
     *
     * @param providerServiceData 供应端服务的信息数据
     */
    void register(T providerServiceData);

    /**
     * emit exists provider info list
     *
     * @param providerList exists provider info list
     */
    void emitProviderList(List<T> providerList);

//    /**
//     * GraphQL Provider变更发布
//     *
//     * @return Publisher
//     */
//    Observable<List<T>> getProviderObservable();
//    Publisher<List<T>> getPublisher();

//    /**
//     * 获取推送者
//     * @return ObservableEmitter
//     */
//    ObservableEmitter<List<T>> getProviderEmitter();
}
