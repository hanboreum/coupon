package com.example.couponcore.util;

public class CouponRedisUtil {

    public static String getIssueRequestKey(long couponId) {
        //couponId 에 따라 redis cache key 가 만들어짐
        return "issue.request.couponId=%s".formatted(couponId);
    }

    public static String getIssueRequestQueueKey() {
        //couponId 에 따라 redis cache key 가 만들어짐
        return "issue.request";
    }
}
