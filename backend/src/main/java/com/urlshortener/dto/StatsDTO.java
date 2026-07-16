package com.urlshortener.dto;

import java.util.List;

public class StatsDTO {
    private Long totalUrls;
    private Long totalClicks;
    private List<ShortenResponse> topUrls;

    public StatsDTO(Long totalUrls, Long totalClicks, List<ShortenResponse> topUrls) {
        this.totalUrls = totalUrls;
        this.totalClicks = totalClicks;
        this.topUrls = topUrls;
    }

    public Long getTotalUrls() {
        return totalUrls;
    }

    public void setTotalUrls(Long totalUrls) {
        this.totalUrls = totalUrls;
    }

    public Long getTotalClicks() {
        return totalClicks;
    }

    public void setTotalClicks(Long totalClicks) {
        this.totalClicks = totalClicks;
    }

    public List<ShortenResponse> getTopUrls() {
        return topUrls;
    }

    public void setTopUrls(List<ShortenResponse> topUrls) {
        this.topUrls = topUrls;
    }
}
