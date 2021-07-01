package com.github.caijh.graphql.core.config;

import java.util.concurrent.TimeUnit;
import javax.annotation.PreDestroy;

import com.google.common.collect.ImmutableList;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Http客户端配置
 *
 * @author xuwenzhen
 * @date 2019/7/16
 */
@Configuration
public class OkHttpClientConfigure {

    private OkHttpClient okHttpClient;

    /**
     * 创建OkHttp客户端
     *
     * @return OkHttp客户端实例
     */
    @Bean
    public OkHttpClient client() {
        this.okHttpClient = new OkHttpClient.Builder()
                .protocols(ImmutableList.of(Protocol.HTTP_2, Protocol.HTTP_1_1))
                .callTimeout(5, TimeUnit.SECONDS)
                .build();

        return this.okHttpClient;
    }

    /**
     * destroy OkHttp instance
     */
    @PreDestroy
    public void destroy() {
        if (this.okHttpClient != null) {
            this.okHttpClient.dispatcher().executorService().shutdown();
            this.okHttpClient.connectionPool().evictAll();
        }
    }

}
