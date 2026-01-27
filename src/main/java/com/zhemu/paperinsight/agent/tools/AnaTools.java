package com.zhemu.paperinsight.agent.tools;

import cn.hutool.core.date.DateUtil;
import io.agentscope.core.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author: lushihao
 * @version: 1.0
 *           create: 2026-01-26 21:11
 */
@Component
@Slf4j
public class AnaTools {

    @Tool(name = "get-now-time", description = "获取当前时间")
    public String getNowTime() {
        return DateUtil.now();
    }
}
