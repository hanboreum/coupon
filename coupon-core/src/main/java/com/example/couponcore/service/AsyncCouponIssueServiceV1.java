package com.example.couponcore.service;

import com.example.couponcore.repository.redis.RedisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AsyncCouponIssueServiceV1 {

    private final RedisRepository redisRepository;

    @Transactional
    public void issue(long couponId, long userId) {
        //1. 유저의 요청을 sorted set 에 적재
        String key = "issue.request.sorted_set.couponId=%s".formatted(couponId);
        //value 는 userId를 활용해 생성
        redisRepository.zAdd(key, String.valueOf(userId), System.currentTimeMillis());

        //2. 유저의 요청 순서를 조회
        //3. 조회 결과를 선착순 조건과 비교
        //4. 쿠폰 발급 Queue 에 적재.
    }
}
