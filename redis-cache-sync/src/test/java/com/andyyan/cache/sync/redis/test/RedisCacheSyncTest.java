package com.andyyan.cache.sync.redis.test;

import com.andyyan.cache.sync.CacheSyncNotify;
import com.andyyan.cache.sync.redis.impl.RedisCacheSyncImpl;
import com.taobao.stresstester.StressTestUtils;
import com.taobao.stresstester.core.StressResult;
import com.taobao.stresstester.core.StressTask;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Create by yantingxin 2018/6/15
 */
public class RedisCacheSyncTest {
    public static void main(String[] args) throws InterruptedException {

        final RedisCacheSyncImpl redisCacheSync = new RedisCacheSyncImpl("localhost", 6379, 5, 60, "yan-test");

        int size = 1000000;
        final ConcurrentHashMap<String, String> map = new ConcurrentHashMap<String, String>((int) ((size / 0.75) + 1));

        redisCacheSync.subscribe("yan-test", "testKey", new CacheSyncNotify() {
            public void notify(String data) {
                map.put(data, data);
                System.out.println(data);
            }
        });

        final AtomicInteger pubInteger = new AtomicInteger(1);
        StressResult stressResult = StressTestUtils.test(20, size, new StressTask() {
            public Object doTask() throws Exception {
                redisCacheSync.publish("yan-test", "testKey", "redis-data-" + pubInteger.getAndIncrement());
                return null;
            }
        }, 0);

        String str = StressTestUtils.format(stressResult);
        System.out.println(str);

        System.out.println("map size:" + map.size() + ", pubInteger:" + pubInteger.get());
        Thread.sleep(10000L);
        redisCacheSync.destroy();
    }
}
