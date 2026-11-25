package com.mzlhsc.orpl.auth.config;

import com.mzlhsc.orpl.auth.util.JwtTool;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnResource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.encrypt.KeyStoreKeyFactory;

import java.security.KeyPair;

/**
 * 令牌工具自动配置。
 * <p>
 * 只有 classpath 下存在密钥库的服务才会创建 KeyPair / JwtTool：
 * 单体启动时统一注册 JWT 组件。
 * 网关自己声明了 KeyPair（SecurityConfig），这里靠 @ConditionalOnMissingBean 让位。
 */
@AutoConfiguration
@EnableConfigurationProperties(JwtProperties.class)
@ConditionalOnResource(resources = "classpath:mzlhsc.jks")
public class JwtAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public KeyPair jwtKeyPair(JwtProperties properties) {
        KeyStoreKeyFactory factory = new KeyStoreKeyFactory(
                properties.getLocation(),
                properties.getPassword().toCharArray());
        return factory.getKeyPair(properties.getAlias(), properties.getPassword().toCharArray());
    }

    @Bean
    @ConditionalOnMissingBean
    public JwtTool jwtTool(KeyPair keyPair) {
        return new JwtTool(keyPair);
    }
}

