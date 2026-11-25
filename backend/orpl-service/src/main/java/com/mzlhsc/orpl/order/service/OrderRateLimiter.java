package com.mzlhsc.orpl.order.service;

import com.mzlhsc.orpl.common.exception.CommonException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 下单接口 Redis 固定窗口限流。
 * <p>
 * 用 Lua 保证 INCR + EXPIRE 原子执行，避免高并发下 key 没有过期时间。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderRateLimiter {

    private static final DefaultRedisScript<Long> SCRIPT = new DefaultRedisScript<>("""
            local current = redis.call('INCR', KEYS[1])
            if current == 1 then
                redis.call('EXPIRE', KEYS[1], ARGV[1])
            end
            return current
            """, Long.class);

    private final StringRedisTemplate stringRedisTemplate;

    @Value("${mzlhsc.rate-limit.order-per-minute:10}")
    private int limitPerMinute;

    public void check(Long userId) {
        if (userId == null) {
            return;
        }
        String key = "rate:order:submit:" + userId;
        Long count = stringRedisTemplate.execute(SCRIPT, List.of(key), "60");
        if (count != null && count > limitPerMinute) {
            log.warn("下单限流触发 userId={} count={} limit={}", userId, count, limitPerMinute);
            throw new CommonException("下单太频繁了，请稍后再试（每分钟最多 " + limitPerMinute + " 次）", 429);
        }
    }
}


