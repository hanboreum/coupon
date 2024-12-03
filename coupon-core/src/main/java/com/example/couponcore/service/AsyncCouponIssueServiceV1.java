package com.example.couponcore.service;

import static com.example.couponcore.exception.ErrorCode.DUPLICATED_COUPON_ISSUE;
import static com.example.couponcore.exception.ErrorCode.FAIL_COUPON_REQUEST;
import static com.example.couponcore.exception.ErrorCode.INVALID_COUPON_ISSUE_DATE;
import static com.example.couponcore.exception.ErrorCode.INVALID_COUPON_ISSUE_QUANTITY;
import static com.example.couponcore.util.CouponRedisUtil.getIssueRequestKey;
import static com.example.couponcore.util.CouponRedisUtil.getIssueRequestQueueKey;

import com.example.couponcore.component.DistributeLockExecutor;
import com.example.couponcore.exception.CouponIssueException;
import com.example.couponcore.model.Coupon;
import com.example.couponcore.repository.redis.RedisRepository;
import com.example.couponcore.repository.redis.dto.CouponIssueRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AsyncCouponIssueServiceV1 {

    private final RedisRepository redisRepository;
    private final CouponIssueRedisService couponIssueRedisService;
    private final CouponIssueService couponIssueService;
    private final DistributeLockExecutor distributeLockExecutor;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void issue(long couponId, long userId) {
        Coupon coupon = couponIssueService.findCoupon(couponId);
        Integer totalQuantity = coupon.getTotalQuantity();
        if (!coupon.availableIssueDate()) {
            throw new CouponIssueException(INVALID_COUPON_ISSUE_DATE,
                    " 발급 기간이 유효 하지 않음, couponId: %s, issueStart:%s, issueEnd:%s".formatted(couponId,
                            coupon.getDateIssueStart(), coupon.getDateIssueEnd()));
        }
        distributeLockExecutor.execute("lock_%s".formatted(couponId), 3000, 3000, () -> {
            if (!couponIssueRedisService.availableTotalIssueQuantity(totalQuantity, couponId)) {
                throw new CouponIssueException(INVALID_COUPON_ISSUE_QUANTITY,
                        "발급 가능한 수량을 초과함. couponId: %s, userId : %s".formatted(couponId, userId));
            }
            if (!couponIssueRedisService.availableUserIssueQuantity(couponId, userId)) {
                throw new CouponIssueException(DUPLICATED_COUPON_ISSUE,
                        "이미 발급 요청이 처리 되었음. couponId: %s, userId : %s".formatted(couponId, userId));
            }
            issueRequest(couponId, userId);
        });
    }

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
            redisRepository.sAdd(getIssueRequestKey(couponId), String.valueOf(userId));

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