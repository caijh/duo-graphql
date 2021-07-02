package com.github.caijh.graphql.provider.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * schema provider.
 */
@Target({TYPE})
@Retention(RUNTIME)
public @interface SchemaProvider {

    /**
     * 基本视图名称
     */
    Class<?> clazz();

    /**
     * 基本视图关联的外键ids
     */
    String[] ids();

}
