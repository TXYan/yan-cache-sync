package com.andyyan.cache.sync.rocketmq.impl;

import com.alibaba.rocketmq.client.consumer.DefaultMQPushConsumer;
import com.alibaba.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import com.alibaba.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import com.alibaba.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import com.alibaba.rocketmq.client.exception.MQClientException;
import com.alibaba.rocketmq.client.producer.DefaultMQProducer;
import com.alibaba.rocketmq.client.producer.SendCallback;
import com.alibaba.rocketmq.client.producer.SendResult;
import com.alibaba.rocketmq.client.producer.SendStatus;
import com.alibaba.rocketmq.common.consumer.ConsumeFromWhere;
import com.alibaba.rocketmq.common.message.Message;
import com.alibaba.rocketmq.common.message.MessageExt;
import com.alibaba.rocketmq.common.protocol.heartbeat.MessageModel;
import com.andyyan.cache.sync.rocketmq.serializer.KryoSerializer;
import com.zhipin.cache.sync.AbstractCacheSync;
import com.zhipin.cache.sync.CacheSyncProperties;
import net.sf.json.JSONObject;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Create by yantingxin 2018/6/12
 */
public class RocketMqCacheSyncImpl extends AbstractCacheSync {

    Logger log = LoggerFactory.getLogger(RocketMqCacheSyncImpl.class);

    private final static int SEND_TIMEOUT = 2000;

    private DefaultMQProducer defaultMQProducer;
    private String topic;
    private String tags;//订阅的tag列表 *代表所有，多个格式如下：tag1||tag2||tag3

    private DefaultMQPushConsumer defaultMQConsumer;

    public void publish(String server, String key, String data) {
        try {
            String body = dataJson(key, data);
            Message message = new Message(topic, server, key, KryoSerializer.encode(body));
            SendResult sendResult = defaultMQProducer.send(message, SEND_TIMEOUT);
            if (sendResult == null || !sendResult.getSendStatus().equals(SendStatus.SEND_OK)) {
                log.warn("RocketMqCacheSyncImpl1.publish(" + "server = " + server + ", key = " + key + ", data = " + data + ")");
            }
        } catch (Exception e) {
            log.error("RocketMqCacheSyncImpl1.publish(" + "server = " + server + ", key = " + key + ", data = " + data + ")", e);
        }
    }

    @Override
    public void publishAsync(final String server, final String key, final String data) {
        try {
            String body = dataJson(key, data);
            Message message = new Message(topic, server, key, KryoSerializer.encode(body));
            defaultMQProducer.send(message, new SendCallback() {
                @Override
                public void onSuccess(SendResult sendResult) {
                    if (sendResult == null || !sendResult.getSendStatus().equals(SendStatus.SEND_OK)) {
                        log.warn("RocketMqCacheSyncImpl1.publish(" + "server = " + server + ", key = " + key + ", data = " + data + ")");
                    }
                }

                @Override
                public void onException(Throwable e) {

                }
            });

        } catch (Exception e) {
            log.error("RocketMqCacheSyncImpl1.publish(" + "server = " + server + ", key = " + key + ", data = " + data + ")", e);
        }
    }

    private String dataJson(String key, String data) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(CacheSyncProperties.BIZ_KEY.getDesc(), key);
        jsonObject.put(CacheSyncProperties.BIZ_DATA.getDesc(), data);
        return jsonObject.toString();
    }

    public void setDefaultMQProducer(DefaultMQProducer defaultMQProducer) {
        this.defaultMQProducer = defaultMQProducer;
        try {
            defaultMQProducer.start();
        } catch (MQClientException e) {
            log.error("RocketMqCacheSyncImpl1.setDefaultMQProducer(" + "defaultMQProducer = " + defaultMQProducer + ")", e);
        }
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public void setDefaultMQConsumer(DefaultMQPushConsumer defaultMQConsumer) {
        this.defaultMQConsumer = defaultMQConsumer;
        try {
            if (StringUtils.isBlank(tags)) {
                tags = "*";
            }
            defaultMQConsumer.subscribe(topic, tags);
            defaultMQConsumer.registerMessageListener(new MessageListenerConcurrently() {
                public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> list, ConsumeConcurrentlyContext consumeConcurrentlyContext) {
                    if (CollectionUtils.isEmpty(list))  {
                        return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
                    }
                    for (int idx = 0; idx < list.size(); idx++) {
                        consumeOneMessage(list.get(idx));
                    }
                    return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
                }
            });
            defaultMQConsumer.setMessageModel(MessageModel.BROADCASTING);
            defaultMQConsumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET);
            //不好使  理解错了？
//            defaultMQConsumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_TIMESTAMP);
//            defaultMQConsumer.setConsumeTimestamp(UtilAll.timeMillisToHumanString3(System.currentTimeMillis()));
            defaultMQConsumer.start();
        } catch (Exception e) {
            log.error("RocketMqCacheSyncImpl1.setDefaultMQConsumer(" + "defaultMQConsumer = " + defaultMQConsumer + ")", e);
        }
    }

    private boolean consumeOneMessage(MessageExt messageExt) {
        if (messageExt == null) {
            return true;
        }
        if(System.currentTimeMillis()- messageExt.getBornTimestamp()> 5*1000) {//5秒
            return true;//过期消息跳过
        }
        try {
            String serverName = messageExt.getTags();
            String body = KryoSerializer.decodeByte2String(messageExt.getBody());
            JSONObject jsonObject = JSONObject.fromObject(body);
            String key = jsonObject.optString(CacheSyncProperties.BIZ_KEY.getDesc());
            String data = jsonObject.optString(CacheSyncProperties.BIZ_DATA.getDesc());
            syncNotify(serverName, key, data);
        } catch (Exception e) {
            log.error("RocketMqCacheSyncImpl1.consumeOneMessage(" + "messageExt = " + messageExt + ")", e);
        }
        return true;
    }
}
