package com.example.couponapi.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @GetMapping("/hello")
    public String hello() throws InterruptedException {
        Thread.sleep(500); //처리량 제한. 0.5 초 쉬고 응답 보냄.
        /**
         * 이렇게 설정시 RPS 400 예상
         * -> 정답. 어떻게 알았을까?
         *
         * 초당 2건을 처리할 수 있다.
         * 2 * N(서버에서 동시에 처리할 수 있는 수)
         * = 2 * 200 (스레드풀의 수) = 400
         */
        return "Hello Locust! ";
    }
}
