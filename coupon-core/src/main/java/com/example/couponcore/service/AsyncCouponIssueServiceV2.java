package com.example.couponcore.service;

import com.example.couponcore.repository.redis.RedisRepository;
import com.example.couponcore.repository.redis.dto.CouponRedisEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncCouponIssueServiceV2 {

    private final RedisRepository redisRepository;
    private final CouponCacheService couponCacheService;

    private final ObjectMapper objectMapper = new ObjectMapper();


    //이 로직에 따라 RPS 가 결정된다.
    public void issue(long couponId, long userId) {
        //쿠폰 캐시를 통한 유효성 검증을 캐시 처리. 엔티티 필드를 가져오는 부분을 캐시 처리
        //issue 호출 시 local cache 가 생성. local cache 가 존재하는 동안에는 redis 룰 참조하지 않음
        //redis 로 들어가는 traffic 줄어든다.
        CouponRedisEntity coupon = couponCacheService.getCouponLocalCache(couponId);
        //캐시로 날짜 유효성 검사
        coupon.checkIssuableCoupon();
        issueRequest(couponId, userId, coupon.totalQuantity());
    }

    //redis 를 사용해 최적화 함
    private void issueRequest(long couponId, long userId, Integer totalIssueQuantity) {
        if (totalIssueQuantity == null) {
            //max 를 넘겨 검증 우회
            redisRepository.issueRequest(couponId, userId, Integer.MAX_VALUE);
        }
        redisRepository.issueRequest(couponId, userId, totalIssueQuantity);
    }
}
/**
 * public void issue(){
 * 1. 로컬 캐시로 엔티티 가져옴
 * CouponRedisEntity coupon = couponCacheService.getCouponLocalCache(couponId);
 * 2. validation 진행
 * coupon.checkIssuableCoupon();
 * 3. issue request 를 통해 쿠폰 발급 대기열에 적재와 동시에
 * CouponIssueService 에서 issue method (쿠폰 발급) 의 트랜잭션 실행. 쿠폰 발급 서버서 실행
 * 다음으로 publishCouponEvent 실행. 쿠폰 발급 완료시 이벤트를 발급 시킴.
 * 이 이벤트는 CouponEventListener - issueComplete Listener 가 잡는다.해당하는 트랜잭션이 커밋되고(db에 반영되고)
 * issue.complete 로그를 출력하고 cache 를 update 해준다.
 * issueRequest(couponId, userId, coupon.totalQuantity()); }
 */