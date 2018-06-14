package com.andyyan.cache.sync.redis.test;

import com.andyyan.cache.sync.CacheSyncNotify;
import com.andyyan.cache.sync.redis.impl.RedisCacheSyncImpl;

/**
 * Create by yantingxin 2018/6/15
 */
public class RedisCacheSyncTest {
    public static void main(String[] args) throws InterruptedException {
        RedisCacheSyncImpl redisCacheSync = new RedisCacheSyncImpl("localhost", 6379, 60, "yan-test");
        redisCacheSync.subscribe("yan-test", "testKey", new CacheSyncNotify() {
            public void notify(String data) {
                System.out.println(data);
            }
        });

        redisCacheSync.publish("yan-test", "testKey", "redis-data-" + System.currentTimeMillis());

        Thread.sleep(1000L);
    }
}
