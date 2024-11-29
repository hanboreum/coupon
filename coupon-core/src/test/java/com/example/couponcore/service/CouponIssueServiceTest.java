package com.example.couponcore.service;

import static org.junit.jupiter.api.Assertions.*;

import com.example.couponcore.TestConfig;
import com.example.couponcore.exception.CouponIssueException;
import com.example.couponcore.exception.ErrorCode;
import com.example.couponcore.model.Coupon;
import com.example.couponcore.model.CouponIssue;
import com.example.couponcore.model.CouponType;
import com.example.couponcore.repository.mysql.CouponIssueJpaRepository;
import com.example.couponcore.repository.mysql.CouponIssueRepository;
import com.example.couponcore.repository.mysql.CouponJpaRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CouponIssueServiceTest extends TestConfig {

    @Autowired
    CouponIssueService sut;

    @Autowired
    CouponIssueJpaRepository couponIssueJpaRepository;

    @Autowired
    CouponJpaRepository couponJpaRepository;

    @Autowired
    CouponIssueRepository couponIssueRepository;

    @BeforeEach
    void clean() {
        couponIssueJpaRepository.deleteAllInBatch();
        couponJpaRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("쿠폰 발급 내역 존재시 예외 반환")
    void saveCouponIssue_1() {
        //given
        CouponIssue couponIssue = CouponIssue.builder()
                .couponId(1L)
                .userId(1L)
                .build();
        couponIssueJpaRepository.save(couponIssue);

        //when
        CouponIssueException exception = assertThrows(CouponIssueException.class, () -> {
            sut.saveCouponIssue(couponIssue.getCouponId(), couponIssue.getUserId());
        });
        assertEquals(exception.getErrorCode(), ErrorCode.DUPLICATED_COUPON_ISSUE);
        //then
    }

    @Test
    @DisplayName("쿠폰 발급 내역 미존재시 쿠폰 발급")
    void saveCouponIssue_2() {
        //given
        long couponId = 1L;
        long userId = 1L;
        //when
        CouponIssue result = sut.saveCouponIssue(couponId, userId);
        //then
        assertTrue(couponIssueJpaRepository.findById(result.getId()).isPresent());
    }

    @Test
    @DisplayName("발급 수량, 기한, 중복 발급 문제가 없다면 쿠폰 발급")
    void issue_1() {
        LocalDateTime now = LocalDateTime.now();
        //given
        long userId = 1L;
        // 발급이 유효한 쿠폰
        Coupon coupon = Coupon.builder()
                .couponType(CouponType.FIRST_COME_FIRST_SERVED)
                .title("선착순 쿠폰 테스트")
                .totalQuantity(100)
                .issuedQuantity(0)
                .dateIssueStart(now.minusDays(1))
                .dateIssueEnd(now.plusDays(1))
                .build();
        couponJpaRepository.save(coupon);

        //when
        sut.issue(coupon.getId(), userId);

        //then
        Coupon result = couponJpaRepository.findById(coupon.getId()).get();
        //발급에 성공 했다면 발급 수량이 1개 늘어난다.
        assertEquals(result.getIssuedQuantity(), 1);

        CouponIssue issueResult = couponIssueRepository.findFirstCouponIssue(coupon.getId(),
                userId);
        assertNotNull(issueResult);
    }

    @Test
    @DisplayName("발급 수량에 문제가 있다면 예외")
    void issue_2() {
        LocalDateTime now = LocalDateTime.now();
        //given
        long userId = 1L;
        // 발급이 유효한 쿠폰
        Coupon coupon = Coupon.builder()
                .couponType(CouponType.FIRST_COME_FIRST_SERVED)
                .title("선착순 쿠폰 테스트")
                .totalQuantity(100)
                .issuedQuantity(100)
                .dateIssueStart(now.minusDays(1))
                .dateIssueEnd(now.plusDays(1))
                .build();
        couponJpaRepository.save(coupon);

        //when & then
        CouponIssueException exception = assertThrows(CouponIssueException.class,()->{
            sut.issue(coupon.getId(), userId);
        });
        assertEquals(exception.getErrorCode(), ErrorCode.INVALID_COUPON_ISSUE_QUANTITY);
    }

    @Test
    @DisplayName("발급 기한에 문제가 있다면 예외")
    void issue_3() {
        LocalDateTime now = LocalDateTime.now();
        //given
        long userId = 1L;
        // 발급이 유효한 쿠폰
        Coupon coupon = Coupon.builder()
                .couponType(CouponType.FIRST_COME_FIRST_SERVED)
                .title("선착순 쿠폰 테스트")
                .totalQuantity(100)
                .issuedQuantity(0)
                .dateIssueStart(now.plusDays(2))
                .dateIssueEnd(now.plusDays(1))
                .build();
        couponJpaRepository.save(coupon);

        //when & then
        CouponIssueException exception = assertThrows(CouponIssueException.class,()->{
            sut.issue(coupon.getId(), userId);
        });
        assertEquals(exception.getErrorCode(), ErrorCode.INVALID_COUPON_ISSUE_DATE);
    }

    @Test
    @DisplayName("중복 발급 이라면 예외")
    void issue_4() {
        LocalDateTime now = LocalDateTime.now();
        //given
        long userId = 1L;
        // 발급이 유효한 쿠폰
        Coupon coupon = Coupon.builder()
                .couponType(CouponType.FIRST_COME_FIRST_SERVED)
                .title("선착순 쿠폰 테스트")
                .totalQuantity(100)
                .issuedQuantity(0)
                .dateIssueStart(now.minusDays(1))
                .dateIssueEnd(now.plusDays(1))
                .build();
        couponJpaRepository.save(coupon);

        //발급된 내역을 저장
        CouponIssue couponIssue = CouponIssue.builder()
                .couponId(coupon.getId())
                .userId(userId)
                .build();
        couponIssueJpaRepository.save(couponIssue);

        //when & then
        CouponIssueException exception = assertThrows(CouponIssueException.class,()->{
            sut.issue(coupon.getId(), userId);
        });
        assertEquals(exception.getErrorCode(), ErrorCode.DUPLICATED_COUPON_ISSUE);
    }

    @Test
    @DisplayName("쿠폰이 존재 하지 않는다면 예외")
    void issue_5() {
        LocalDateTime now = LocalDateTime.now();
        //given
        long userId = 1L;
        long couponId = 1L;

        //when & then
        CouponIssueException exception = assertThrows(CouponIssueException.class,()->{
            sut.issue(couponId, userId);
        });
        assertEquals(exception.getErrorCode(), ErrorCode.COUPON_NOT_EXIST);
    }
}