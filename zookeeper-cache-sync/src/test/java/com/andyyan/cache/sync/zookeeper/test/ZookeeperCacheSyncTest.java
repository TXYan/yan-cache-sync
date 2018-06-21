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
        final AbstractCacheSync cacheSync = new ZookeeperCacheSyncImpl("127.0.0.1:2181", 3000, "/andyyan", 50);
        cacheSync.subscribe(serverName, "lc-test", new CacheSyncNotify() {
            public void notify(String data) {
                System.out.println("i receive data:" + data);
            }
        });
        //并发或者过快会导致丢信息 对同一个数据修改并不会这么快，20%的数据丢失
        for (int i = 0; i < 10000; i++) {
            cacheSync.publish(serverName, "lc-test", "repeat:" + i);
        }
        Thread.sleep(60000L);
    }
}
