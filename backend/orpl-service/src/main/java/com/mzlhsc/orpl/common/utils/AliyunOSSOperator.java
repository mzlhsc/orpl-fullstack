package com.mzlhsc.orpl.common.utils;

import cn.hutool.core.util.StrUtil;
import com.aliyun.oss.ClientBuilderConfiguration;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.common.auth.CredentialsProviderFactory;
import com.aliyun.oss.common.auth.DefaultCredentialProvider;
import com.aliyun.oss.common.comm.SignVersion;
import com.mzlhsc.orpl.common.exception.BizIllegalException;
import com.mzlhsc.orpl.common.properties.AliyunOSSProperties;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 阿里云 OSS 上传工具。
 * <p>
 * 客户端延迟初始化：没有配置 AccessKey 时应用仍可正常启动，
 * 只有真正调用上传接口时才提示缺少配置，避免因为一个可选功能拖垮整个服务。
 * OSS 客户端是线程安全的，只创建一次、随应用关闭而释放。
 */
@Slf4j
@Component
public class AliyunOSSOperator {

    private final AliyunOSSProperties properties;

    /** 全局复用的 OSS 客户端，延迟初始化 */
    private volatile OSS ossClient;

    public AliyunOSSOperator(AliyunOSSProperties properties) {
        this.properties = properties;
    }

    /**
     * OSS 是否具备可用配置（endpoint / bucket / region + 凭证）。
     * 上传接口据此决定走 OSS 还是本地存储，避免没配 OSS 时直接报错。
     */
    public boolean isConfigured() {
        if (StrUtil.isBlank(properties.getEndpoint())
                || StrUtil.isBlank(properties.getBucketName())
                || StrUtil.isBlank(properties.getRegion())) {
            return false;
        }
        if (StrUtil.isNotBlank(properties.getAccessKeyId())
                && StrUtil.isNotBlank(properties.getAccessKeySecret())) {
            return true;
        }
        return StrUtil.isNotBlank(System.getenv("OSS_ACCESS_KEY_ID"))
                && StrUtil.isNotBlank(System.getenv("OSS_ACCESS_KEY_SECRET"));
    }

    /**
     * 上传文件
     *
     * @param content          文件字节
     * @param originalFilename 原始文件名（只用来取后缀）
     * @return 可访问的 URL
     */
    public String upload(byte[] content, String originalFilename) {
        if (content == null || content.length == 0) {
            throw new BizIllegalException("上传文件内容不能为空");
        }
        String objectName = buildObjectName(originalFilename);
        getClient().putObject(properties.getBucketName(), objectName, new ByteArrayInputStream(content));
        String url = buildUrl(objectName);
        log.info("文件上传成功: {}", url);
        return url;
    }

    private OSS getClient() {
        if (ossClient == null) {
            synchronized (this) {
                if (ossClient == null) {
                    ossClient = createClient();
                }
            }
        }
        return ossClient;
    }

    private OSS createClient() {
        if (StrUtil.isBlank(properties.getEndpoint())
                || StrUtil.isBlank(properties.getBucketName())
                || StrUtil.isBlank(properties.getRegion())) {
            throw new BizIllegalException("OSS 配置不完整：请配置 mzlhsc.aliyun.endpoint/bucket-name/region");
        }

        ClientBuilderConfiguration conf = new ClientBuilderConfiguration();
        // V4 签名（2024 年后新建 Bucket 强制要求）
        conf.setSignatureVersion(SignVersion.V4);

        var builder = OSSClientBuilder.create()
                .endpoint(properties.getEndpoint())
                .clientConfiguration(conf)
                .region(properties.getRegion());

        // 凭证优先级：配置文件 > 环境变量
        if (StrUtil.isNotBlank(properties.getAccessKeyId())) {
            builder.credentialsProvider(new DefaultCredentialProvider(
                    properties.getAccessKeyId(), properties.getAccessKeySecret()));
        } else {
            try {
                builder.credentialsProvider(
                        CredentialsProviderFactory.newEnvironmentVariableCredentialsProvider());
            } catch (Exception e) {
                throw new BizIllegalException(
                        "未配置 OSS AccessKey：请在 mzlhsc.aliyun.access-key-id 填写，"
                                + "或设置环境变量 OSS_ACCESS_KEY_ID / OSS_ACCESS_KEY_SECRET");
            }
        }

        OSS client = builder.build();
        log.info("OSS 客户端初始化完成: endpoint={}, bucket={}",
                properties.getEndpoint(), properties.getBucketName());
        return client;
    }

    /**
     * 按日期分目录 + UUID 重命名，例如 2026/11/2f1c....png
     */
    private String buildObjectName(String originalFilename) {
        String dir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
        String extension = "";
        int dot = originalFilename == null ? -1 : originalFilename.lastIndexOf('.');
        if (dot >= 0) {
            extension = originalFilename.substring(dot);
        }
        return dir + "/" + UUID.randomUUID() + extension;
    }

    /**
     * 拼出外网访问地址：https://{bucket}.{host}/{objectName}
     */
    private String buildUrl(String objectName) {
        String endpoint = properties.getEndpoint();
        String host = endpoint.substring(endpoint.indexOf("//") + 2);
        if (host.endsWith("/")) {
            host = host.substring(0, host.length() - 1);
        }
        return "https://" + properties.getBucketName() + "." + host + "/" + objectName;
    }

    @PreDestroy
    public void shutdown() {
        if (ossClient != null) {
            ossClient.shutdown();
            log.info("OSS 客户端已关闭");
        }
    }
}
