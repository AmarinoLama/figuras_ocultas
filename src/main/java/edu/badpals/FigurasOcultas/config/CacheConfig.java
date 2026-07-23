package edu.badpals.FigurasOcultas.config;

import org.ehcache.config.CacheConfiguration;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.ExpiryPolicyBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.MemoryUnit;
import org.ehcache.jsr107.Eh107Configuration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.jcache.JCacheCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.cache.Caching;
import javax.cache.spi.CachingProvider;
import java.time.Duration;

@Configuration
@EnableCaching
public class CacheConfig {
    
    @Bean
    public JCacheCacheManager cacheManager() {
        CachingProvider cachingProvider = Caching.getCachingProvider();
        javax.cache.CacheManager cacheManager = cachingProvider.getCacheManager();
        
        // Alumnos cache - 5 minutes
        createCache(cacheManager, "alumnos", 500, Duration.ofMinutes(5));
        
        // Cartas cache - 15 minutes
        createCache(cacheManager, "cartas", 200, Duration.ofMinutes(15));
        
        // WebConfig cache - 1 hour
        createCache(cacheManager, "webConfig", 10, Duration.ofHours(1));
        
        // Inventario cache - 5 minutes
        createCache(cacheManager, "inventario", 500, Duration.ofMinutes(5));
        
        // Image caches - 24 hours with memory size limit
        createCacheWithMemory(cacheManager, "cartaImagen", 20, Duration.ofHours(24));
        createCacheWithMemory(cacheManager, "insigniaImagen", 10, Duration.ofHours(24));
        
        return new JCacheCacheManager(cacheManager);
    }
    
    private void createCache(javax.cache.CacheManager cacheManager, String cacheName, 
                            long heapEntries, Duration ttl) {
        CacheConfiguration<Object, Object> cacheConfiguration = CacheConfigurationBuilder
            .newCacheConfigurationBuilder(Object.class, Object.class,
                ResourcePoolsBuilder.heap(heapEntries))
            .withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(ttl))
            .build();
            
        cacheManager.createCache(cacheName, 
            Eh107Configuration.fromEhcacheCacheConfiguration(cacheConfiguration));
    }
    
    private void createCacheWithMemory(javax.cache.CacheManager cacheManager, String cacheName,
                                      long heapMB, Duration ttl) {
        CacheConfiguration<Object, Object> cacheConfiguration = CacheConfigurationBuilder
            .newCacheConfigurationBuilder(Object.class, Object.class,
                ResourcePoolsBuilder.newResourcePoolsBuilder().heap(heapMB, MemoryUnit.MB))
            .withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(ttl))
            .build();
            
        cacheManager.createCache(cacheName,
            Eh107Configuration.fromEhcacheCacheConfiguration(cacheConfiguration));
    }
}
