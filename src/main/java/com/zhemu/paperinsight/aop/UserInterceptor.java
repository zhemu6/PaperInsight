package com.zhemu.paperinsight.aop;

import com.zhemu.paperinsight.common.UserContext;
import com.zhemu.paperinsight.constant.UserConstant;
import com.zhemu.paperinsight.model.entity.SysUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 用户拦截器
 * 用于将当前登录用户 ID 放入 ThreadLocal
 * 
 * @author lushihao
 */
@Component
public class UserInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        if (userObj instanceof SysUser) {
            SysUser user = (SysUser) userObj;
            UserContext.setUserId(user.getId());
        }
        return true; // 无论是否登录都放行，鉴权交给 Sa-Token 或 AuthCheck 注解，这里只负责填 UserContext
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
            throws Exception {
        // 请求结束后清理 ThreadLocal，防止内存泄漏
        UserContext.clear();
    }
}
