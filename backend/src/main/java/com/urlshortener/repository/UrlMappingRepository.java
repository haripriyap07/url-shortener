package com.urlshortener.repository;

import com.urlshortener.entity.UrlMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface UrlMappingRepository extends JpaRepository<UrlMapping, Long> {

    Optional<UrlMapping> findByShortCode(String shortCode);

    List<UrlMapping> findTop5ByOrderByClickCountDesc();

    @Query("SELECT COALESCE(SUM(u.clickCount), 0) FROM UrlMapping u")
    Long sumClickCount();

    // Atomic increment — avoids read-modify-write race condition
    @Modifying
    @Query("UPDATE UrlMapping u SET u.clickCount = u.clickCount + 1 WHERE u.shortCode = :shortCode")
    void incrementClickCount(@Param("shortCode") String shortCode);

    @Modifying
    @Query("UPDATE UrlMapping u SET u.clickCount = u.clickCount + :amount WHERE u.shortCode = :shortCode")
    void incrementClickCountBy(@Param("shortCode") String shortCode, @Param("amount") int amount);
}