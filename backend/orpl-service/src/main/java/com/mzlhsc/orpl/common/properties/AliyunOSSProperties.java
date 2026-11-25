package com.mzlhsc.orpl.common.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 阿里云 OSS 配置，对应 application.yml 里的 {@code mzlhsc.aliyun}
 */
@Data
@Component
@ConfigurationProperties(prefix = "mzlhsc.aliyun")
public class AliyunOSSProperties {

    /** 地域节点，如 https://oss-cn-shenzhen.aliyuncs.com */
    private String endpoint;

    /** 存储空间名称 */
    private String bucketName;

    /** 地域，如 cn-shenzhen（使用 V4 签名时必填） */
    private String region;

    /** AccessKey ID，不填则读取环境变量 OSS_ACCESS_KEY_ID */
    private String accessKeyId;

    /** AccessKey Secret，不填则读取环境变量 OSS_ACCESS_KEY_SECRET */
    private String accessKeySecret;
}
