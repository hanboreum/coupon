package com.example.couponconsumer.component;

import static com.example.couponcore.util.CouponRedisUtil.getIssueRequestQueueKey;

import com.example.couponcore.repository.redis.RedisRepository;
import com.example.couponcore.repository.redis.dto.CouponIssueRequest;
import com.example.couponcore.service.CouponIssueService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@EnableScheduling
@Component
@Slf4j
public class CouponIssueListener {

    private final RedisRepository redisRepository;
    //transaction 처리
    private final CouponIssueService couponIssueService;
    private final ObjectMapper objectMapper;
    private final String issueRequestQueueKey = getIssueRequestQueueKey();

    /**
     * 주기적으로 실행
     * 쿠폰 발급 대기열 queue 에 적재되어 있는 것들을 읽고 쿠폰 발급을 처리하는 기능
     * target 을 읽어옴
     */
    @Scheduled(fixedDelay = 1000)
    public void issue() throws JsonProcessingException {
        log.info("Listen...");
        while (existCouponIssueTarget()) { // 존재 한다면 쿠폰 발급
            //쿠폰 발급 대상이 있다면 여기서 처리
            CouponIssueRequest target = getIssueTarget(); //큐에서 가장 앞에 있는 index 를 가져온다.
            log.info("발급 시작 target:{}", target);
            couponIssueService.issue(target.couponId(), target.userId()); //쿠폰 발급 트랜잭션 처리
            log.info("발급 완료 target: {}", target);
            removeIssuedTarget(); //발급 종료 시 remove
        }
    }

    private boolean existCouponIssueTarget() {
        //redis repo 에서 큐의 사이즈를 보면 된다.
        //0 보다 크면 대상 존재 한다는 것.
        return redisRepository.lSize(issueRequestQueueKey) > 0;
    }

    //target 이 있다면 redis 에서 가져오기
    private CouponIssueRequest getIssueTarget() throws JsonProcessingException {
        return objectMapper.readValue(redisRepository.lIndex(issueRequestQueueKey, 0), CouponIssueRequest.class);
    }

    private void removeIssuedTarget() {
        redisRepository.lPop(issueRequestQueueKey);
    }
}