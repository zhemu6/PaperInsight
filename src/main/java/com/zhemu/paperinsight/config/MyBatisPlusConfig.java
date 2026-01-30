package com.zhemu.paperinsight.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.zhemu.paperinsight.common.UserContext;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis Plus 配置
 * 包括：多租户插件
 * 
 * @author lushihao
 */
@Configuration
@MapperScan("com.zhemu.paperinsight.mapper")
public class MyBatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 添加多租户插件
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(new TenantLineHandler() {
            @Override
            public Expression getTenantId() {
                // 从 ThreadLocal 中获取当前登录用户的 ID
                Long userId = UserContext.getUserId();
                if (userId == null) {
                    // 如果没有登录（例如注册/登录接口），则返回 null 或 throw 异常
                    // 在这里我们返回一个不存在的 ID 防止误查，或者根据表名忽略
                    return new LongValue(0);
                }
                return new LongValue(userId);
            }

            @Override
            public String getTenantIdColumn() {
                // 数据库中表示租户ID的列名
                return "user_id";
            }

            @Override
            public boolean ignoreTable(String tableName) {
                // 忽略系统用户表 (登录注册必须能查全表)
                // 忽略 paper_insight 表 (它通过 paper_id 关联，不直接关联 user_id，或者后续设计加上)
                // 根据当前设计 paper_insight 一般是一对一 paper_info，间接属于用户。
                // 暂时忽略 sys_user；另外 announcement 为全局表，不包含 user_id，需要忽略租户条件
                return "sys_user".equalsIgnoreCase(tableName)
                        || "announcement".equalsIgnoreCase(tableName);
            }
        }));

        // 添加分页插件
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));

        return interceptor;
    }
}
