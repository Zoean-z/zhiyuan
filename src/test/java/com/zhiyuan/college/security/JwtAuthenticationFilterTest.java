package com.zhiyuan.college.security;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

class JwtAuthenticationFilterTest {

    @Test
    void shouldAuthenticateAsyncDispatches() {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(null);
        assertFalse(filter.shouldNotFilterAsyncDispatch());
    }
}
