package com.andyyan.cache.sync.zookeeper.impl;

import com.andyyan.cache.sync.AbstractCacheSync;
import com.andyyan.cache.sync.CacheSyncNotify;
import org.I0Itec.zkclient.IZkDataListener;
import org.I0Itec.zkclient.ZkClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Create by yantingxin 2018/6/12
 */
public class ZookeeperCacheSyncImpl extends AbstractCacheSync {

    private static final Logger log = LoggerFactory.getLogger(ZookeeperCacheSyncImpl.class);

    private static final String PATH_SEPARATOR = "/";

    private ZkClient zkClient;
    private String path;
    private IZkDataListener zkDataListener;

    public ZookeeperCacheSyncImpl(String hosts, int timeout, String path) {
        zkClient = new ZkClient(hosts, timeout);
        this.path = path;
        if (!zkClient.exists(path)) {
            zkClient.createPersistent(path, true);
        }

        zkDataListener = new IZkDataListener() {
            public void handleDataChange(String path, Object data) throws Exception {
                String key = path;
                int spitIdx = path.lastIndexOf(PATH_SEPARATOR);
                if (spitIdx >= 0) {
                    key = key.substring(spitIdx + 1, key.length());
                }
                String parentPath = path.substring(0, spitIdx);
                String serverName = parentPath.substring(parentPath.lastIndexOf(PATH_SEPARATOR) + 1);
                syncNotify(serverName, key, data.toString());
            }

            public void handleDataDeleted(String s) throws Exception {

            }
        };
    }

    @Override
    public void subscribe(String serverName, String key, CacheSyncNotify syncNotify) {
        try {
            String keyPath = path + PATH_SEPARATOR + serverName + PATH_SEPARATOR + key;
            if (!zkClient.exists(keyPath)) {
                zkClient.createPersistent(keyPath, true);
            }
            zkClient.subscribeDataChanges(keyPath, zkDataListener);
            super.subscribe(serverName, key, syncNotify);
        } catch (Exception e) {
            log.error("ZookeeperCacheSyncImpl.subscribe(" + "serverName = " + serverName + ", key = " + key + ", syncNotify = " + syncNotify + ")", e);
        }

    }

    @Override
    public void unsubscribe(String serverName, String key) {
        try {
            String keyPath = path + PATH_SEPARATOR + serverName + PATH_SEPARATOR + key;
            zkClient.unsubscribeDataChanges(keyPath, zkDataListener);
            super.unsubscribe(serverName, key);
        } catch (Exception e) {
            log.error("ZookeeperCacheSyncImpl.unsubscribe(" + "serverName = " + serverName + ", key = " + key + ")", e);
        }
    }

    public void publish(String serverName, String key, String data) {
        try {
            String keyPath = path + PATH_SEPARATOR + serverName + PATH_SEPARATOR + key;
            zkClient.writeData(keyPath, data);
        } catch (Exception e) {
            log.error("ZookeeperCacheSyncImpl.publish(" + "serverName = " + serverName + ", key = " + key + ", data = " + data + ")", e);
        }
    }

    @Override
    public void publishAsync(String serverName, String key, String data) {
        throw new UnsupportedOperationException("not supported");
    }

    @Override
    public void destroy() {
        super.destroy();
        if (zkClient != null) {
            zkClient.close();
        }
    }
}
