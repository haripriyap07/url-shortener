package com.urlshortener.controller;

import com.urlshortener.dto.ShortenRequest;
import com.urlshortener.dto.ShortenResponse;
import com.urlshortener.dto.StatsDTO;
import com.urlshortener.service.UrlShortenerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class UrlShortenerController {

    private final UrlShortenerService service;

    // POST /api/shorten
    @PostMapping("/api/shorten")
    @ResponseStatus(HttpStatus.CREATED)
    public ShortenResponse shorten(@Valid @RequestBody ShortenRequest request) {
        return service.shorten(request);
    }

    // GET /{code} — the actual redirect
    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode) {
        String originalUrl = service.resolve(shortCode);
        service.recordClick(shortCode);  // async, doesn't block
        // 302 = temporary redirect (preserves click analytics)
        // Use 301 only if the mapping is truly permanent
        return ResponseEntity.status(HttpStatus.FOUND)
            .location(URI.create(originalUrl))
            .build();
    }

    // GET /api/urls — stats page data
    @GetMapping("/api/urls")
    public List<ShortenResponse> getAllUrls() {
        return service.getAllUrls();
    }

    // GET /api/urls/stats — aggregate totals and top URL click stats
    @GetMapping("/api/urls/stats")
    public StatsDTO getStats() {
        return service.getStats();
    }

    // DELETE /api/urls/{code}
    @DeleteMapping("/api/urls/{shortCode}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String shortCode) {
        service.delete(shortCode);
    }
}
