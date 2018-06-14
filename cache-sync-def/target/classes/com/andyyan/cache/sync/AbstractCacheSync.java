package com.andyyan.cache.sync;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Create by yantingxin 2018/6/12
 */
public abstract class AbstractCacheSync implements CacheSync {

    private static final Logger log = LoggerFactory.getLogger(AbstractCacheSync.class);

    //记录通知的关系
    private ConcurrentHashMap<String, CacheSyncNotify> syncNotifyMap = new ConcurrentHashMap<String, CacheSyncNotify>();


    public void destroy() {

    }

    protected String getSyncKey(String serverName, String key) {
        return serverName + ":" + key;
    }

    public void subscribe(String serverName, String key, CacheSyncNotify localNotify) {
        if (serverName == null || StringUtils.isEmpty(key) || localNotify == null) {
            return;
        }
        String syncKey = getSyncKey(serverName, key);
        syncNotifyMap.put(syncKey, localNotify);
    }

    public void unsubscribe(String serverName, String key) {
        if (serverName == null || StringUtils.isEmpty(key)) {
            return;
        }
        String syncKey = getSyncKey(serverName,  key);
        syncNotifyMap.remove(syncKey);
    }

    public void syncNotify(String serverName, String key, String data) {
        if (serverName == null || key == null) {
            return;
        }
        log.info("AbstractCacheSync.syncNotify(" + "serverName = " + serverName + ", key = " + key + ", data = " + data + ")");
        try {
            String syncKey = getSyncKey(serverName, key);
            CacheSyncNotify syncNotify = syncNotifyMap.get(syncKey);
            if (syncNotify == null) {
                log.info("AbstractCacheSync.syncNotify(" + "serverName = " + serverName + ", key = " + key + ", data = " + data + "), no notifier");
                return;
            }

            syncNotify.notify(data);
        } catch (Exception e) {
            log.error("AbstractCacheSync.syncNotify(" + "serverName = " + serverName + ", key = " + key + ", data = " + data + ")", e);
        }
    }
}
