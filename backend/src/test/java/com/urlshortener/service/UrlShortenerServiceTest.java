package com.urlshortener.service;

import com.urlshortener.repository.UrlMappingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UrlShortenerServiceTest {

    @Mock
    private UrlMappingRepository repository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private UrlShortenerService service;

    @Test
    void recordClick_incrementsDatabaseImmediately() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment("clicks:abc123")).thenReturn(1L);

        service.recordClick("abc123");

        verify(repository).incrementClickCount("abc123");
    }
}
