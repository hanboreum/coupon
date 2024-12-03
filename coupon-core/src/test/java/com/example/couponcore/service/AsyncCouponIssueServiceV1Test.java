package com.example.couponcore.service;

import static com.example.couponcore.exception.ErrorCode.COUPON_NOT_EXIST;
import static com.example.couponcore.exception.ErrorCode.DUPLICATED_COUPON_ISSUE;
import static com.example.couponcore.exception.ErrorCode.INVALID_COUPON_ISSUE_DATE;
import static com.example.couponcore.exception.ErrorCode.INVALID_COUPON_ISSUE_QUANTITY;
import static com.example.couponcore.util.CouponRedisUtil.getIssueRequestKey;
import static com.example.couponcore.util.CouponRedisUtil.getIssueRequestQueueKey;
import static org.junit.jupiter.api.Assertions.*;

import com.example.couponcore.TestConfig;
import com.example.couponcore.exception.CouponIssueException;
import com.example.couponcore.model.Coupon;
import com.example.couponcore.model.CouponType;
import com.example.couponcore.repository.mysql.CouponJpaRepository;
import com.example.couponcore.repository.redis.dto.CouponIssueRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

class AsyncCouponIssueServiceV1Test extends TestConfig {

    @Autowired
    AsyncCouponIssueServiceV1 sut;

    @Autowired
    RedisTemplate<String, String> redisTemplate;

    @Autowired
    CouponJpaRepository couponJpaRepository;

    @BeforeEach
    void clear() {
        Collection<String> redisKey = redisTemplate.keys("*");
        redisTemplate.delete(redisKey);
    }

    @Test
    @DisplayName("쿠폰 발급 - 쿠폰이 존재하지 않는다면 예외를 반환한다")
    void issue_1() {
        //given
        long couponId = 1;
        long userId = 1;
        //when& then
        CouponIssueException exception = assertThrows(CouponIssueException.class, () -> {
            sut.issue(couponId, userId);
        });
        assertEquals(exception.getErrorCode(), COUPON_NOT_EXIST);
    }

    @Test
    @DisplayName("쿠폰 발급 - 발급 가능 수량이 존재 하지 않으면 예외를 반환한다")
    void issue_2() {
        LocalDateTime now = LocalDateTime.now();
        //given
        long userId = 1000L;
        Coupon coupon = Coupon.builder()
                .couponType(CouponType.FIRST_COME_FIRST_SERVED)
                .title("선착순 테스트 쿠폰")
                .totalQuantity(10)
                .issuedQuantity(0)
                .dateIssueStart(now.minusDays(1))
                .dateIssueEnd(now.plusDays(1))
                .build();
        couponJpaRepository.save(coupon);
        //redis 에 값을 미리 저장.
        IntStream.range(0, coupon.getTotalQuantity()).forEach(idx -> {
            redisTemplate.opsForSet().add(getIssueRequestKey(coupon.getId()), String.valueOf(idx));
        });
        //when & then
        CouponIssueException exception = assertThrows(CouponIssueException.class, () -> {
            sut.issue(coupon.getId(), userId);
        });
        assertEquals(exception.getErrorCode(), INVALID_COUPON_ISSUE_QUANTITY);
    }

    @Test
    @DisplayName("쿠폰 발급 - 이미 발급된 유저라면 예외를 반환한다")
    void issue_3() {
        LocalDateTime now = LocalDateTime.now();
        //given
        long userId = 1L;
        Coupon coupon = Coupon.builder()
                .couponType(CouponType.FIRST_COME_FIRST_SERVED)
                .title("선착순 테스트 쿠폰")
                .totalQuantity(10)
                .issuedQuantity(0)
                .dateIssueStart(now.minusDays(1))
                .dateIssueEnd(now.plusDays(1))
                .build();
        couponJpaRepository.save(coupon);
        redisTemplate.opsForSet().add(getIssueRequestKey(coupon.getId()), String.valueOf(userId));

        //when & then
        CouponIssueException exception = assertThrows(CouponIssueException.class, () -> {
            sut.issue(coupon.getId(), userId);
        });
        assertEquals(exception.getErrorCode(), DUPLICATED_COUPON_ISSUE);
    }

    @Test
    @DisplayName("쿠폰 발급 - 발급 가능 수량이 존재 하지 않으면 예외를 반환한다")
    void issue_4() {
        LocalDateTime now = LocalDateTime.now();
        //given
        long userId = 1L;
        Coupon coupon = Coupon.builder()
                .couponType(CouponType.FIRST_COME_FIRST_SERVED)
                .title("선착순 테스트 쿠폰")
                .totalQuantity(10)
                .issuedQuantity(0)
                .dateIssueStart(now.plusDays(1))
                .dateIssueEnd(now.plusDays(2))
                .build();
        couponJpaRepository.save(coupon);
        redisTemplate.opsForSet().add(getIssueRequestKey(coupon.getId()), String.valueOf(userId));

        //when & then
        CouponIssueException exception = assertThrows(CouponIssueException.class, () -> {
            sut.issue(coupon.getId(), userId);
        });

        assertEquals(exception.getErrorCode(), INVALID_COUPON_ISSUE_DATE);
    }

    @Test
    @DisplayName("쿠폰 발급 - 쿠폰 발급을 기록 한다")
    void issue_5() {
        LocalDateTime now = LocalDateTime.now();
        //given
        long userId = 1L;
        Coupon coupon = Coupon.builder()
                .couponType(CouponType.FIRST_COME_FIRST_SERVED)
                .title("선착순 테스트 쿠폰")
                .totalQuantity(10)
                .issuedQuantity(0)
                .dateIssueStart(now.minusDays(1))
                .dateIssueEnd(now.plusDays(2))
                .build();
        couponJpaRepository.save(coupon);
        //when
        sut.issue(coupon.getId(), userId);
        //then
        Boolean isSaved = redisTemplate.opsForSet()
                .isMember(getIssueRequestKey(coupon.getId()), String.valueOf(userId));
        assertTrue(isSaved);
    }

    @Test
    @DisplayName("쿠폰 발급 - 쿠폰 발급 요청이 성공하면 쿠폰 발급 큐에 적재한다.")
    void issue_6() throws JsonProcessingException {
        LocalDateTime now = LocalDateTime.now();
        //given
        long userId = 1L;
        Coupon coupon = Coupon.builder()
                .couponType(CouponType.FIRST_COME_FIRST_SERVED)
                .title("선착순 테스트 쿠폰")
                .totalQuantity(10)
                .issuedQuantity(0)
                .dateIssueStart(now.minusDays(1))
                .dateIssueEnd(now.plusDays(1))
                .build();
        couponJpaRepository.save(coupon);
        CouponIssueRequest request = new CouponIssueRequest(coupon.getId(), userId);
        //when
        sut.issue(coupon.getId(), userId);
        //then
        String savedIssueRequest = redisTemplate.opsForList().leftPop(getIssueRequestQueueKey());
        assertEquals(new ObjectMapper().writeValueAsString(request), savedIssueRequest);
    }
}