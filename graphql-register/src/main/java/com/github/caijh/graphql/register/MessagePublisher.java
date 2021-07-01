package com.github.caijh.graphql.register;

import java.util.Set;

import com.google.common.collect.Sets;

/**
 * 消息发布器
 *
 * @author xuwenzhen
 * @date 2019/8/21
 */
public interface MessagePublisher {

    Set<String> topics = Sets.newConcurrentHashSet();

    /**
     * 发布消息
     *
     * @param topic   需要发送的队列
     * @param message 消息
     */
    void publish(String topic, String message);

    /**
     * 订阅某个topic
     *
     * @param topic 需要订阅的topic
     */
    void subscribe(String topic);

}
