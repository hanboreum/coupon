package com.example.couponcore.model;

import static org.junit.jupiter.api.Assertions.*;

import com.example.couponcore.exception.CouponIssueException;
import com.example.couponcore.exception.ErrorCode;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CouponTest {

    @Test
    @DisplayName("발급 수량이 남아 있다면 true")
    void availableIssueQuantity_1() {
        //given
        Coupon coupon = Coupon.builder()
                .totalQuantity(100)
                .issuedQuantity(99)
                .build();

        //when
        boolean result = coupon.availableIssueQuantity();

        //then
        assertTrue(result);
    }

    @Test
    @DisplayName("발급 수량이 없다면 false")
    void availableIssueQuantity_2() {
        //given
        Coupon coupon = Coupon.builder()
                .totalQuantity(100)
                .issuedQuantity(100)
                .build();

        //when
        boolean result = coupon.availableIssueQuantity();

        //then
        assertFalse(result);
    }

    @Test
    @DisplayName("최대 발급 수량이 설정 되지 않았다면 true")
    void availableIssueQuantity_3() {
        //given
        Coupon coupon = Coupon.builder()
                .totalQuantity(null)
                .issuedQuantity(100)
                .build();

        //when
        boolean result = coupon.availableIssueQuantity();

        //then
        assertTrue(result);
    }

    @Test
    @DisplayName("발급 기간이 시작 되지 않았다면 false")
    void availableIssueQuantity_4() {
        //given
        LocalDateTime now = LocalDateTime.now();

        Coupon coupon = Coupon.builder()
                .dateIssueStart(now.plusDays(1))
                .dateIssueEnd(now.plusDays(2))
                .build();

        //when
        boolean result = coupon.availableIssueDate();

        //then
        assertFalse(result);
    }

    @Test
    @DisplayName("발급 기간이 해당 되면 true")
    void availableIssueQuantity_5() {
        //given
        LocalDateTime now = LocalDateTime.now();

        Coupon coupon = Coupon.builder()
                .dateIssueStart(now.minusDays(1))
                .dateIssueEnd(now.plusDays(2))
                .build();

        //when
        boolean result = coupon.availableIssueDate();

        //then
        assertTrue(result);
    }

    @Test
    @DisplayName("발급 기간이 종료 되었다면 false")
    void availableIssueQuantity_6() {
        //given
        LocalDateTime now = LocalDateTime.now();

        Coupon coupon = Coupon.builder()
                .dateIssueStart(now.minusDays(2))
                .dateIssueEnd(now.minusDays(1))
                .build();

        //when
        boolean result = coupon.availableIssueDate();

        //then
        assertFalse(result);
    }

    @Test
    @DisplayName("발급 수량과 발급 기간이 유효 하다면 발급에 성공")
    void issue_1() {
        //given
        LocalDateTime now = LocalDateTime.now();

        Coupon coupon = Coupon.builder()
                .totalQuantity(100)
                .issuedQuantity(99)
                .dateIssueStart(now.minusDays(1))
                .dateIssueEnd(now.plusDays(2))
                .build();

        //when
        coupon.issue();

        //then
        assertEquals(coupon.getIssuedQuantity(), 100);
    }

    @Test
    @DisplayName("발급 수량을 초과 하면 Exception")
    void issue_2() {
        //given
        LocalDateTime now = LocalDateTime.now();

        Coupon coupon = Coupon.builder()
                .totalQuantity(100)
                .issuedQuantity(100)
                .dateIssueStart(now.minusDays(1))
                .dateIssueEnd(now.plusDays(2))
                .build();

        //when & then
        CouponIssueException exception = assertThrows(CouponIssueException.class, coupon::issue);
        assertEquals(exception.getErrorCode(), ErrorCode.INVALID_COUPON_ISSUE_QUANTITY );
    }

    @Test
    @DisplayName("발급 기간이 유효 하지 않으면 Exception")
    void issue_3(){
        //given
        LocalDateTime now = LocalDateTime.now();

        Coupon coupon = Coupon.builder()
                .totalQuantity(100)
                .issuedQuantity(99)
                .dateIssueStart(now.plusDays(1))
                .dateIssueEnd(now.plusDays(2))
                .build();

        //when & then
        CouponIssueException exception = assertThrows(CouponIssueException.class, coupon::issue);
        assertEquals(exception.getErrorCode(), ErrorCode.INVALID_COUPON_ISSUE_DATE);
    }

    @Test
    @DisplayName("발급 기간이 종료되면 true")
    void isCompleted_1(){
        LocalDateTime now = LocalDateTime.now();
        //given
        Coupon coupon = Coupon.builder()
                .totalQuantity(100)
                .issuedQuantity(0)
                .dateIssueStart(now.minusDays(2))
                .dateIssueEnd(now.minusDays(1))
                .build();
        //when
        boolean result = coupon.isIssueComplete();
        //then
        assertTrue(result);
    }

    @Test
    @DisplayName("발급 수량이 없다면 true")
    void isCompleted_2(){
        LocalDateTime now = LocalDateTime.now();
        //given
        Coupon coupon = Coupon.builder()
                .totalQuantity(100)
                .issuedQuantity(111)
                .dateIssueStart(now.minusDays(2))
                .dateIssueEnd(now.plusDays(1))
                .build();
        //when
        boolean result = coupon.isIssueComplete();
        //then
        assertTrue(result);
    }

    @Test
    @DisplayName("발급 기한과 수량이 유효하면 false를 반환한다")
    void isIssueComplete_3() {
        // given
        Coupon coupon = Coupon.builder()
                .dateIssueStart(LocalDateTime.now().minusDays(1))
                .dateIssueEnd(LocalDateTime.now().plusDays(2))
                .totalQuantity(100)
                .issuedQuantity(0)
                .build();
        // when
        boolean result = coupon.isIssueComplete();
        // then
        assertFalse(result);
    }
}