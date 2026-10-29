package com.example.elibrary.config;

import com.example.elibrary.application.event.CatalogueChanged;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Drops the cached view of a book once its availability changed. */
@Slf4j
@Component
public class CatalogueCacheInvalidator {

    private static final String DETAIL_KEY_PREFIX = "detail:";

    private final CacheManager cacheManager;

    public CatalogueCacheInvalidator(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @EventListener
    public void onCatalogueChanged(CatalogueChanged event) {
        var cache = cacheManager.getCache(RedisCacheConfig.CATALOGUE_CACHE);
        if (cache == null) {
            return;
        }
        cache.evictIfPresent(DETAIL_KEY_PREFIX + event.bookId());
        log.debug("Evicted cached detail for bookId={}", event.bookId());
    }
}
