package com.example.couponcore.exception;

public enum ErrorCode {
    INVALID_COUPON_ISSUE_QUANTITY("발급 가능 수량 초과"),
    INVALID_COUPON_ISSUE_DATE("유효하지 않은 발급 기간"),
    COUPON_NOT_EXIST("존재하지 않는 구폰"),
    DUPLICATED_COUPON_ISSUE("이미 발급된 쿠폰");

    public final String message;

    ErrorCode(String message) {
        this.message = message;
    }
}
