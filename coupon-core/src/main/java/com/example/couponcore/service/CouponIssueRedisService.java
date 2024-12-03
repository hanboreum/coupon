package com.example.couponcore.service;

import static com.example.couponcore.util.CouponRedisUtil.getIssueRequestKey;

import com.example.couponcore.repository.redis.RedisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CouponIssueRedisService {

    private final RedisRepository redisRepository;

    //중복 요청 검증
    public boolean availableUserIssueQuantity(long couponId, long userId) {
        String key = getIssueRequestKey(couponId);
        return !redisRepository.sIsMember(key, String.valueOf(userId));
    }

    //수량 검증
    public boolean availableTotalIssueQuantity(Integer totalQuantity, long couponId) {

        if (totalQuantity == null) {
            return true;
        }
        String key = getIssueRequestKey(couponId);
        //set 의 요청 수가 더 작을 때만  true 를 반환
        return totalQuantity > redisRepository.sCard(key);
    }
}
