package com.github.caijh.graphql.register.server;

/**
 * 服务schema / query 加载器
 *
 * @author xuwenzhen
 * @since 2019/4/3
 */
public interface GraphqlRegisterServer {

    /**
     * 注册数据供应服务
     *
     * @param data Provider注册的数据
     */
    void registerGraphql(String data);

}
