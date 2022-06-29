package com.paji.pangu;

import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
public class SimplyRedisLock5 {
    // Redis分布式锁的key
    public static final String REDIS_LOCK = "redis_lock";

    @Autowired
    StringRedisTemplate template;

    @Autowired
    RedissonClient redissonClient;

    @Before
    public void addGood(){
        template.opsForValue().set("001", String.valueOf(100));
    }

    @Test
    public void test() throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        for (int i = 0; i < 1000; i++) {
            executorService.submit(()-> {
                try {
                    index();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
            Thread.sleep(2);
        }
    }

    public void index() throws InterruptedException {

        RLock lock = redissonClient.getLock(REDIS_LOCK);
        lock.lock();
        // 每个人进来先要进行加锁，key值为"redis_lock"
        String value = UUID.randomUUID().toString().replace("-","");
        try {
            String result = template.opsForValue().get("001");
            int total = result == null ? 0 : Integer.parseInt(result);
            if (total > 0) {
                // 如果在此处需要调用其他微服务，处理时间较长。。。
                int realTotal = total - 1;
                template.opsForValue().set("001", String.valueOf(realTotal));
                log.info("购买商品成功，库存还剩：" + realTotal + "件");
                return;
            } else {
                log.info("购买商品失败");
            }
        }finally {
            if(lock.isLocked() && lock.isHeldByCurrentThread()){
                lock.unlock();
            }
        }

//        RSemaphore semaphore = redissonClient.getSemaphore("semaphore");
//        log.info("permits:{}",semaphore.availablePermits());
//        semaphore.acquire(23);
//        log.info("permits:{}",semaphore.availablePermits());
    }
}