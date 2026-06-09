package com.zhiyuan.college.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "cache.redis")
public class CacheRedisProperties {

    private boolean enabled = false;
    private Duration recommendationTtl = Duration.ofMinutes(10);
    private Duration metaTtl = Duration.ofMinutes(30);

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Duration getRecommendationTtl() {
        return recommendationTtl;
    }

    public void setRecommendationTtl(Duration recommendationTtl) {
        this.recommendationTtl = recommendationTtl;
    }

    public Duration getMetaTtl() {
        return metaTtl;
    }

    public void setMetaTtl(Duration metaTtl) {
        this.metaTtl = metaTtl;
    }
}
