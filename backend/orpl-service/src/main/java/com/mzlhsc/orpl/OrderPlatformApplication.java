package com.mzlhsc.orpl;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * order-platform 单体启动类。
 *
 * <p>原来的 11 个 Spring Boot 服务、网关、Nacos 注册中心全部合并到这里：
 * 一个进程、一个端口、MySQL + Redis 两个中间件。</p>
 *
 * <p>只扫描业务包；公共组件的自动配置仍由 orpl-common 的
 * AutoConfiguration.imports 负责，避免 Jwt/Redis/MyBatis 配置被扫描两次。</p>
 */
@SpringBootApplication(scanBasePackages = {
        "com.mzlhsc.orpl.emp",
        "com.mzlhsc.orpl.ctgy",
        "com.mzlhsc.orpl.dish",
        "com.mzlhsc.orpl.setmeal",
        "com.mzlhsc.orpl.order",
        "com.mzlhsc.orpl.user",
        "com.mzlhsc.orpl.report",
        "com.mzlhsc.orpl.shop",
        "com.mzlhsc.orpl.common.controller",
        "com.mzlhsc.orpl.common.properties",
        "com.mzlhsc.orpl.common.utils",
        "com.mzlhsc.orpl.common.interceptor",
        "com.mzlhsc.orpl.common.webconfig"
})
@MapperScan({
        "com.mzlhsc.orpl.emp.mapper",
        "com.mzlhsc.orpl.ctgy.mapper",
        "com.mzlhsc.orpl.dish.mapper",
        "com.mzlhsc.orpl.setmeal.mapper",
        "com.mzlhsc.orpl.order.mapper",
        "com.mzlhsc.orpl.user.mapper"
})
@EnableCaching
@EnableTransactionManagement
public class OrderPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderPlatformApplication.class, args);
    }

    /**
     * 显式提供本地缓存管理器，避免仅开启 @EnableCaching 时动态 cache 未注册。
     */
    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager();
    }
}
