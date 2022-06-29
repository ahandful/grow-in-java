package com.paji.pangu;

import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
public class SimplyRedisLock3 {
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
        ExecutorService executorService = Executors.newFixedThreadPool(100);
        for (int i = 0; i < 1000; i++) {
            executorService.submit(()->index());
            Thread.sleep(3);
        }
    }

    public void index() {

        // 每个人进来先要进行加锁，key值为"redis_lock"，value随机生成
        String value = UUID.randomUUID().toString().replace("-","");
        try{
            // 加锁
            Boolean flag = template.opsForValue().setIfAbsent(REDIS_LOCK, value,10L, TimeUnit.SECONDS);
            // 加锁失败
            if (!flag) {
//                log.info("抢锁失败！");
                return;
            }
            log.info(value + " 抢锁成功");
            // 业务逻辑
            String result = template.opsForValue().get("001");
            int total = result == null ? 0 : Integer.parseInt(result);
            if (total > 0) {
                int realTotal = total - 1;
                template.opsForValue().set("001", String.valueOf(realTotal));
                log.info("购买商品成功，库存还剩：" + realTotal + "件");
                return;
            } else {
                log.info("购买商品失败");
            }
        }finally {
            // 谁加的锁，谁才能删除！！！！
            if(template.opsForValue().get(REDIS_LOCK).equals(value)){
                template.delete(REDIS_LOCK);
            }
        }
    }   
}
