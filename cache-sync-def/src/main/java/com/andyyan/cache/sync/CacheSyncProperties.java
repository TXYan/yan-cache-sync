package com.andyyan.cache.sync;

/**
 * Create by yantingxin 2018/6/12
 */
public enum CacheSyncProperties {

    BIZ_KEY("biz-key"),
    BIZ_DATA("biz-data"),
    ;

    private String desc;

    CacheSyncProperties(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }

}
