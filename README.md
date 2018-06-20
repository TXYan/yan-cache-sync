## 缓存实时通知机制
项目中很多地方使用到了内存缓存，在集群环境中，数据变更后会导致机器数据不一致问题，此项目就是为了解决此问题,达到实时更新的目的，避免因数据不一致导致莫名奇妙的问题，另外还大大减小了QA测试等待时间，提高测试效率。
目前提供了rocketmq机制和zookeeper机制，还可以扩展redis机制，以及其他
rocketmq、redis可以使用 zookeeper在并发情况会丢消息

### rocket mq 参数说明
- 共用
    - cache.sync.nameServer 服务的地址
    - cache.sync.instanceName 实例名称标识
    - cache.sync.topic topic
- 生产者
    - cache.sync.producer.group 生产者的组 
    - cache.sync.producer.queueNum 对应 defaultTopicQueueNums属性
    - cache.sync.producer.retryTimes 对应retryTimesWhenSendFailed属性
- 消费者
    - cache.sync.consumer.group 消费者组
    - cache.sync.consumer.batchMax 对应 consumeConcurrentlyMaxSpan 属性
    - cache.sync.consumer.concurrentMaxSpan 对应 consumeConcurrentlyMaxSpan 属性
    - cache.sync.consumer.threadMin 对应 consumeThreadMin属性
    - cache.sync.consumer.threadMax 对应consumeThreadMax 属性
    - cache.sync.consumer.tags 对应订阅的tag列表，这里代表关心的服务
    
- 配置文件示例 （开发环境，其实为了方便copy）
cache.sync.nameServer=192.168.1.24:9876
cache.sync.instanceName=cache-sync
cache.sync.topic=BOSS_TP03
cache.sync.producer.group=BOSS_TP03_P
cache.sync.producer.queueNum=4
cache.sync.producer.retryTimes=2
cache.sync.consumer.group=BOSS_TP03_C
cache.sync.consumer.batchMax=10
cache.sync.consumer.concurrentMaxSpan=5000
cache.sync.consumer.threadMin=2
cache.sync.consumer.threadMax=10
cache.sync.consumer.tags=*

### 使用示例

1、引入包
<dependency>
    <groupId>com.andyyan</groupId>
    <artifactId>rocketmq-cache-sync</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>

2、引入对象
    @Autowired
    private CacheSync cacheSync;

3、注册本地缓存更新事件处理
server是当前服务名字 key代表本地缓存的标识 notify的data是接收的数据
cacheSync.subscribe(server, key, new CacheSyncNotify() {
    @Override
    public void notify(String data) {
        System.out.println("TestApiController cacheSyncSubscribe server:" + server + ", key:" + key + ", data:" + s);
    }
});

4、发布更新事件
cacheSync.publish(server, key, data);
