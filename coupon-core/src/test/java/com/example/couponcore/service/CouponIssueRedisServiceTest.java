package com.example.couponcore.service;

import static com.example.couponcore.util.CouponRedisUtil.getIssueRequestKey;
import static org.junit.jupiter.api.Assertions.*;

import com.example.couponcore.TestConfig;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

class CouponIssueRedisServiceTest extends TestConfig {

    @Autowired
    CouponIssueRedisService sut;

    @Autowired
    RedisTemplate<String, String> redisTemplate;

    @BeforeEach
    void clear(){
        Collection<String> redisKey = redisTemplate.keys("*");
        redisTemplate.delete(redisKey);
    }

    @Test
    @DisplayName("쿠폰 수량 검증 - 발급 가능 수량이 존재하면 true")
    void availableTotalQuantity_1(){
        //given
        int totalIssueQuantity = 10;
        long couponId =1L;
        //when
        boolean result = sut.availableTotalIssueQuantity(totalIssueQuantity, couponId);
        //then
        assertTrue(result);
    }

    @Test
    @DisplayName("쿠폰 수량 검증 - 발급 가능 수량이 소진되면 false")
    void availableTotalQuantity_2(){
        //given
        int totalIssueQuantity = 10;
        long couponId =1L;
        //when
        IntStream.range(0, totalIssueQuantity).forEach(userId ->{
            redisTemplate.opsForSet().add(getIssueRequestKey(couponId), String.valueOf(userId));
        });
        // when
        boolean result = sut.availableTotalIssueQuantity(totalIssueQuantity, couponId);
        //then
        assertFalse(result);
    }

    @Test
    @DisplayName("쿠폰 중복 발급 검증 - 발급된 내역에 유저가 존재 하면 false")
    void availableTotalQuantity_3(){
        //given
        long couponId =1L;
        long userId =1L;
        redisTemplate.opsForSet().add(getIssueRequestKey(couponId), String.valueOf(userId));
        //when
        boolean result = sut.availableUserIssueQuantity(couponId, userId);
        //then
        assertFalse(result);
    }
}