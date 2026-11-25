package com.mzlhsc.orpl.auth.util;

import cn.hutool.core.exceptions.ValidateException;
import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTValidator;
import cn.hutool.jwt.signers.JWTSigner;
import cn.hutool.jwt.signers.JWTSignerUtil;
import com.mzlhsc.orpl.auth.exception.UnauthorizedException;

import java.security.KeyPair;
import java.time.Duration;
import java.util.Date;

/**
 * 令牌工具类：签发 + 解析。
 * JWT 拦截器校验和登录签发共用。
 */
public class JwtTool {

    public static final String EMP_ID = "empId";
    public static final String USER_ID = "userId";

    private final JWTSigner jwtSigner;

    public JwtTool(KeyPair keyPair) {
        this.jwtSigner = JWTSignerUtil.createSigner("rs256", keyPair);
    }

    /**
     * 创建管理端（员工）令牌
     */
    public String empCreateToken(Long empId, Duration ttl) {
        return JWT.create()
                .setPayload(EMP_ID, empId)
                .setExpiresAt(new Date(System.currentTimeMillis() + ttl.toMillis()))
                .setSigner(jwtSigner)
                .sign();
    }

    /**
     * 创建用户端令牌
     */
    public String userCreateToken(Long userId, Duration ttl) {
        return JWT.create()
                .setPayload(USER_ID, userId)
                .setExpiresAt(new Date(System.currentTimeMillis() + ttl.toMillis()))
                .setSigner(jwtSigner)
                .sign();
    }

    /**
     * 解析并校验令牌（不限定身份，兼容旧调用）
     *
     * @return 令牌里的 id（员工 id 或用户 id）
     */
    public Long parseToken(String token) {
        return parseToken(token, null);
    }

    /**
     * 解析员工令牌：必须携带 empId，用户令牌不能冒充员工
     */
    public Long parseEmpToken(String token) {
        return parseToken(token, EMP_ID);
    }

    /**
     * 解析用户令牌：必须携带 userId，员工令牌不能当用户令牌使用
     */
    public Long parseUserToken(String token) {
        return parseToken(token, USER_ID);
    }

    /**
     * 解析并校验令牌
     *
     * @param token    令牌
     * @param claimKey 期望的载荷字段；为 null 时 empId / userId 均可
     */
    private Long parseToken(String token, String claimKey) {
        // 1.校验token是否为空
        if (token == null || token.isBlank()) {
            throw new UnauthorizedException("未登录");
        }
        token = token.trim();
        // 兼容前端传 "Bearer xxx"
        if (token.regionMatches(true, 0, "Bearer ", 0, 7)) {
            token = token.substring(7).trim();
        }
        if (token.isEmpty()) {
            throw new UnauthorizedException("未登录");
        }
        // 2.校验并解析jwt
        JWT jwt;
        try {
            jwt = JWT.of(token).setSigner(jwtSigner);
        } catch (Exception e) {
            throw new UnauthorizedException("无效的token", e);
        }
        if (!jwt.verify()) {
            throw new UnauthorizedException("无效的token");
        }
        // 3.校验是否过期
        try {
            JWTValidator.of(jwt).validateDate();
        } catch (ValidateException e) {
            throw new UnauthorizedException("token已经过期");
        }
        // 4.数据格式校验：员工令牌读 empId，用户令牌读 userId
        Object payload = claimKey == null ? jwt.getPayload(EMP_ID) : jwt.getPayload(claimKey);
        if (payload == null && claimKey == null) {
            payload = jwt.getPayload(USER_ID);
        }
        if (payload == null) {
            throw new UnauthorizedException("无效的token");
        }
        // 5.数据解析
        try {
            return Long.valueOf(payload.toString());
        } catch (RuntimeException e) {
            throw new UnauthorizedException("无效的token");
        }
    }
}

