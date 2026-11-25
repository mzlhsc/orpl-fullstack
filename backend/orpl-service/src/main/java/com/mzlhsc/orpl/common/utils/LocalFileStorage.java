package com.mzlhsc.orpl.common.utils;

import com.mzlhsc.orpl.common.exception.BizIllegalException;
import com.mzlhsc.orpl.common.properties.StorageProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 本地文件存储。
 * <p>
 * 未配置阿里云 OSS 时作为兜底：图片按 {@code yyyy/MM/随机名.后缀} 落到
 * {@code mzlhsc.storage.local-dir}，返回 {@code 前缀/相对路径} 形式的对外 URL。
 * 生产环境把目录指向 {@code /opt/orpl/web/uploads}，由 nginx 直接提供访问；
 * 本地开发时由 {@link com.mzlhsc.orpl.common.webconfig.LocalStorageWebConfig} 做静态资源映射。
 */
@Slf4j
@Component
public class LocalFileStorage {

    /** 只允许常见图片/文档后缀，避免路径穿越 */
    private static final Pattern SAFE_EXT = Pattern.compile("^\\.[a-z0-9]{1,8}$");

    private final StorageProperties properties;

    public LocalFileStorage(StorageProperties properties) {
        this.properties = properties;
    }

    /**
     * 保存文件
     *
     * @param content          文件字节
     * @param originalFilename 原始文件名（只取后缀）
     * @return 可通过 HTTP 访问的相对地址，如 /uploads/2026/11/xxx.png
     */
    public String upload(byte[] content, String originalFilename) {
        if (content == null || content.length == 0) {
            throw new BizIllegalException("上传文件内容不能为空");
        }

        String objectName = buildObjectName(originalFilename);
        Path root = rootDir();
        Path target = root.resolve(objectName).normalize();
        if (!target.startsWith(root)) {
            throw new BizIllegalException("非法的文件路径");
        }

        try {
            Files.createDirectories(target.getParent());
            Files.write(target, content,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        } catch (IOException e) {
            log.error("本地文件写入失败: {}", target, e);
            throw new BizIllegalException("文件保存失败：" + e.getMessage());
        }

        String url = urlPrefix() + "/" + objectName;
        log.info("文件已保存到本地: {} -> {}", target, url);
        return url;
    }

    /** 本地存储根目录的绝对路径 */
    public Path rootDir() {
        return Paths.get(properties.getLocalDir()).toAbsolutePath().normalize();
    }

    /** 对外访问前缀，保证以 / 开头、不以 / 结尾 */
    public String urlPrefix() {
        String prefix = properties.getPublicPrefix() == null ? "" : properties.getPublicPrefix().trim();
        if (prefix.isEmpty()) {
            return "";
        }
        if (!prefix.startsWith("/")) {
            prefix = "/" + prefix;
        }
        while (prefix.endsWith("/")) {
            prefix = prefix.substring(0, prefix.length() - 1);
        }
        return prefix;
    }

    private String buildObjectName(String originalFilename) {
        String dir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
        String extension = "";
        int dot = originalFilename == null ? -1 : originalFilename.lastIndexOf('.');
        if (dot >= 0 && dot < originalFilename.length() - 1) {
            String candidate = originalFilename.substring(dot).toLowerCase();
            if (SAFE_EXT.matcher(candidate).matches()) {
                extension = candidate;
            }
        }
        return dir + "/" + UUID.randomUUID().toString().replace("-", "") + extension;
    }
}

