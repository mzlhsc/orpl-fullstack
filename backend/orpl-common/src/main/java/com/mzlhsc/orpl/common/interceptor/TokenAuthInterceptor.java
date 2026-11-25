package com.mzlhsc.orpl.common.interceptor;

import com.mzlhsc.orpl.auth.config.AuthProperties;
import com.mzlhsc.orpl.auth.exception.UnauthorizedException;
import com.mzlhsc.orpl.auth.util.JwtTool;
import com.mzlhsc.orpl.common.context.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 原来网关 AuthGlobalFilter 的单体版本。
 *
 * <p>网关没了以后不再把 empId/userId 透传到下游，而是校验 JWT 后直接写入
 * {@link UserContext}，同一个进程内业务代码直接从 ThreadLocal 取身份。</p>
 *
 * <p>规则保持原样：请求头 zmlhsc / mzlhsc = 员工令牌，authorization = 用户令牌；
 * 员工令牌只能访问 /admin/**，用户令牌只能访问用户态接口；登录、商品浏览、
 * 扫码看桌台、接口文档等白名单不校验。单体内部模块直接走 Service 调用，
 * 不再保留 /inner/** 这种进程内 HTTP 接口。</p>
 */
@Component
@RequiredArgsConstructor
public class TokenAuthInterceptor implements HandlerInterceptor {

    private static final String HEADER_ADMIN = "zmlhsc";
    private static final String HEADER_ADMIN_ALT = "mzlhsc";
    private static final String HEADER_USER = "authorization";

    private final JwtTool jwtTool;
    private final AuthProperties authProperties;
    private final AntPathMatcher antPathMatcher = new AntPathMatcher();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 预检请求直接放行，交给 CORS 配置处理
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String path = request.getRequestURI();
        if (isExclude(path)) {
            return true;
        }

        String adminToken = request.getHeader(HEADER_ADMIN);
        if (!StringUtils.hasText(adminToken)) {
            adminToken = request.getHeader(HEADER_ADMIN_ALT);
        }
        String userToken = request.getHeader(HEADER_USER);

        UserContext.Identity identity;
        String token;
        if (StringUtils.hasText(adminToken)) {
            identity = UserContext.Identity.ADMIN;
            token = adminToken;
        } else if (StringUtils.hasText(userToken)) {
            identity = UserContext.Identity.USER;
            token = userToken;
        } else {
            return reject(response, "未登录");
        }

        if (!isIncludePath(path, identity)) {
            return reject(response, identity == UserContext.Identity.ADMIN
                    ? "员工令牌不能访问用户端接口"
                    : "用户令牌不能访问管理端接口");
        }

        Long id;
        try {
            id = identity == UserContext.Identity.ADMIN
                    ? jwtTool.parseEmpToken(token)
                    : jwtTool.parseUserToken(token);
        } catch (UnauthorizedException e) {
            return reject(response, e.getMessage());
        } catch (RuntimeException e) {
            return reject(response, "无效的token");
        }

        UserContext.setCurrentId(id, identity);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        // 线程池复用必须清理，否则会串号
        UserContext.removeCurrent();
    }

    private boolean isIncludePath(String path, UserContext.Identity identity) {
        List<String> patterns = identity == UserContext.Identity.ADMIN
                ? authProperties.getAdminIncludePaths()
                : authProperties.getUserIncludePaths();
        if (CollectionUtils.isEmpty(patterns)) {
            return antPathMatcher.match(identity == UserContext.Identity.ADMIN ? "/admin/**" : "/user/**", path);
        }
        for (String pattern : patterns) {
            if (antPathMatcher.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    private boolean isExclude(String path) {
        List<String> patterns = authProperties.getExcludePaths();
        if (CollectionUtils.isEmpty(patterns)) {
            return false;
        }
        for (String pattern : patterns) {
            if (antPathMatcher.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 直接写 JSON 响应，不再交给 Controller，保持原网关的行为。
     * 未登录/越权返回 401。
     */
    private boolean reject(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json;charset=UTF-8");
        String safe = message == null ? "未登录" : message.replace("\\", "\\\\").replace("\"", "\\\"");
        response.getWriter().write("{\"code\":" + HttpServletResponse.SC_UNAUTHORIZED + ",\"msg\":\"" + safe + "\"}");
        return false;
    }
}
