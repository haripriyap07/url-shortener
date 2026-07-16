package com.urlshortener.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RedisRateLimitService redisRateLimitService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        String ipAddress = xForwardedFor != null && !xForwardedFor.isBlank()
            ? xForwardedFor.split(",")[0].trim()
            : request.getRemoteAddr();

        if (!redisRateLimitService.isAllowed(ipAddress)) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"detail\": \"Rate limit exceeded. Max 10 requests per minute per IP.\"}");
            response.getWriter().flush();
            return false;
        }

        return true;
    }
}
