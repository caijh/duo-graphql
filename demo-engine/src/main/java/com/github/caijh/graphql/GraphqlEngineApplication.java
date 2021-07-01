package com.github.caijh.graphql;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author xuwenzhen
 */
@SpringBootApplication
public class GraphqlEngineApplication {

    public static void main(String[] args) {
        new SpringApplication(GraphqlEngineApplication.class).run(args);
    }

}
