package com.example.couponcore.service;

import static com.example.couponcore.exception.ErrorCode.DUPLICATED_COUPON_ISSUE;
import static com.example.couponcore.exception.ErrorCode.INVALID_COUPON_ISSUE_QUANTITY;
import static com.example.couponcore.util.CouponRedisUtil.getIssueRequestKey;

import com.example.couponcore.exception.CouponIssueException;
import com.example.couponcore.repository.redis.RedisRepository;
import com.example.couponcore.repository.redis.dto.CouponRedisEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CouponIssueRedisService {

    private final RedisRepository redisRepository;

    public void checkCouponIssueQuantity(CouponRedisEntity couponRedisEntity,
            long userId) {
        Long id = couponRedisEntity.id();
        Integer totalQuantity = couponRedisEntity.totalQuantity();
        if (!availableTotalIssueQuantity(totalQuantity, id)) {
            throw new CouponIssueException(INVALID_COUPON_ISSUE_QUANTITY,
                    "발급 가능한 수량을 초과함. couponId: %s, userId : %s".formatted(id, userId));
        }
        if (!availableUserIssueQuantity(id, userId)) {
            throw new CouponIssueException(DUPLICATED_COUPON_ISSUE,
                    "이미 발급 요청이 처리 되었음. couponId: %s, userId : %s".formatted(id, userId));
        }
    }

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
