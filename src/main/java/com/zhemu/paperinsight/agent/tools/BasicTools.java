package com.zhemu.paperinsight.agent.tools;

import cn.hutool.core.date.DateUtil;
import io.agentscope.core.tool.Tool;
import reactor.core.publisher.Mono;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 基础工具集
 * 包含通用的基础功能
 */
@Component("basicTools")
@Slf4j
public class BasicTools {

    @Tool(name = "get-now-time", description = "获取当前时间")
    public Mono<String> getNowTime() {
        return Mono.fromSupplier(DateUtil::now);
    }

    
}
