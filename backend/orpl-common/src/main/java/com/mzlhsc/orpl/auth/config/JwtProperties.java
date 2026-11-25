package com.mzlhsc.orpl.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.time.Duration;

/**
 * 令牌相关配置。
 * <p>
 * 所有字段都给了默认值，密钥库 mzlhsc.jks 也由本模块提供并打进 jar，
 * 所以各业务服务的 application.yml 里<b>不需要</b>再重复写 jwt 配置。
 * <p>
 * 只有需要覆盖默认值时才写，例如：
 * <pre>
 * mzlhsc:
 *   jwt:
 *     token-ttl: 2h
 * </pre>
 */
@Data
@ConfigurationProperties(prefix = "mzlhsc.jwt")
public class JwtProperties {

    /** 密钥库位置，默认为 classpath 根目录下的 mzlhsc.jks */
    private Resource location = new ClassPathResource("mzlhsc.jks");

    /** 密钥库密码 */
    private String password = "mzlhsc111";

    /** 密钥别名 */
    private String alias = "mzlhsc";

    /** 令牌有效期 */
    private Duration tokenTTL = Duration.ofMinutes(30);
}
