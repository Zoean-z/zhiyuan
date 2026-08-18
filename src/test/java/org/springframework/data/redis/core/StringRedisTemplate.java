package org.springframework.data.redis.core;

import java.time.Duration;

/**
 * Test-only shim for offline sandbox compilation when spring-data-redis jars
 * are unavailable. Unit tests in this workspace mock higher-level services and
 * do not depend on real Redis behavior.
 */
public class StringRedisTemplate {

    public ValueOperations opsForValue() {
        return new ValueOperations();
    }

    public static class ValueOperations {
        public String get(String key) {
            return null;
        }

        public void set(String key, String value, Duration timeout) {
            // no-op test shim
        }
    }
}
