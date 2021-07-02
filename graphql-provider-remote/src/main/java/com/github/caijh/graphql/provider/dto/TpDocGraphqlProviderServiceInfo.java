package com.github.caijh.graphql.provider.dto;

import java.util.List;
import java.util.Map;

/**
 * Duo-Doc项目的接入请求
 *
 * @author xuwenzhen
 * @since 2019/4/16
 */
public class TpDocGraphqlProviderServiceInfo {

    /**
     * Superdiamond 或 MeshService上的名称
     */
    private String appId;

    /**
     * 版本ID，比如commitId
     */
    private String vcsId;

    /**
     * Schema中的领域名称
     */
    private String moduleName;

    /**
     * 服务端地址，如果未设置，会使用全局的地址代替
     */
    private String server;

    /**
     * Provider提供的基础视图信息
     */
    private List<ProviderModelInfo> models;

    /**
     * 如果本服务还提供了默认领域外的接口时配置
     * moduleName => controllers
     * 多个controller使用半角逗号分隔开
     */
    private Map<String, String> moduleMap;

    /**
     * GraphQL 引擎中Schema的名称
     */
    private String schemaName;

    public String getAppId() {
        return this.appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getVcsId() {
        return this.vcsId;
    }

    public void setVcsId(String vcsId) {
        this.vcsId = vcsId;
    }

    public String getServer() {
        return this.server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public String getModuleName() {
        return this.moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public List<ProviderModelInfo> getModels() {
        return this.models;
    }

    public void setModels(List<ProviderModelInfo> models) {
        this.models = models;
    }

    public Map<String, String> getModuleMap() {
        return this.moduleMap;
    }

    public void setModuleMap(Map<String, String> moduleMap) {
        this.moduleMap = moduleMap;
    }

    public String getSchemaName() {
        return this.schemaName;
    }

    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    /**
     * 为了少依赖，自己写吧...
     *
     * @return
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb
                .append("{\"appId\":").append(this.appId == null ? "null" : "\"" + this.appId + "\"")
                .append(",\"vcsId\":").append(this.vcsId == null ? "null" : "\"" + this.vcsId + "\"")
                .append(",\"moduleName\":").append(this.moduleName == null ? "null" : "\"" + this.moduleName + "\"")
                .append(",\"server\":").append(this.server == null ? "null" : "\"" + this.server + "\"")
                .append(",\"schemaName\":").append(this.schemaName == null ? "null" : "\"" + this.schemaName + "\"")
                .append(",\"models\":[");
        if (this.models != null && !this.models.isEmpty()) {
            for (int i = 0; i < this.models.size(); i++) {
                ProviderModelInfo model = this.models.get(i);
                sb.append(model.toString());
                if (i < this.models.size() - 1) {
                    sb.append(",");
                }
            }
        }
        sb.append("]");
        if (this.moduleMap != null && !this.moduleMap.isEmpty()) {
            sb.append(",\"moduleMap\":{");
            this.moduleMap.forEach((key, value) -> {
                sb.append("\"");
                sb.append(key);
                sb.append("\":\"");
                sb.append(value);
                sb.append("\",");
            });
            sb.deleteCharAt(sb.length() - 1);
            sb.append("}");
        }
        sb.append("}");
        return sb.toString();
    }

}
