package com.example.couponcore.component;

import com.example.couponcore.model.event.CouponIssueCompleteEvent;
import com.example.couponcore.service.CouponCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@RequiredArgsConstructor
@Component
@Slf4j
public class CouponEventListener {

    private final CouponCacheService couponCacheService;

    /**
     * issueComplete 가 요청 되면 해당하는 트랜잭션이 커밋 된 다음 listener 가 실행된다.
     * redis cache update, local cache update 가 진행된다.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void issueComplete(CouponIssueCompleteEvent event) {
        log.info("Issue complete. Cache refresh start coupon Id : {}", event.couponId());
        couponCacheService.putCouponCache(event.couponId());
        couponCacheService.putCouponLocalCache(event.couponId());
        log.info("Issue complete. Cache refresh end coupon Id : {}", event.couponId());
    }
}
