package com.example.couponcore.service;

import static com.example.couponcore.exception.ErrorCode.FAIL_COUPON_REQUEST;
import static com.example.couponcore.util.CouponRedisUtil.getIssueRequestKey;
import static com.example.couponcore.util.CouponRedisUtil.getIssueRequestQueueKey;

import com.example.couponcore.component.DistributeLockExecutor;
import com.example.couponcore.exception.CouponIssueException;
import com.example.couponcore.repository.redis.RedisRepository;
import com.example.couponcore.repository.redis.dto.CouponIssueRequest;
import com.example.couponcore.repository.redis.dto.CouponRedisEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncCouponIssueServiceV1 {

    private final RedisRepository redisRepository;
    private final CouponIssueRedisService couponIssueRedisService;
    private final CouponCacheService couponCacheService;
    private final DistributeLockExecutor distributeLockExecutor;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void issue(long couponId, long userId) {
        //쿠폰 캐시를 통한 유효성 검증을 캐시 처리. 엔티티 필드를 가져오는 부분을 캐시 처리
        CouponRedisEntity coupon = couponCacheService.getCouponCache(couponId);
        //캐시로 날짜 유효성 검사
        coupon.checkIssuableCoupon();
        //동시성 제어를 위한 락
        distributeLockExecutor.execute("lock_%s".formatted(couponId), 3000, 3000, () -> {
            //수량 검증
            couponIssueRedisService.checkCouponIssueQuantity(coupon, userId);
            //쿠폰 발급 queue 에 적재
            issueRequest(couponId, userId);
        });
    }


    //유저 요청 set에 저장, queue 에 적재
    private void issueRequest(long couponId, long userId) {
        CouponIssueRequest couponIssueRequest = new CouponIssueRequest(couponId, userId);
        try {
            /**
             * value = 직렬화. CouponIssueRequest -> String
             * sAdd : set 에 해당 하는 요청 추가
             * rPush: 쿠폰 발급 queue 에 적재
             *
             * sAdd, rPush 는 목적이 다르므로, key 를 다르게 설정 해야함
             *
             * 이 과정을 통해 쿠폰 발급 요청을 처리, 발급 대상을 Queue 에 쌓고,
             * 이후에는 Queue 에 쌓은 데이터를 쿠폰 발급 서버에서 트랜잭션 처리 할 수 있도록 구현
             */
            String value = objectMapper.writeValueAsString(couponIssueRequest);
            //쿠폰 발급 요청 저장
            redisRepository.sAdd(getIssueRequestKey(couponId), String.valueOf(userId));
            //쿠폰 발급 큐 적재
            redisRepository.rPush(getIssueRequestQueueKey(), value);
        } catch (JsonProcessingException e) {
            throw new CouponIssueException(FAIL_COUPON_REQUEST,
                    "input: %s".formatted(couponIssueRequest));
        }
    }
}
/*
SORTED SET
 //1. 유저의 요청을 sorted set 에 적재
        String key = "issue.request.sorted_set.couponId=%s".formatted(couponId);
        //value 는 userId를 활용해 생성
        redisRepository.zAdd(key, String.valueOf(userId), System.currentTimeMillis());

        //2. 유저의 요청 순서를 조회
        //3. 조회 결과를 선착순 조건과 비교
        //4. 쿠폰 발급 Queue 에 적재.

 */