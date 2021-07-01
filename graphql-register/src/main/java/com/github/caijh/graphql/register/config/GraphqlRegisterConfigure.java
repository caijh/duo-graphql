package com.github.caijh.graphql.register.config;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Component;

/**
 * @author xuwenzhen
 * @date 2019/4/2
 */
@Component
@ComponentScan(basePackages = "com.github.caijh.graphql")
@ConfigurationProperties(prefix = "duo.graphql.register")
public class GraphqlRegisterConfigure {

    /**
     * 注册中心地址，目前支持zookeeper
     */
    private String address;

    /**
     * zk根目录名称，默认为：graphql
     */
    private String root;

    /**
     * 以客户端模式运行时，当前服务ID
     * 需要用此进行mesh调用，所以需要保持一致！
     */
    private String serviceId;

    private Map<String, String> providerService;

    public String getAddress() {
        return this.address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getRoot() {
        return this.root;
    }

    public void setRoot(String root) {
        this.root = root;
    }

    public String getServiceId() {
        return this.serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public Map<String, String> getProviderService() {
        return this.providerService;
    }

    public void setProviderService(Map<String, String> providerService) {
        this.providerService = providerService;
    }

}
