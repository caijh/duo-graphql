package com.github.caijh.graphql.core;

import java.util.Map;
import java.util.Set;

import com.github.caijh.graphql.core.subscribe.GraphqlSubscriber;
import com.github.caijh.graphql.fetcher.batcher.BatchLoader;
import com.google.common.collect.Maps;

/**
 * 查询运行时上下文
 *
 * @author xuwenzhen
 * @date 2019/6/24
 */
public class UserExecutionContext {

    /**
     * 当前schema名称
     */
    private String schemaName;

    /**
     * 当前属性路径->BatchLoader
     */
    private final Map<String, BatchLoader> batchDataLoaders = Maps.newConcurrentMap();

    /**
     * 请求头
     */
    private Map<String, String> headers = Maps.newHashMap();

    /**
     * 需要透传的header names
     */
    private Set<String> headerNames;

    /**
     * 当前查询的缓存KEY
     */
    private String executionKey;
    private GraphqlSubscriber subscriber;

    /**
     * 将某个dataFetcher添加到批量处理里面，并执行请求
     *
     * @param batchLoader 批量处理器
     */
    public void bathFetch(BatchLoader batchLoader) {
        batchLoader.setHeaders(this.headers);
        this.batchDataLoaders.computeIfAbsent(batchLoader.getPath(), key -> batchLoader).fetchData();
    }

    public BatchLoader getBatchLoader(String key) {
        return this.batchDataLoaders.get(key);
    }

    public Map<String, String> getHeaders() {
        return this.headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public void addHeader(String headerName, String headerValue) {
        this.headers.put(headerName, headerValue);
    }

    public String getExecutionKey() {
        return this.executionKey;
    }

    public void setExecutionKey(String executionKey) {
        this.executionKey = executionKey;
    }

    public String getSchemaName() {
        return this.schemaName;
    }

    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    public void setSubscriber(GraphqlSubscriber subscriber) {
        this.subscriber = subscriber;
    }

    public GraphqlSubscriber getSubscriber() {
        return this.subscriber;
    }

    public Set<String> getHeaderNames() {
        return this.headerNames;
    }

    public void setHeaderNames(Set<String> headerNames) {
        this.headerNames = headerNames;
    }

}
