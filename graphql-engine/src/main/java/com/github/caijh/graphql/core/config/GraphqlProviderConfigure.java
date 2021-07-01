package com.github.caijh.graphql.core.config;

import java.util.Map;

import com.github.caijh.graphql.core.GraphqlConsts;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author xuwenzhen
 * @date 2019/4/2
 */
@Component
@ConfigurationProperties(prefix = "graphql.provider")
public class GraphqlProviderConfigure {

    private Map<String, String> providerService;

    private Map<String, String> innerProviderSchema;

    private Map<String, String> urlSchemaMap;

    public String getProviderService(String appId) {
        if (this.providerService == null) {
            return null;
        }
        return this.providerService.get(appId);
    }

    public void setProviderService(Map<String, String> providerService) {
        this.providerService = providerService;
    }

    public Map<String, String> getInnerProviderSchema() {
        return this.innerProviderSchema;
    }

    public void setInnerProviderSchema(Map<String, String> innerProviderSchema) {
        this.innerProviderSchema = innerProviderSchema;
    }

    public Map<String, String> getUrlSchemaMap() {
        return this.urlSchemaMap;
    }

    public void setUrlSchemaMap(Map<String, String> urlSchemaMap) {
        this.urlSchemaMap = urlSchemaMap;
    }

    public String getUrlSchemaName(String url) {
        if (this.urlSchemaMap == null) {
            return GraphqlConsts.STR_DEFAULT;
        }
        return this.urlSchemaMap.computeIfAbsent(url, u -> GraphqlConsts.STR_DEFAULT);
    }

}
