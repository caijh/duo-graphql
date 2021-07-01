package com.github.caijh.graphql;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author xuwenzhen
 */
@SpringBootApplication //Spring启动时需要扫描的包名
public class GraphqlProviderApplication {

    public static void main(String[] args) {
        new SpringApplication(GraphqlProviderApplication.class).run(args);
    }

}
