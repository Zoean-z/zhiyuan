package com.zhiyuan.college.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * SPA 入口不缓存：/ 与 /index.html 每次返回最新的入口 HTML（引用带内容 hash 的 chunk），
 * 避免浏览器缓存旧 index.html 后引用已被新构建覆盖的 chunk → 404 → 页面点击无响应/白屏。
 * assets/*.js 自带内容 hash，天然适合长缓存，不在此列。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class IndexNoCacheFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        String path = req.getRequestURI();
        if (path.equals("/") || path.equals("/index.html")) {
            resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            resp.setHeader("Pragma", "no-cache");
            resp.setHeader("Expires", "0");
        }
        chain.doFilter(request, response);
    }
}
