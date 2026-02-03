package com.maximum0.fastpickbe.coupon.infra.redis;

public class CouponRedisSchema {
    public static final String STOCK = "coupon:%d:stock";
    public static final String EXPIRE_AT = "coupon:%d:expire_at";
    public static final String USER_SET = "coupon:%d:users";
    public static final String WAITING_QUEUE = "coupon:%d:waiting";
    public static final String ACTIVE_QUEUES = "active_coupon_queues";

    public static String getStockKey(Long id) { return String.format(STOCK, id); }
    public static String getExpireAt(Long id) { return String.format(EXPIRE_AT, id); }
    public static String getUserSetKey(Long id) { return String.format(USER_SET, id); }
    public static String getWaitingKey(Long id) { return String.format(WAITING_QUEUE, id); }
    public static String getActiveQueuesKey() { return ACTIVE_QUEUES; }
}
