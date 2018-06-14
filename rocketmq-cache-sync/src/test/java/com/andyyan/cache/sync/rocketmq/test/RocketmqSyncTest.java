package com.andyyan.cache.sync.rocketmq.test;

import com.andyyan.cache.sync.CacheSyncNotify;
import com.andyyan.cache.sync.rocketmq.impl.RocketMqCacheSyncImpl;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Create by yantingxin 2018/6/12
 */
public class RocketmqSyncTest {

    public static void main(String[] args) {
        ApplicationContext context = new ClassPathXmlApplicationContext("classpath:spring/*.xml");
        String serverName = "yan-test";
        RocketMqCacheSyncImpl rocketmqCacheSync = (RocketMqCacheSyncImpl) context.getBean("rocketMqCacheSyncImpl1");
        rocketmqCacheSync.subscribe(serverName, "testKey", new CacheSyncNotify() {
            public void notify(String data) {
                System.out.println("notify data:" + data);
            }
        });
        rocketmqCacheSync.publish(serverName, "testKey", "test-data1" + System.currentTimeMillis());

        try {
            Thread.sleep(3000L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println(rocketmqCacheSync);
    }
}
