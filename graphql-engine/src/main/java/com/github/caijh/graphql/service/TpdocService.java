package com.github.caijh.graphql.service;

import com.github.caijh.graphql.provider.dto.provider.ProviderApiDto;

/**
 * Duo-Doc服务
 *
 * @author xuwenzhen
 * @date 2019/8/6
 */
public interface TpdocService {

    /**
     * 拉取Duo-Doc上的配置
     *
     * @param appId    appId
     * @param vcsId    版本号，如果为空时，取最新的
     * @param apiCodes 需要拉取的接口code，多个使用半角逗号分隔
     * @return 返回接口文档信息
     */
    ProviderApiDto fetchDocData(String appId, String vcsId, String apiCodes);

}
