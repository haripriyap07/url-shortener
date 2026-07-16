package com.urlshortener.ratelimit;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RedisRateLimitService {

    private final StringRedisTemplate redisTemplate;

    /*
     * Sliding window counter pattern for IP-based rate limiting.
     * It counts requests per IP in Redis and expires the counter after 60 seconds.
     * This is simpler than a token bucket and avoids in-memory limits in clustered deployments.
     */
    public boolean isAllowed(String ipAddress) {
        String key = "rate_limit:" + ipAddress;
        Long counter = redisTemplate.opsForValue().increment(key);

        if (counter == null) {
            return false;
        }

        if (counter == 1) {
            redisTemplate.expire(key, Duration.ofSeconds(60));
        }

        return counter <= 10;
    }
}
