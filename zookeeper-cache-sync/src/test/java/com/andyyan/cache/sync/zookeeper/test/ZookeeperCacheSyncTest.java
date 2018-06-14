package com.andyyan.cache.sync.zookeeper.test;

import com.andyyan.cache.sync.AbstractCacheSync;
import com.andyyan.cache.sync.CacheSyncNotify;
import com.andyyan.cache.sync.zookeeper.impl.ZookeeperCacheSyncImpl;

/**
 * Create by yantingxin 2018/6/12
 */
public class ZookeeperCacheSyncTest {

    public static void main(String[] args) throws InterruptedException {
        String serverName = "yan-test";
        final AbstractCacheSync cacheSync = new ZookeeperCacheSyncImpl("192.168.1.11:2181,192.168.1.11:2181", 3000, "/andyyan");
        cacheSync.subscribe(serverName, "lc-test", new CacheSyncNotify() {
            public void notify(String data) {
                System.out.println("i receive data:" + data);
            }
        });
        cacheSync.publish(serverName, "lc-test", "repeat");
        cacheSync.publish(serverName, "lc-test", "repeat1");
        cacheSync.publish(serverName, "lc-test", "repeat2");
        cacheSync.publish(serverName, "lc-test", "repeat3");
        Thread.sleep(3000L);
    }
}
