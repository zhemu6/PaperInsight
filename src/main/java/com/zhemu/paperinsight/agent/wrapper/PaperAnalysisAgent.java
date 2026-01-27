package com.zhemu.paperinsight.agent.wrapper;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.zhemu.paperinsight.agent.core.InnovationAgent;
import com.zhemu.paperinsight.agent.core.MethodologyAgent;
import com.zhemu.paperinsight.agent.core.ScoreAgent;
import com.zhemu.paperinsight.agent.core.SummaryAgent;
import com.zhemu.paperinsight.agent.tools.PdfExtractionTool;
import com.zhemu.paperinsight.model.entity.PaperInsight;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.TextBlock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple4;

import java.time.Duration;

/**
 * 论文分析智能体包装类 (Orchestrator)
 * 负责协调 PDF 提取和多个子智能体(Agent)的并行工作
 * 
 * @author lushihao
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaperAnalysisAgent {

    private final PdfExtractionTool pdfExtractionTool;
    private final SummaryAgent summaryAgent;
    private final InnovationAgent innovationAgent;
    private final MethodologyAgent methodologyAgent;
    private final ScoreAgent scoreAgent;

    /**
     * 执行全量分析
     * 
     * @param paperId 论文ID
     * @param pdfUrl  PDF文件地址
     * @return 分析结果实体
     */
    public Mono<PaperInsight> analyzePaper(Long paperId, String pdfUrl) {
        return Mono.fromCallable(() -> pdfExtractionTool.extractPdfText(pdfUrl))
                .flatMap(text -> {
                    // 并行执行4个分析任务
                    Mono<Msg> summaryTask = summaryAgent.analyze(text);
                    Mono<Msg> innovationTask = innovationAgent.analyze(text);
                    Mono<Msg> methodologyTask = methodologyAgent.analyze(text);
                    Mono<Msg> scoreTask = scoreAgent.analyze(text);

                    return Mono.zip(summaryTask, innovationTask, methodologyTask, scoreTask)
                            .map(tuple -> buildPaperInsight(paperId, tuple))
                            .timeout(Duration.ofMinutes(10)); // 防止一直卡住
                });
    }

    private PaperInsight buildPaperInsight(Long paperId, Tuple4<Msg, Msg, Msg, Msg> tuple) {
        String summary = extractContent(tuple.getT1());
        String innovation = extractContent(tuple.getT2());
        String methods = extractContent(tuple.getT3());
        String scoreJson = extractContent(tuple.getT4());

        Integer finalScore = 0;
        try {
            // 尝试从JSON中解析分数
            JSONObject json = JSONUtil.parseObj(scoreJson);
            finalScore = json.getInt("score", 0);
        } catch (Exception e) {
            log.warn("Failed to parse score JSON: {}", scoreJson);
        }

        return PaperInsight.builder()
                .paperId(paperId)
                .summaryMarkdown(summary)
                .innovationPoints(innovation)
                .methods(methods)
                .score(finalScore)
                .scoreDetails(scoreJson)
                .build();
    }

    private String extractContent(Msg msg) {
        if (msg == null || msg.getContent() == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (var block : msg.getContent()) {
            if (block instanceof TextBlock textBlock) {
                sb.append(textBlock.getText()).append("\n");
            }
        }
        return sb.toString().trim();
    }
}
