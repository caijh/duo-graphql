package com.github.caijh.graphql.provider.redis;

import javax.inject.Inject;

import com.github.caijh.framework.data.redis.support.Redis;
import org.springframework.stereotype.Service;

/**
 * @author xuwenzhen
 * @since 2019/8/21
 */
@Service
public class RedisMessagePublisher {

    @Inject
    private Redis redis;

    /**
     * 发布消息
     *
     * @param message 消息
     */
    public void publish(String topic, String message) {
        this.redis.getRedisTemplate().convertAndSend(topic, message);
    }

}
