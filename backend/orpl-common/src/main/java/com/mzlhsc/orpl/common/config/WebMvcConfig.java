package com.mzlhsc.orpl.common.config;

import com.mzlhsc.orpl.auth.config.AuthProperties;
import com.mzlhsc.orpl.common.interceptor.TokenAuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.CollectionUtils;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * 单体版网关替代：JWT 普通拦截器 + CORS。
 *
 * <p>拦截 /admin/**、/user/**、/addressBook/**、/shopping-cart/**，
 * 登录、商品浏览、扫码看桌台、接口文档、健康检查等白名单由 AuthProperties 排除。</p>
 */
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(AuthProperties.class)
public class WebMvcConfig implements WebMvcConfigurer {

    private final TokenAuthInterceptor tokenAuthInterceptor;
    private final AuthProperties authProperties;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        var registration = registry.addInterceptor(tokenAuthInterceptor)
                .addPathPatterns("/admin/**", "/user/**", "/addressBook/**", "/shopping-cart/**");
        List<String> excludes = authProperties.getExcludePaths();
        if (!CollectionUtils.isEmpty(excludes)) {
            registration.excludePathPatterns(excludes.toArray(new String[0]));
        }
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("*")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
