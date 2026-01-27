package com.zhemu.paperinsight.mq;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.rabbitmq.client.Channel;
import com.zhemu.paperinsight.agent.common.ElasticsearchStore;
import com.zhemu.paperinsight.agent.tools.PdfExtractionTool;
import com.zhemu.paperinsight.agent.wrapper.PaperAnalysisAgent;
import com.zhemu.paperinsight.common.UserContext;
import com.zhemu.paperinsight.config.RabbitMqConfig;
import com.zhemu.paperinsight.model.dto.mq.PaperAnalysisMessage;
import com.zhemu.paperinsight.model.entity.PaperInsight;
import com.zhemu.paperinsight.service.PaperInsightService;
import io.agentscope.core.rag.Knowledge;
import io.agentscope.core.rag.model.Document;
import io.agentscope.core.rag.reader.ReaderInput;
import io.agentscope.core.rag.reader.SplitStrategy;
import io.agentscope.core.rag.reader.TextReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 论文分析任务消费者
 *
 * @author lushihao
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AnalysisTaskConsumer {

    private final PaperAnalysisAgent paperAnalysisAgent;
    private final PaperInsightService paperInsightService;
    private final Knowledge knowledge;
    private final PdfExtractionTool pdfExtractionTool;
    private final ElasticsearchStore elasticsearchStore;

    @RabbitListener(queues = RabbitMqConfig.PAPER_ANALYSIS_QUEUE, ackMode = "MANUAL")
    public void receiveAnalysisTask(String messageStr, Channel channel, Message message) {
        log.info("Received analysis task: {}", messageStr);
        long deliveryTag = message.getMessageProperties().getDeliveryTag();

        try {
            // 1. 解析消息
            PaperAnalysisMessage task = JSONUtil.toBean(messageStr, PaperAnalysisMessage.class);
            if (task == null || task.getPaperId() == null) {
                log.warn("Invalid message format: {}", messageStr);
                channel.basicAck(deliveryTag, false);
                return;
            }

            // 设置用户上下文
            if (task.getUserId() != null) {
                UserContext.setUserId(task.getUserId());
            }

            // 2. 调用 Agent 进行分析 (同步等待结果)
            PaperInsight paperInsight = paperAnalysisAgent.analyzePaper(task.getPaperId(), task.getPdfUrl()).block();

            if (paperInsight != null) {
                // 3. 更新数据库
                UpdateWrapper<PaperInsight> updateWrapper = new UpdateWrapper<>();
                updateWrapper.eq("paper_id", task.getPaperId());
                paperInsightService.update(paperInsight, updateWrapper);
                log.info("Analysis completed and saved for paperId: {}", task.getPaperId());

                // 3.1 RAG 入库
                try {
                    log.info("Starting RAG indexing for paperId: {}", task.getPaperId());

                    // 使用 PdfExtractionTool 提取文本（它内部处理了 URL 下载）
                    String extractedText = pdfExtractionTool.extractPdfText(task.getPdfUrl());

                    if (extractedText != null && !extractedText.isEmpty() && !extractedText.startsWith("Error")) {
                        // 使用 TextReader 切分文本
                        TextReader textReader = new TextReader(512, SplitStrategy.PARAGRAPH, 50);
                        List<Document> docs = textReader.read(ReaderInput.fromString(extractedText)).block();
                        // 执行入库
                        knowledge.addDocuments(docs).block();
                        log.info("RAG indexing completed for paperId: {}, docs count: {}", task.getPaperId(),
                                docs.size());
                    } else {
                        log.warn("Failed to extract text from PDF for paperId: {}", task.getPaperId());
                    }
                } catch (Exception e) {
                    log.error("RAG indexing failed for paperId: {}", task.getPaperId(), e);
                    // 不阻断流程，仅记录错误
                }

            } else {
                log.error("Analysis returned null for paperId: {}", task.getPaperId());
            }

            // 4. 确认消息
            channel.basicAck(deliveryTag, false);

        } catch (Exception e) {
            log.error("Failed to process analysis task", e);
            try {
                channel.basicNack(deliveryTag, false, false);
            } catch (IOException ex) {
                log.error("Failed to nack message", ex);
            }
        } finally {
            UserContext.clear();
        }
    }
}
