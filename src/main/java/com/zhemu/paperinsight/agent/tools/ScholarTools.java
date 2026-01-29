package com.zhemu.paperinsight.agent.tools;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import reactor.core.publisher.Mono;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 学术工具集
 * 演示用，包含论文检索相关功能
 */
@Component("scholarTools")
@Slf4j
public class ScholarTools {

    @Tool(name = "search-paper", description = "模拟搜索论文")
    public Mono<String> searchPaper(
            @ToolParam(name = "keyword", description = "搜索关键词") String keyword) {
        return Mono.fromSupplier(() -> {
            log.info("Searching paper with keyword: {}", keyword);
            return String.format("Found 3 papers about '%s': 1. AgentScope Review, 2. LLM Agents, 3. RAG Systems",
                    keyword);
        });
    }

    @Tool(name = "get-paper-citation", description = "模拟获取论文引用")
    public Mono<String> getPaperCitation(
            @ToolParam(name = "title", description = "论文标题") String title) {
        return Mono.fromSupplier(() -> {
            log.info("Getting citation for: {}", title);
            return String.format("Citation for '%s': Smith et al., 2024, arXiv:2401.xxxxx", title);
        });
    }
}
