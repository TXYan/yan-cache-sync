package com.andyyan.cache.sync;

/**
 * Create by yantingxin 2018/6/12
 */
public interface CacheSync {

    void subscribe(String serverName, String key, CacheSyncNotify syncNotify);

    void unsubscribe(String serverName, String key);

    void publish(String serverName, String key, String data);

    void publishAsync(String serverName, String key, String data);
}
