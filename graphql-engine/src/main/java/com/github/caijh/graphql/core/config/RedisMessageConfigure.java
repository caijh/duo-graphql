package com.github.caijh.graphql.core.config;

import com.github.caijh.graphql.core.subscribe.RedisMessageSubscriber;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

/**
 * @author xuwenzhen
 * @date 2019/8/21
 */
@Configuration
public class RedisMessageConfigure {

    @Bean
    public RedisMessageListenerContainer redisContainer(RedisConnectionFactory redisConnectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(redisConnectionFactory);
        return container;
    }

    @Bean
    public MessageListenerAdapter messageListener(RedisTemplate<String, Object> redisTemplate) {
        RedisMessageSubscriber redisMessageSubscriber = new RedisMessageSubscriber();
        redisMessageSubscriber.setRedisTemplate(redisTemplate);
        return new MessageListenerAdapter(redisMessageSubscriber);
    }

}
