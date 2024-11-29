package com.example.couponapi.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(value = Include.NON_NULL) //발급 성공시 comment x
public record CouponIssueResponseDto(boolean isSucceed, String comment) {

}
