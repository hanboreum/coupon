package com.example.couponcore.repository.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RedisRepository {

    private final RedisTemplate<String, String> redisTemplate;

    //Sorted Set
    public Boolean zAdd(String key, String value, double score){
        //add(key, value, score) 사용시 value 유지, score 업데이트가 되며 후순위로 다시 들어감.
        return redisTemplate.opsForZSet().addIfAbsent(key, value, score);
    }

    /**
     * Set
     * 요청의 unique, 발급 수량 제어
     */
    public Long sAdd(String key, String value){
        return redisTemplate.opsForSet().add(key, value);
    }

    //set 의 사이즈만
    public Long sCard(String key){
        return redisTemplate.opsForSet().size(key);
    }

    public Boolean sIsMember(String key, String value){
        return redisTemplate.opsForSet().isMember(key, value);
    }

    /**
     *List
     * 쿠폰 발급 대기열
     */
    public Long rPush(String key, String value){
        return redisTemplate.opsForList().rightPush(key, value);
    }
}
