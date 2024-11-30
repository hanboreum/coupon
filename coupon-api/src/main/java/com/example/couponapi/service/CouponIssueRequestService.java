package com.example.couponapi.service;

import com.example.couponapi.controller.dto.CouponIssueRequestDto;
import com.example.couponcore.component.DistributeLockExecutor;
import com.example.couponcore.service.CouponIssueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
@Slf4j
public class CouponIssueRequestService {

    private final CouponIssueService couponIssueService;
    private final DistributeLockExecutor distributeLockExecutor;

    public void issueRequestV1(CouponIssueRequestDto requestDto) {
        distributeLockExecutor.execute("lock_" + requestDto.couponId(), 10000, 10000, () -> {
            /**
             * executor 에 runnable 을 넘겨 execute 안에서 실행 할 수 있도록.
             */
            couponIssueService.issue(requestDto.couponId(), requestDto.userId());

        });
        log.info("쿠폰 발급 완료. couponId: {}, userId: {}", requestDto.couponId(), requestDto.userId());
    }
}
