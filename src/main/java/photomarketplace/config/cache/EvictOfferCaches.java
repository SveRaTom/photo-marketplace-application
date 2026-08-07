package photomarketplace.config.cache;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Caching(evict = {
        @CacheEvict(cacheNames = CacheNames.OFFER_CATALOG, allEntries = true),
        @CacheEvict(cacheNames = CacheNames.OFFER_DETAILS, allEntries = true),
        @CacheEvict(cacheNames = CacheNames.PHOTOGRAPHER_OFFERS, allEntries = true)
})
public @interface EvictOfferCaches {
}
