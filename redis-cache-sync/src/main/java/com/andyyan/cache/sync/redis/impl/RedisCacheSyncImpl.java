package com.andyyan.cache.sync.redis.impl;

import com.andyyan.cache.sync.AbstractCacheSync;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.*;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Create by yantingxin 2018/5/30
 */
public class RedisCacheSyncImpl extends AbstractCacheSync {

    private static final Logger log = LoggerFactory.getLogger(RedisCacheSyncImpl.class);

    private static final int DEFAULT_PORT = 6379;
    private static final String ADDRESS_SPIT = "\\s*(,|，)\\s*";
    private static final String HOST_PORT_SPIT = ":";
    private static final String KEY_DATA_SPIT = ":";

    private Jedis subJedis;
    JedisPubSub pubSub;
    private String channel;
    JedisPool jedisPool;

    private ExecutorService fixPool = new ThreadPoolExecutor(1, 1, 1, TimeUnit.DAYS, new ArrayBlockingQueue<Runnable>(1));

    public RedisCacheSyncImpl(String host, int port, int toal, int timeout, String channel) {
        this.channel = channel;
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(toal);
        config.setTestOnBorrow(true);
        jedisPool = new JedisPool(config, host, port, timeout);
        subJedis = jedisPool.getResource();

        pubSub = new JedisPubSub() {
            @Override
            public void onMessage(String channel, String message) {
                log.debug("RedisCacheSyncImpl.onMessage(" + "channel = " + channel + ", message = " + message + ")");
                int idx = message.indexOf(KEY_DATA_SPIT);
                if (idx < 0) {
                    log.warn("RedisCacheSyncImpl.onMessage(" + "channel = " + channel + ", message = " + message + "), ignore");
                    return;
                }
                String key = message.substring(0, idx);
                String data = message.substring(idx + 1);
                syncNotify(channel, key, data);
            }
        };

        subscribeChannel(channel);
    }

    private Set<HostAndPort> generateHostAndPortSet(String hostsAndPorts) {
        if (StringUtils.isBlank(hostsAndPorts)) {
            return new HashSet<HostAndPort>(0);
        }
        Set<HostAndPort> hostAndPortSet = new HashSet<HostAndPort>(10);
        String[] hostAndPortArr = hostsAndPorts.split(ADDRESS_SPIT);
        for (int idx = 0; idx < hostAndPortArr.length; idx++) {
            HostAndPort hostAndPort = generateHostAndPort(hostAndPortArr[idx]);
            if (hostAndPort != null) {
                hostAndPortSet.add(hostAndPort);
            }
        }
        return hostAndPortSet;
    }

    private HostAndPort generateHostAndPort(String hostAndPort) {
        if (StringUtils.isBlank(hostAndPort)) {
            return null;
        }
        String[] hostPortArr = hostAndPort.split(HOST_PORT_SPIT);
        if (hostPortArr.length <= 0 || hostPortArr.length > 2) {
            return null;
        }
        String host = hostPortArr[0];
        int port = DEFAULT_PORT;
        if (hostPortArr.length == 2) {
            port = NumberUtils.toInt(hostPortArr[1], DEFAULT_PORT);
        }
        return new HostAndPort(host, port);
    }

    public void publish(String serverName, String key, String data) {
        Jedis pubJedis = null;
        try {
            pubJedis = jedisPool.getResource();
            pubJedis.publish(channel, key + KEY_DATA_SPIT + data);
        } catch (Exception e) {
            log.error("RedisCacheSyncImpl.publish(" + "key = " + key + ", data = " + data + ")", e);
        } finally {
            if (pubJedis != null) {
                jedisPool.returnResource(pubJedis);
            }
        }
    }

    public void publishAsync(String serverName, String key, String data) {
        throw new UnsupportedOperationException("Not Supported async");
    }

    private void subscribeChannel(final String channel) {

        fixPool.execute(new Runnable() {
            public void run() {
                subJedis.subscribe(pubSub, channel);
            }
        });
    }

    @Override
    public void destroy() {
        try {
            super.destroy();
            if (subJedis != null) {
                pubSub.unsubscribe(channel);
                jedisPool.returnResource(subJedis);
            }

            jedisPool.destroy();
            fixPool.shutdown();
        } catch (Exception e) {
            log.error("RedisCacheSyncImpl.destroy()", e);
        }

    }
}
