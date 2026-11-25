package com.mzlhsc.orpl.common.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 文件存储配置，对应 application.yml 里的 {@code mzlhsc.storage}。
 * <p>
 * 没有配置阿里云 OSS 时，上传的图片可以落到服务器本地目录，
 * 由 nginx（生产）或 Spring 静态资源映射（本地开发）对外提供访问。
 */
@Data
@Component
@ConfigurationProperties(prefix = "mzlhsc.storage")
public class StorageProperties {

    /** 存储方式：auto（默认，OSS 配置齐全就用 OSS，否则本地）/ oss / local */
    private String type = "auto";

    /** 本地存储目录，相对路径按应用启动目录解析 */
    private String localDir = "./uploads";

    /** 本地文件的对外访问前缀，如 /uploads */
    private String publicPrefix = "/uploads";
}
