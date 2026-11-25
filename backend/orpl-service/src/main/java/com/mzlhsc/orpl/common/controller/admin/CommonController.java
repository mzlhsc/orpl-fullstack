package com.mzlhsc.orpl.common.controller.admin;

import cn.hutool.core.util.StrUtil;
import com.mzlhsc.orpl.common.properties.StorageProperties;
import com.mzlhsc.orpl.common.result.R;
import com.mzlhsc.orpl.common.utils.AliyunOSSOperator;
import com.mzlhsc.orpl.common.utils.LocalFileStorage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * 通用接口：文件上传
 * <p>
 * 存储方式由 {@code mzlhsc.storage.type} 决定：
 * <ul>
 *   <li>auto（默认）：OSS 配置齐全就传 OSS，否则落到服务器本地目录</li>
 *   <li>oss：强制走阿里云 OSS，缺配置时直接报错</li>
 *   <li>local：强制存本地，图片由 nginx / 应用静态映射提供访问</li>
 * </ul>
 */
@RequestMapping("/admin/common")
@RestController
@RequiredArgsConstructor
@Tag(name = "通用接口", description = "文件上传")
@Slf4j
public class CommonController {

    private final AliyunOSSOperator aliyunOSSOperator;
    private final LocalFileStorage localFileStorage;
    private final StorageProperties storageProperties;

    /**
     * 上传文件
     *
     * @return 可直接访问的图片 URL
     */
    @PostMapping("/upload")
    @Operation(summary = "文件上传", description = "未配置 OSS 时自动保存到服务器本地，返回可访问的 URL")
    public R<String> upload(MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            return R.error("上传文件不能为空");
        }
        String mode = resolveMode();
        log.info("文件上传: {}, 大小 {} 字节, 存储方式 {}", file.getOriginalFilename(), file.getSize(), mode);
        String url = "oss".equals(mode)
                ? aliyunOSSOperator.upload(file.getBytes(), file.getOriginalFilename())
                : localFileStorage.upload(file.getBytes(), file.getOriginalFilename());
        return R.ok(absoluteUrl(url));
    }

    /**
     * 相对地址在 Web 端能直接显示，但小程序 / 安卓里会被当成包内资源或本地文件，
     * 所以统一补成当前访问域名下的绝对地址（nginx 会把原始 Host 透传给后端）。
     */
    private String absoluteUrl(String url) {
        if (url == null || url.isEmpty()
                || url.startsWith("http://") || url.startsWith("https://")) {
            return url;
        }
        String base = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
        return url.startsWith("/") ? base + url : base + "/" + url;
    }

    private String resolveMode() {
        String type = StrUtil.blankToDefault(storageProperties.getType(), "auto").trim().toLowerCase();
        if ("oss".equals(type)) {
            return "oss";
        }
        if ("local".equals(type)) {
            return "local";
        }
        return aliyunOSSOperator.isConfigured() ? "oss" : "local";
    }
}
