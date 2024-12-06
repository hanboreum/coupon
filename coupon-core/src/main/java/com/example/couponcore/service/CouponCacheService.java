package com.example.couponcore.service;

import com.example.couponcore.model.Coupon;
import com.example.couponcore.repository.redis.dto.CouponRedisEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.aop.framework.AopContext;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class CouponCacheService {

    private final CouponIssueService couponIssueService;

    @Cacheable(cacheNames = "coupon")
    public CouponRedisEntity getCouponCache(long couponId) {
        Coupon coupon = couponIssueService.findCoupon(couponId);
        //위 coupon 을 cache 생성자에 전달
        return new CouponRedisEntity(coupon);
    }

    //기본적으로 local cache 에 있으면 local cache 를 반환 .
    //없다면 redis cache 에서 반환
    @Cacheable(cacheNames = "coupon", cacheManager = "LocalCacheManager")
    public CouponRedisEntity getCouponLocalCache(long couponId) {
        return proxy().getCouponCache(couponId);
    }

    //cache update
    @CachePut(cacheNames = "coupon")
    public CouponRedisEntity putCouponCache(long couponId) {
        return getCouponCache(couponId);
    }

    //local cache update
    @CachePut(cacheNames = "coupon", cacheManager = "LocalCacheManager")
    public CouponRedisEntity putCouponLocalCache(long couponId) {
        return getCouponLocalCache(couponId);
    }

    private CouponCacheService proxy() {
        return ((CouponCacheService) AopContext.currentProxy());
    }
}
