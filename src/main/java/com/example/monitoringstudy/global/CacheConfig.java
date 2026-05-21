package com.example.monitoringstudy.global;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@EnableCaching
@Configuration
public class CacheConfig {

    public static final String EVENT_SEATS_CACHE = "eventSeats";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(EVENT_SEATS_CACHE);

        cacheManager.setCaffeine(Caffeine.newBuilder()
                // 캐시 만료 시간 TTL 10초 (캐시 스탬피드 유도용)
                .expireAfterWrite(10, TimeUnit.SECONDS)
                .maximumSize(100));

        return cacheManager;
    }
}
