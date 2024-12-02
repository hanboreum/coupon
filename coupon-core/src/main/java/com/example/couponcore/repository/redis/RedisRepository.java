package com.example.couponcore.repository.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RedisRepository {

    private final RedisTemplate<String, String> redisTemplate;

    public boolean zAdd(String key, String value, double score){
        //add(key, value, score) 사용시 value 유지, score 업데이트가 되며 후순위로 다시 들어감.
        return redisTemplate.opsForZSet().addIfAbsent(key, value, score);
    }
}
