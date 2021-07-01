package com.github.caijh.graphql.service.impl;

import java.io.IOException;

import com.github.caijh.graphql.core.GraphqlConsts;
import com.github.caijh.graphql.core.exception.OkHttpInvocationException;
import com.github.caijh.graphql.core.util.OkHttpUtils;
import com.github.caijh.graphql.provider.dto.provider.ProviderApiDto;
import com.github.caijh.graphql.register.JsonService;
import com.github.caijh.graphql.register.config.GraphqlRegisterConfigure;
import com.github.caijh.graphql.register.config.GraphqlServerConfigure;
import com.github.caijh.graphql.register.utils.GzipUtils;
import com.github.caijh.graphql.service.TpdocService;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * @author xuwenzhen
 * @date 2019/8/6
 */
@Service
public class TpdocServiceImpl implements TpdocService {

    private static final Logger logger = LoggerFactory.getLogger(TpdocServiceImpl.class);

    @Autowired
    private OkHttpClient okHttpClient;

    @Autowired
    private JsonService jsonService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private GraphqlServerConfigure graphqlServerConfigure;

    @Autowired
    private GraphqlRegisterConfigure graphQLRegisterConfigure;

    /**
     * 拉取Duo-Doc上的配置
     *
     * @param appId    appId
     * @param vcsId    版本号，如果为空时，取最新的
     * @param apiCodes 需要拉取的接口code，多个使用半角逗号分隔
     * @return 返回接口文档信息
     */
    @Override
    public ProviderApiDto fetchDocData(String appId, String vcsId, String apiCodes) {
        if (StringUtils.isEmpty(vcsId)) {
            vcsId = GraphqlConsts.STR_DEFAULT_VCS_ID;
        }
        //尝试从redis中取
        String key = this.getProviderPath(appId);

        HashOperations<String, Object, Object> opsForHash = this.redisTemplate.opsForHash();
        String providerDocStr = (String) opsForHash.get(key, vcsId);
        if (StringUtils.isEmpty(providerDocStr)) {
            providerDocStr = this.getProviderDoc(appId, vcsId, apiCodes);
            if (!StringUtils.isEmpty(providerDocStr)) {
                logger.info("put redis {}:{}", key, vcsId);
                opsForHash.put(key, vcsId, providerDocStr);
            }
        } else {
            logger.info("doc from redis. {}:{}", key, vcsId);
        }

        return this.jsonService.toObject(providerDocStr, ProviderApiDto.class);
    }

    private String getProviderDoc(String appId, String vcsId, String apiCodes) {
        String tpdocAddress = this.graphqlServerConfigure.getTpdocUrl();
        String url = tpdocAddress + "/api/doc/app/" + appId + GraphqlConsts.PATH_SPLITTER;
        HttpUrl httpUrl = HttpUrl.parse(url);
        if (httpUrl == null) {
            throw new OkHttpInvocationException("调用发生错误，url异常：" + url);
        }

        HttpUrl.Builder urlBuilder = httpUrl.newBuilder();
        //添加参数
        urlBuilder.addEncodedQueryParameter(GraphqlConsts.VCS_ID, vcsId);
        if (!StringUtils.isEmpty(apiCodes)) {
            urlBuilder.addEncodedQueryParameter(GraphqlConsts.API_CODES, apiCodes);
        }

        Request.Builder requestBuilder = OkHttpUtils.getRestFulRequestBuilder(urlBuilder);

        int index = tpdocAddress.indexOf(GraphqlConsts.STR_DOUBLE_PATH_SPLITTER) + GraphqlConsts.STR_DOUBLE_PATH_SPLITTER.length();
        String domain = tpdocAddress.substring(index);
        requestBuilder.addHeader(GraphqlConsts.DOMAIN, domain);

        Request request = requestBuilder.build();
        logger.info("获取Provider API信息请求：{}", request);
        try (Response response = this.okHttpClient.newCall(request).execute()) {
            byte[] bytes;
            try (ResponseBody body = response.body()) {
                if (body == null) {
                    logger.error("获取服务{}文档数据为空，query: {}", appId, request);
                    return null;
                }
                bytes = body.bytes();
            }
            if (bytes.length < GraphqlConsts.MIN_PROVIDER_API_INFO_LEN) {
                logger.error("获取服务{}文档数据异常，query: {}", appId, request);
                return null;
            }

            return GzipUtils.decompress(bytes);
        } catch (IOException e) {
            logger.error("获取服务{}文档数据失败，query: {}", appId, request, e);
            return null;
        }
    }

    private String getProviderPath(String appId) {
        return this.graphQLRegisterConfigure.getRoot()
                + GraphqlConsts.STR_CLN
                + GraphqlConsts.STR_APIS
                + GraphqlConsts.STR_CLN
                + appId;
    }

}
