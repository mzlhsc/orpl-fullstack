package com.mzlhsc.orpl.common.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 单体探活接口，部署脚本/负载均衡可直接 GET /health。
 */
@RestController
public class HealthController {

    @GetMapping("/health")
    public String health() {
        return "order-platform-ok";
    }
}
