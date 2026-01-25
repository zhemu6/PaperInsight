package com.zhemu.paperinsight.common;

/**
 * 用户上下文，用于存储当前线程的登录用户ID
 * 供 MyBatis Plus 多租户插件使用
 * 
 * @author lushihao
 */
public class UserContext {
    private static final ThreadLocal<Long> USER_ID_HOLDER = new ThreadLocal<>();

    public static void setUserId(Long userId) {
        USER_ID_HOLDER.set(userId);
    }

    public static Long getUserId() {
        return USER_ID_HOLDER.get();
    }

    public static void clear() {
        USER_ID_HOLDER.remove();
    }
}
