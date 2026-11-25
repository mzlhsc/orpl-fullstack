package com.mzlhsc.orpl.common.config;

import com.mzlhsc.orpl.common.advice.CommonExceptionAdvice;
import com.mzlhsc.orpl.common.aspect.AutoFillAspect;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

/**
 * 公共组件自动装配（单体版）。
 * <p>业务包由启动类扫描，公共 advice/aspect/handler 仍由自动配置注册，
 * 避免把 com.mzlhsc.orpl.common.config 下的自动配置类也扫描一遍。</p>
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class CommonAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public CommonExceptionAdvice commonExceptionAdvice() {
        return new CommonExceptionAdvice();
    }

    @Bean
    @ConditionalOnMissingBean
    public AutoFillAspect autoFillAspect() {
        return new AutoFillAspect();
    }

    @Bean
    @ConditionalOnClass(OpenAPI.class)
    @ConditionalOnMissingBean(OpenAPI.class)
    public OpenAPI commonOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("order-platform 单体接口文档")
                .description("外卖 + 堂食 + 库存 + 报表，单体 Spring Boot")
                .version("1.0")
                .contact(new Contact().name("mzlhsc")));
    }
}
