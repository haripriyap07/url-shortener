package com.urlshortener.service;

import com.urlshortener.dto.ShortenRequest;
import com.urlshortener.dto.ShortenResponse;
import com.urlshortener.dto.StatsDTO;
import com.urlshortener.entity.UrlMapping;
import com.urlshortener.exception.UrlNotFoundException;
import com.urlshortener.repository.UrlMappingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class UrlShortenerService {

    private final UrlMappingRepository repository;
    private final StringRedisTemplate redisTemplate;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    private static final String ALPHABET =
        "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int CLICK_FLUSH_THRESHOLD = 10;
    private static final int CLICK_FLUSH_INTERVAL_SECONDS = 30;

    // --- Shorten a URL ---
    // Evict global URL stats so the cached summary stays fresh after a new URL is created.
    @Transactional
    @CacheEvict(value = "stats", key = "'global'")
    public ShortenResponse shorten(ShortenRequest request) {
        String shortCode = generateUniqueCode();

        UrlMapping mapping = new UrlMapping();
        mapping.setOriginalUrl(request.getUrl());
        mapping.setShortCode(shortCode);

        if (request.getTtlHours() != null) {
            mapping.setExpiresAt(LocalDateTime.now().plusHours(request.getTtlHours()));
        }

        repository.save(mapping);

        return toResponse(mapping);
    }

    // --- Resolve short code to original URL ---
    /*
     * Cache-aside pattern for URL resolution.
     * Without this, every redirect would hit the database and performance would degrade under load.
     */
    @Cacheable(value = "urls", key = "#shortCode")
    public String resolve(String shortCode) {
        UrlMapping mapping = repository.findByShortCode(shortCode)
            .orElseThrow(() -> new UrlNotFoundException(shortCode));

        if (mapping.getExpiresAt() != null &&
            mapping.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new UrlNotFoundException(shortCode + " (expired)");
        }

        return mapping.getOriginalUrl();
    }

    // --- Refresh the cached URL mapping after an update ---
    /*
     * @CachePut stores the fresh DTO directly into cache after reading from the DB.
     * This avoids an evict-then-retrieve gap where a request could see a cache miss
     * and then immediately hit the database again.
     */
    @CachePut(value = "urls", key = "#shortCode")
    public String refreshCache(String shortCode) {
        UrlMapping mapping = repository.findByShortCode(shortCode)
            .orElseThrow(() -> new UrlNotFoundException(shortCode));

        if (mapping.getExpiresAt() != null &&
            mapping.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new UrlNotFoundException(shortCode + " (expired)");
        }

        return mapping.getOriginalUrl();
    }

    // --- Increment click count immediately and use Redis as a lightweight buffer ---
    /*
     * Clicks should appear in the UI promptly. We update the database immediately
     * for correctness and keep Redis only as a short-lived buffer for aggregation.
     */
    @Async
    @Transactional
    public void recordClick(String shortCode) {
        repository.incrementClickCount(shortCode);

        try {
            String key = "clicks:" + shortCode;
            Long totalClicks = redisTemplate.opsForValue().increment(key);

            if (totalClicks == null) {
                return;
            }

            if (totalClicks == 1) {
                redisTemplate.expire(key, Duration.ofSeconds(CLICK_FLUSH_INTERVAL_SECONDS));
            }

            if (totalClicks % CLICK_FLUSH_THRESHOLD == 0) {
                repository.incrementClickCountBy(shortCode, CLICK_FLUSH_THRESHOLD);
                redisTemplate.opsForValue().set(key, "0");
            }
        } catch (DataAccessException ex) {
            log.warn("Redis unavailable while recording click for {}. Click recorded directly in DB.", shortCode, ex);
        }
    }

    // --- Get all URLs for the stats page ---
    public List<ShortenResponse> getAllUrls() {
        return repository.findAll()
            .stream()
            .map(this::toResponse)
            .toList();
    }

    // --- Get global URL stats with caching ---
    /*
     * A single key is used to cache aggregated stats. The SpEL string literal is quoted
     * as "'global'" because cache keys are evaluated as expressions.
     */
    @Cacheable(value = "stats", key = "'global'")
    public StatsDTO getStats() {
        Long totalUrls = repository.count();
        Long totalClicks = repository.sumClickCount();
        List<ShortenResponse> topUrls = repository.findTop5ByOrderByClickCountDesc()
            .stream()
            .map(this::toResponse)
            .toList();

        return new StatsDTO(totalUrls, totalClicks, topUrls);
    }

    // --- Delete a URL ---
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "urls", key = "#shortCode"),
        @CacheEvict(value = "stats", key = "'global'")
    })
    public void delete(String shortCode) {
        UrlMapping mapping = repository.findByShortCode(shortCode)
            .orElseThrow(() -> new UrlNotFoundException(shortCode));
        repository.delete(mapping);
    }

    // --- Scheduled flush of click counters into the DB ---
    @Scheduled(fixedRate = 30000)
    @Transactional
    public void syncAllClickCountsToDb() {
        try {
            Set<String> keys = redisTemplate.keys("clicks:*");
            if (keys == null || keys.isEmpty()) {
                return;
            }

            for (String key : keys) {
                String currentCount = redisTemplate.opsForValue().get(key);
                if (currentCount == null) {
                    continue;
                }
                Long amount;
                try {
                    amount = Long.parseLong(currentCount);
                } catch (NumberFormatException ex) {
                    continue;
                }
                if (amount <= 0) {
                    redisTemplate.delete(key);
                    continue;
                }

                String shortCode = key.substring("clicks:".length());
                repository.incrementClickCountBy(shortCode, amount.intValue());
                redisTemplate.delete(key);
            }
        } catch (DataAccessException ex) {
            log.warn("Redis unavailable during scheduled click sync. Skipping this run.", ex);
        }
    }

    // --- Helpers ---
    private String generateUniqueCode() {
        String code;
        int attempts = 0;
        do {
            code = randomCode(6);
            attempts++;
            if (attempts > 10) throw new RuntimeException("Could not generate unique code");
        } while (repository.findByShortCode(code).isPresent());
        return code;
    }

    private String randomCode(int length) {
        Random random = new Random();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }

    private ShortenResponse toResponse(UrlMapping m) {
        return new ShortenResponse(
            m.getShortCode(),
            baseUrl + "/" + m.getShortCode(),
            m.getOriginalUrl(),
            m.getClickCount()
        );
    }
}