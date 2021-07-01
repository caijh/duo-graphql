package com.github.caijh.graphql.service.message;

import java.util.List;

import com.github.caijh.graphql.core.GraphqlConsts;
import com.github.caijh.graphql.fetcher.SubscriptionDataFetcherProxy;
import com.github.caijh.graphql.register.JsonService;
import com.google.common.collect.Lists;
import io.reactivex.ObservableEmitter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

/**
 * 订阅消息处理
 *
 * @author xuwenzhen
 * @date 2019/8/22
 */
@Service
public class SubscriptionMessageServiceImpl extends BaseMessageService {

    private static final Logger logger = LoggerFactory.getLogger(SubscriptionMessageServiceImpl.class);

    /**
     * 订阅的名称长度
     */
    private int subscriptionNameLen = 0;

    @Autowired
    private JsonService jsonService;

    /**
     * 获取当前消息处理的topic或pattern
     *
     * @return topic / pattern
     */
    @Override
    public String getTopic() {
        String subscriptionName = this.getRoot() + GraphqlConsts.STR_SUBSCRIPTION + GraphqlConsts.STR_CLN;
        this.subscriptionNameLen = subscriptionName.length();
        return subscriptionName + GraphqlConsts.STR_STAR;
    }

    /**
     * 处理消息
     *
     * @param channel 频道名称
     * @param data    接收到的数据
     */
    @Override
    public void process(String channel, String data) {
        String topicName = channel.substring(this.subscriptionNameLen);
        logger.info("[{}]准备处理：{}", topicName, data);
        List<ObservableEmitter<Object>> emitters = SubscriptionDataFetcherProxy.TOPIC_EMITTERS_MAP.get(topicName);

        //推送到各订阅者名下
        if (!CollectionUtils.isEmpty(emitters)) {
            Object dataMap = this.jsonService.toObject(data);
            List<ObservableEmitter<Object>> disposedEmitter = Lists.newArrayList();
            emitters.forEach(emitter -> {
                if (emitter.isDisposed()) {
                    disposedEmitter.add(emitter);
                    logger.info("[{}]删除emitter", topicName);
                } else {
                    emitter.onNext(dataMap);
                }
            });
            if (!disposedEmitter.isEmpty()) {
                emitters.removeAll(disposedEmitter);
            }
        } else {
            logger.info("[{}]未找到订阅者，丢弃", topicName);
        }
    }

}
