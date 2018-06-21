package com.andyyan.cache.sync.zookeeper.impl;

import com.andyyan.cache.sync.AbstractCacheSync;
import com.andyyan.cache.sync.CacheSyncNotify;
import org.I0Itec.zkclient.IZkDataListener;
import org.I0Itec.zkclient.ZkClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Create by yantingxin 2018/6/12
 */
public class ZookeeperCacheSyncImpl extends AbstractCacheSync {

    private static final Logger log = LoggerFactory.getLogger(ZookeeperCacheSyncImpl.class);

    private static final String PATH_SEPARATOR = "/";
    private static final String PATH_NODE_NAME_SEPARATOR = "-";

    private ConcurrentHashMap<String, Boolean> initMap = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, AtomicLong> atomicMap = new ConcurrentHashMap<>();
    private String path;
    private int dataNodeSize = 20;
    private ZkClient zkClient;
    private IZkDataListener zkDataListener;

    public ZookeeperCacheSyncImpl(String hosts, int timeout, String path, int dataNodeSize) {
        this.path = path;
        this.dataNodeSize = dataNodeSize;
        zkClient = new ZkClient(hosts, timeout);
        if (!zkClient.exists(path)) {
            zkClient.createPersistent(path, true);
        }

        zkDataListener = new IZkDataListener() {
            public void handleDataChange(String path, Object data) throws Exception {
                int spitIdx = path.lastIndexOf(PATH_SEPARATOR);
                String parentPath = path.substring(0, spitIdx);

                String key = parentPath;
                spitIdx = parentPath.lastIndexOf(PATH_SEPARATOR);
                if (spitIdx >= 0) {
                    key = key.substring(spitIdx + 1, key.length());
                }

                String gPath = parentPath.substring(0, spitIdx);
                String serverName = gPath.substring(gPath.lastIndexOf(PATH_SEPARATOR) + 1);
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
            Boolean hasInit = initMap.get(keyPath);
            if (hasInit != null && hasInit) {
                return;
            }
            String dataNodePath = keyPath + PATH_SEPARATOR + key + PATH_NODE_NAME_SEPARATOR;
            for (int idx = 0; idx < dataNodeSize; idx++) {
                zkClient.createEphemeral(dataNodePath + idx, true);
                zkClient.subscribeDataChanges(dataNodePath + idx, zkDataListener);
            }
            initMap.put(key, true);
            atomicMap.putIfAbsent(keyPath, new AtomicLong(0));
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
            initMap.put(key, false);
        } catch (Exception e) {
            log.error("ZookeeperCacheSyncImpl.unsubscribe(" + "serverName = " + serverName + ", key = " + key + ")", e);
        }
    }

    public void publish(String serverName, String key, String data) {
        try {
            String keyPath = getPath(path, serverName, key);
            AtomicLong atomicLong = atomicMap.get(keyPath);
            if (atomicLong == null) {
                log.error("ZookeeperCacheSyncImpl.publish(" + "serverName = " + serverName + ", key = " + key + ", data = " + data + "), atomicMap not has key:" + keyPath);
                return;
            }
            keyPath = getPath(keyPath, key) + PATH_NODE_NAME_SEPARATOR + (atomicLong.getAndIncrement() % dataNodeSize);
            zkClient.writeData(keyPath, data);
        } catch (Exception e) {
            log.error("ZookeeperCacheSyncImpl.publish(" + "serverName = " + serverName + ", key = " + key + ", data = " + data + ")", e);
        }
    }

    private String getPath(String ... names) {
        StringBuilder sb = new StringBuilder();
        for (int idx = 0; idx < names.length; idx++) {
            sb.append(names[idx]).append(PATH_SEPARATOR);
        }
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
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
