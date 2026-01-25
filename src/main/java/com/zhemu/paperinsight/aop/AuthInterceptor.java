package com.zhemu.paperinsight.aop;

import com.zhemu.paperinsight.annotation.AuthCheck;
import com.zhemu.paperinsight.common.UserContext;
import com.zhemu.paperinsight.constant.RedisConstants;
import com.zhemu.paperinsight.exception.BusinessException;
import com.zhemu.paperinsight.exception.ErrorCode;
import com.zhemu.paperinsight.model.entity.SysUser;
import com.zhemu.paperinsight.model.enums.UserRoleEnum;
import com.zhemu.paperinsight.service.SysUserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * @author lushihao
 */
@Aspect
@Component
@RequiredArgsConstructor
public class AuthInterceptor {

    private final SysUserService userService;

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 执行拦截
     *
     * @param joinPoint 切入点
     * @param authCheck 权限校验注解
     */
    @Around("@annotation(authCheck)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {
        String mustRole = authCheck.mustRole();
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
        // 当前登录用户
        SysUser loginUser = userService.getLoginUser(request);

        // --- 访客统计 (Redis HyperLogLog) ---
        try {
            if (loginUser != null && loginUser.getId() != null) {
                String userId = String.valueOf(loginUser.getId());
                // e.g. 2025-01-08
                String date = cn.hutool.core.date.DateUtil.today();
                // 日活
                stringRedisTemplate.opsForHyperLogLog()
                        .add(RedisConstants.VISITOR_DAILY_KEY + date, userId);
                // 总访客
                stringRedisTemplate.opsForHyperLogLog()
                        .add(RedisConstants.VISITOR_TOTAL_KEY, userId);
            }
        } catch (Exception e) {
            // 统计失败不影响业务
            // log.error("Visitor count error", e);
        }
        // ----------------------------------

        // ----------------------------------

        if (loginUser != null) {
            UserContext.setUserId(loginUser.getId());
        }

        try {
            UserRoleEnum mustRoleEnum = UserRoleEnum.getEnumByValue(mustRole);
            // 不需要权限，放行
            if (mustRoleEnum == null) {
                return joinPoint.proceed();
            }
            // 以下为：必须有该权限才通过
            // 获取当前用户具有的权限
            UserRoleEnum userRoleEnum = UserRoleEnum.getEnumByValue(loginUser.getUserRole());
            // 没有权限，拒绝
            if (userRoleEnum == null) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
            // 要求必须有管理员权限，但用户没有管理员权限，拒绝
            if (UserRoleEnum.Admin.equals(mustRoleEnum) && !UserRoleEnum.Admin.equals(userRoleEnum)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
            // 通过权限校验，放行
            return joinPoint.proceed();
        } finally {
            UserContext.clear();
        }
    }
}
