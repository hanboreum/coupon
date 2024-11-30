package com.example.couponcore.component;

import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DistributeLockExecutor {

    private final RedissonClient redissonClient;

    public void execute(String lockName, long waitMilSecond,
            long leaseMillSecond, Runnable runnable) {
        // 락에 대한 객체를 가져온다. getLock 으로 락 이름 지정.
        RLock lock = redissonClient.getLock(lockName);

        try {
            /**
             * 락 획득 시도
             * waitMillSecond: 락 획득 시도하는 것을 얼마나 기다릴 것인지
             * leaseMillSecond: 획득 후, 락 유지 시간
             * TimeUnit: 단위 지정
             */
            boolean isLocked = lock.tryLock(waitMilSecond, leaseMillSecond, TimeUnit.MILLISECONDS);

            //락 획득 실패시
            if (!isLocked) {
                throw new IllegalStateException("[" + lockName + "] 락 획득 실패");
            }
            //획득 성공 하면 로직 실행
            runnable.run();
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }finally {
            //락 반환
            if(lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
