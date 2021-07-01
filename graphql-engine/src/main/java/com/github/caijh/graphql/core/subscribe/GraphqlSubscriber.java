package com.github.caijh.graphql.core.subscribe;

import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import javax.websocket.Session;

import com.github.caijh.graphql.core.util.GraphqlContextUtils;
import com.github.caijh.graphql.register.JsonService;
import com.google.common.collect.Sets;
import graphql.ExecutionResult;
import io.reactivex.ObservableEmitter;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 订阅器，每个会议是一个新的对象
 *
 * @author xuwenzhen
 * @date 2019/8/22
 */
public class GraphqlSubscriber implements Subscriber<ExecutionResult> {

    private static final Logger logger = LoggerFactory.getLogger(GraphqlSubscriber.class);
    private static JsonService jsonService;

    private final AtomicReference<Subscription> subscriptionReference = new AtomicReference<>();

    private final Session session;

    private Set<ObservableEmitter<Object>> emitters;

    /**
     * 消息ID
     */
    private String id;

    @Override
    public void onSubscribe(Subscription subscriber) {
        logger.info("[{}]Subscription onSubscribe", this.session.getId());
        this.subscriptionReference.set(subscriber);
        this.request(1);
    }

    @Override
    public void onNext(ExecutionResult er) {
        init();
        Object data = er.getData();
        String dataJson;
        if (data instanceof String) {
            dataJson = (String) data;
        } else {
            //如果数据不是字符型
            dataJson = jsonService.toJsonString(data);
        }

        logger.info("[{}]Sending update data: {}", this.session.getId(), dataJson);
        this.sendMessage(this.id, GraphqlSubscriptionTypeEnum.GQL_CONNECTION_KEEP_ALIVE.getType(), dataJson);
    }

    @Override
    public void onError(Throwable t) {
        init();
        this.sendMessage(null, GraphqlSubscriptionTypeEnum.GQL_CONNECTION_ERROR.getType(), null);
        logger.error("[{}]Subscription threw an exception", this.session.getId(), t);
        this.cancelSubscription();
    }

    @Override
    public void onComplete() {
        init();
        this.sendMessage(null, GraphqlSubscriptionTypeEnum.GQL_COMPLETE.getType(), null);
        logger.info("[{}]Subscription complete", this.session.getId());
        this.cancelSubscription();
    }

    public GraphqlSubscriber(Session session) {
        this.session = session;
    }

    public void cancelSubscription() {
        logger.info("[{}]Subscription cancelSubscription", this.session.getId());
        Subscription subscription = this.subscriptionReference.get();
        if (subscription != null) {
            subscription.cancel();
        }
    }

    public Session getSession() {
        return this.session;
    }

    public void addEmitter(ObservableEmitter<Object> emitter) {
        if (this.emitters == null) {
            this.emitters = Sets.newHashSet();
        }
        this.emitters.add(emitter);
    }

    public Set<ObservableEmitter<Object>> getEmitters() {
        return this.emitters;
    }

    private void sendMessage(String id, String type, String dataJson) {
        SubscriptionMessage message = new SubscriptionMessage();
        message.setId(id);
        message.setType(type);
        message.setPayload(dataJson);

        this.session.getAsyncRemote().sendText(jsonService.toJsonString(message));
        this.request(1);
    }

    private static void init() {
        if (jsonService == null) {
            jsonService = GraphqlContextUtils.getApplicationContext().getBean(JsonService.class);
        }
    }

    private void request(int n) {
        logger.info("[{}]Subscription request", this.session.getId());
        Subscription subscription = this.subscriptionReference.get();
        if (subscription != null) {
            subscription.request(n);
        }
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return this.id;
    }

}
