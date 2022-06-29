package com.paji.pangu;

import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openjdk.jmh.annotations.Benchmark;
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
public class SimplyRedisLock {
    // Redis分布式锁的key
    public static final String REDIS_LOCK = "redis_lock";

    @Autowired
    StringRedisTemplate template;

    @Before
    public void addGood(){
        template.opsForValue().set("001", String.valueOf(100));
    }


    @Test
    public void test() throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        for (int i = 0; i < 100; i++) {
            executorService.submit(()->index());
            Thread.sleep(2);
        }
    }

    public void index() {
        // 每个人进来先要进行加锁，key值为"redis_lock"，value随机生成
        String value = UUID.randomUUID().toString().replace("-", "");
        try {
            // 加锁
            Boolean flag = template.opsForValue().setIfAbsent(REDIS_LOCK, value);
            // 加锁失败
            if (!flag) {
                log.info("抢锁失败！");
                return;
            }
            log.info(value + " 抢锁成功");
            // 业务逻辑
            String result = template.opsForValue().get("001");
            int total = result == null ? 0 : Integer.parseInt(result);
            if (total > 0) {
                int realTotal = total - 1;
                template.opsForValue().set("001", String.valueOf(realTotal));
                // 如果在抢到锁之后，删除锁之前，发生了异常，锁就无法被释放，
                // 释放锁操作不能在此操作，要在finally处理
                // template.delete(REDIS_LOCK);
                log.info("购买商品成功，库存还剩：" + realTotal + "件");
                return;
            } else {
                log.info("购买商品失败");
            }
        } finally {
            // 释放锁
            template.delete(REDIS_LOCK);
            log.info("释放锁");
        }
    }
}