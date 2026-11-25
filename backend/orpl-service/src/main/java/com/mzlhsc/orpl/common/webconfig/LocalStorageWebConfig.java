package com.mzlhsc.orpl.common.webconfig;

import com.mzlhsc.orpl.common.utils.LocalFileStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 本地存储的静态资源映射。
 * <p>
 * 开发环境（没有 nginx）时，{@code GET /uploads/2026/11/xxx.png} 直接由应用读出文件；
 * 生产环境 nginx 会先命中 {@code location /uploads/}，这里只是兜底。
 * <p>
 * 放在独立包 {@code com.mzlhsc.orpl.common.webconfig} 并显式加入启动类扫描，
 * 避免把 ORM / Redis 等公共配置重复装配。
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class LocalStorageWebConfig implements WebMvcConfigurer {

    private final LocalFileStorage localFileStorage;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String prefix = localFileStorage.urlPrefix();
        if (prefix.isEmpty()) {
            return;
        }
        String location = localFileStorage.rootDir().toUri().toString();
        if (!location.endsWith("/")) {
            location = location + "/";
        }
        registry.addResourceHandler(prefix + "/**")
                .addResourceLocations(location)
                .setCachePeriod(3600);
        log.info("本地文件访问映射: {}/** -> {}", prefix, location);
    }
}
