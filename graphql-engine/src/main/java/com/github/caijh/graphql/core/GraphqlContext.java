package com.github.caijh.graphql.core;

import java.util.List;
import java.util.Map;

import com.github.caijh.graphql.pipeline.RegistryState;
import com.github.caijh.graphql.provider.BaseDataFetcher;
import com.github.caijh.graphql.provider.InnerProvider;
import com.github.caijh.graphql.provider.dto.provider.Api;
import com.github.caijh.graphql.provider.dto.provider.Entity;
import com.google.common.collect.Maps;
import org.springframework.util.CollectionUtils;

/**
 * GraphQL Provider上下文环境
 *
 * @author xuwenzhen
 * @date 2019/6/3
 */
public class GraphqlContext {

    /**
     * 各实体映射表（不包含InnerProvider的）
     * entityName => Entity
     */
    private Map<String, Entity> entityMap = Maps.newHashMap();

    /**
     * API Map
     * api.code / graphqlProviderName => Api
     */
    private Map<String, Api> apiMap = Maps.newHashMap();

    /**
     * 接口-DataFetcher映射表
     * api code => BaseDataFetch
     * refId => BaseDataFetch
     * FieldCoordinates => BaseDataFetcher
     */
    private Map<String, BaseDataFetcher> dataFetcherMap = Maps.newHashMap();

    /**
     * 是否正在构建
     */
    private boolean busy = false;

    /**
     * 各模块环境Map
     * moduleName => GraphqlModuleContext
     */
    private Map<String, GraphqlModuleContext> moduleContextMap = Maps.newLinkedHashMap();

    /**
     * 当前正在注册的服务，注册成功后将被设置为null
     */
    private RegistryState registryState;

    public GraphqlContext(RegistryState registryState) {
        this.setRegistryState(registryState);
    }

    public void setRegistryState(RegistryState registryState) {
        this.registryState = registryState;
        this.busy = true;
    }

    /**
     * 是否正在构建
     *
     * @return 返回是否正在构建
     */
    public boolean isBusy() {
        return this.busy;
    }

    /**
     * 完成注册
     */
    public void finish() {
        if (this.registryState != null) {
            this.entityMap = this.registryState.getEntityMap();
            this.dataFetcherMap = this.registryState.getDataFetcherMap();
            this.moduleContextMap = this.registryState.getModuleContextMap();
            this.apiMap = this.registryState.getApiMap();

            //销毁
            this.registryState = null;
        }
        this.busy = false;
    }

    /**
     * 中断
     */
    public void restart() {
        this.registryState = null;
        this.busy = false;
    }

    public RegistryState getRegistryState() {
        return this.registryState;
    }

    /**
     * 注册Inner Providers
     *
     * @param innerProviders 内置服务列表
     */
    public void registryInnerProvider(List<InnerProvider> innerProviders) {
        if (CollectionUtils.isEmpty(innerProviders)) {
            return;
        }
        innerProviders.forEach(provider -> {
            GraphqlModuleContext moduleContext = new GraphqlModuleContext(provider);
            this.moduleContextMap.put(provider.getModuleName(), moduleContext);
        });
    }

    /**
     * 通过实体名称获取实体信息
     *
     * @param entityName 实体名称
     * @return 指定实体名称的Entity实例，找不到返回null
     */
    public Entity getEntity(String entityName) {
        return this.entityMap.get(entityName);
    }

    public Map<String, GraphqlModuleContext> getModuleContextMap() {
        return this.moduleContextMap;
    }

    /**
     * 通过领域名称获取领域上下文
     *
     * @param moduleName 领域名称
     * @return 领域上下文
     */
    public GraphqlModuleContext getContextModule(String moduleName) {
        return this.moduleContextMap.get(moduleName);
    }

    /**
     * get Api by api.code or refId or FieldCoordinates string
     *
     * @param code api.code or refId or FieldCoordinates
     * @return Api or null if not exists
     */
    public Api getApi(String code) {
        return this.apiMap.get(code);
    }

    /**
     * get dataFetcher by FieldCoordinates
     *
     * @param typeName  type name
     * @param fieldName field name in type
     * @return DataFetcher or null if not exists
     */
    public BaseDataFetcher getDataFetcher(String typeName, String fieldName) {
        return this.dataFetcherMap.get(typeName + GraphqlConsts.CHAR_DOT + fieldName);
    }

}
