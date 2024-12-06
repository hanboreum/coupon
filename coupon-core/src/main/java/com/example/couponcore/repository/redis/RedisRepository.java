package com.example.couponcore.repository.redis;

import static com.example.couponcore.exception.ErrorCode.FAIL_COUPON_REQUEST;
import static com.example.couponcore.util.CouponRedisUtil.getIssueRequestKey;
import static com.example.couponcore.util.CouponRedisUtil.getIssueRequestQueueKey;

import com.example.couponcore.exception.CouponIssueException;
import com.example.couponcore.repository.redis.dto.CouponIssueRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RedisRepository {

    private final RedisTemplate<String, String> redisTemplate;

    private final RedisScript<String> issueScript = issueRequestScript();
    private final String issueRequestQueueKey = getIssueRequestQueueKey();
    private final ObjectMapper objectMapper = new ObjectMapper();

    //Sorted Set
    public Boolean zAdd(String key, String value, double score) {
        //add(key, value, score) 사용시 value 유지, score 업데이트가 되며 후순위로 다시 들어감.
        return redisTemplate.opsForZSet().addIfAbsent(key, value, score);
    }

    /**
     * Set 요청의 unique, 발급 수량 제어
     */
    public Long sAdd(String key, String value) {
        return redisTemplate.opsForSet().add(key, value);
    }

    //set 의 사이즈만
    public Long sCard(String key) {
        return redisTemplate.opsForSet().size(key);
    }

    public Boolean sIsMember(String key, String value) {
        return redisTemplate.opsForSet().isMember(key, value);
    }

    /**
     * List 쿠폰 발급 대기열
     */
    public Long rPush(String key, String value) {
        return redisTemplate.opsForList().rightPush(key, value);
    }

    public String lIndex(String key, long index) {
        return redisTemplate.opsForList().index(key, index);
    }

    public String lPop(String key) {
        return redisTemplate.opsForList().leftPop(key);
    }

    public Long lSize(String key) {
        return redisTemplate.opsForList().size(key);
    }

    //script 실행
    public void issueRequest(long couponId, long userId, int totalIssueQuantity) {
        String issueRequestKey = getIssueRequestKey(couponId);
        CouponIssueRequest couponIssueRequest = new CouponIssueRequest(couponId, userId);
        try {
            String code = redisTemplate.execute(
                    issueScript,
                    List.of(issueRequestKey, issueRequestQueueKey),
                    String.valueOf(userId),
                    String.valueOf(totalIssueQuantity),
                    objectMapper.writeValueAsString(couponIssueRequest)
            );
            CouponIssueRequestCode.checkRequestResult(CouponIssueRequestCode.find(code));
        } catch (JsonProcessingException e) {
            throw new CouponIssueException(FAIL_COUPON_REQUEST, "input: %s".formatted(couponIssueRequest));
        }
    }

    /**
     * call 을 통해 redis 명령어 SISMEMBER 실행. 결과가 이미 존재 한다면 2 return
     *
     *if 발급 수량이 남았으면
     * redis 명령어 실행
     * SADD - 요청 저장
     * RPUSH - 쿠폰 발급 큐에 적재
     * 1 return
     *
     * 그 이외에는 3 return
     */
    private RedisScript<String> issueRequestScript() {
        String script = """
                if redis.call('SISMEMBER', KEYS[1], ARGV[1]) == 1 then
                    return '2'
                end
                                
                if tonumber(ARGV[2]) > redis.call('SCARD', KEYS[1]) then
                    redis.call('SADD', KEYS[1], ARGV[1])
                    redis.call('RPUSH', KEYS[2], ARGV[3])
                    return '1'
                end
                                
                return '3'
                """;
        return RedisScript.of(script, String.class);
    }
}