package com.github.caijh.graphql.core;

import java.io.Serializable;
import java.util.List;

import com.github.caijh.graphql.fetcher.DataFetcherProxy;
import com.github.caijh.graphql.provider.dto.provider.Api;
import com.google.common.collect.Lists;

/**
 * 需要进行合并请求的数据
 *
 * @author xuwenzhen
 * @date 2019/7/29
 */
public class BatchDataFetcherData implements Serializable {

    private static final long serialVersionUID = 7039501003342037525L;
    /**
     * 当前模块的上下文
     */
    private final GraphqlModuleContext contextModule;

    /**
     * 合并请求的接口
     */
    private final Api api;

    /**
     * 当前字段绑定的DataFetcher
     */
    private final DataFetcherProxy dataFetcher;

    /**
     * 需要查询的字段
     */
    private List<String> selections;

    /**
     * 字段路径
     */
    private final String fieldPath;

    public BatchDataFetcherData(GraphqlModuleContext contextModule, DataFetcherProxy dataFetcher, Api api, String fieldPath) {
        this.contextModule = contextModule;
        this.dataFetcher = dataFetcher;
        this.api = api;
        this.fieldPath = fieldPath;
    }

    public DataFetcherProxy getDataFetcher() {
        return this.dataFetcher;
    }

    public Api getApi() {
        return this.api;
    }

    public GraphqlModuleContext getContextModule() {
        return this.contextModule;
    }

    public String getFieldPath() {
        return this.fieldPath;
    }

    public List<String> getSelections() {
        return this.selections;
    }

    public void setSelections(List<String> selections) {
        this.selections = selections;
    }

    public void addSelection(String selection) {
        if (this.selections == null) {
            this.selections = Lists.newArrayList();
        }
        this.selections.add(selection);
    }

}
