package com.mzlhsc.orpl.common.context;

/**
 * 用户信息存放点工具类
 * <p>
 * 注意：这里存的 id 可能是「用户 id」也可能是「员工 id」，业务含义由 {@link Identity} 决定，
 * 下游使用前先用 {@link #isAdmin()} 判断，别把员工 id 当用户 id 用。
 */
public class UserContext {

    /**
     * 当前登录者身份类型
     */
    public enum Identity {
        /** 管理端员工，对应请求头 empId */
        ADMIN,
        /** 用户端用户，对应请求头 userId */
        USER
    }

    private static final ThreadLocal<Long> ID = new ThreadLocal<>();
    private static final ThreadLocal<Identity> IDENTITY = new ThreadLocal<>();

    /**
     * 保存当前登录者 id（不带身份类型，兼容老代码）
     */
    public static void setCurrentId(Long currentId) {
        ID.set(currentId);
    }

    /**
     * 保存当前登录者 id 及其身份类型
     */
    public static void setCurrentId(Long currentId, Identity identity) {
        ID.set(currentId);
        IDENTITY.set(identity);
    }

    /**
     * 获取当前登录者 id
     */
    public static Long getCurrentId() {
        return ID.get();
    }

    /**
     * 获取当前登录者身份类型，可能为 null
     */
    public static Identity getIdentity() {
        return IDENTITY.get();
    }

    /**
     * 当前是否为管理端员工
     */
    public static boolean isAdmin() {
        return Identity.ADMIN == IDENTITY.get();
    }

    /**
     * 移除当前登录用户信息（必须在请求结束时调用，否则线程池复用会串号）
     */
    public static void removeCurrent() {
        ID.remove();
        IDENTITY.remove();
    }
}
