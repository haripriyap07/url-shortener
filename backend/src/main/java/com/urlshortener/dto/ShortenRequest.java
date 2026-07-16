// ShortenRequest.java
package com.urlshortener.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class ShortenRequest {

    @NotBlank(message = "URL cannot be blank")
    @Pattern(
        regexp = "^https?://.*",
        message = "URL must start with http:// or https://"
    )
    private String url;

    // Optional TTL in hours, null = no expiry
    private Integer ttlHours;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Integer getTtlHours() {
        return ttlHours;
    }

    public void setTtlHours(Integer ttlHours) {
        this.ttlHours = ttlHours;
    }
}