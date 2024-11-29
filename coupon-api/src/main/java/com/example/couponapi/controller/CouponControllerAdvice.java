package com.example.couponapi.controller;

import com.example.couponapi.controller.dto.CouponIssueResponseDto;
import com.example.couponcore.exception.CouponIssueException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
//응답 실패에 대한 처리
public class CouponControllerAdvice {

    @ExceptionHandler(CouponIssueException.class)
    public CouponIssueResponseDto couponIssueException(CouponIssueException e) {
        return new CouponIssueResponseDto(false, e.getErrorCode().message);
    }
}
