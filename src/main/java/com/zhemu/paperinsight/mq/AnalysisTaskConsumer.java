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
import com.zhemu.paperinsight.model.entity.Notification;
import com.zhemu.paperinsight.model.enums.NotificationTypeEnum;
import com.zhemu.paperinsight.model.entity.PaperInfo;
import com.zhemu.paperinsight.service.PaperInfoService;
import com.zhemu.paperinsight.service.NotificationService;
import com.zhemu.paperinsight.service.PaperInsightService;
import io.agentscope.core.rag.Knowledge;
import io.agentscope.core.rag.model.Document;
import io.agentscope.core.rag.model.DocumentMetadata;
import io.agentscope.core.rag.reader.ReaderInput;
import io.agentscope.core.rag.reader.SplitStrategy;
import io.agentscope.core.rag.reader.TextReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
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
    private final NotificationService notificationService;
    private final PaperInfoService paperInfoService;
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

                // 3.0 写入用户通知（幂等）
                createNotificationSafely(task.getUserId(), task.getPaperId(), NotificationTypeEnum.PaperAnalysisSuccess,
                        "论文已完成分析，请前往详情页查看。",
                        paperInsight);

                // 3.1 RAG 入库
                try {
                    log.info("Starting RAG indexing for paperId: {}", task.getPaperId());

                    // 使用 PdfExtractionTool 提取文本（它内部处理了 URL 下载）
                    String extractedText = pdfExtractionTool.extractPdfText(task.getPdfUrl());

                    if (extractedText != null && !extractedText.isEmpty() && !extractedText.startsWith("Error")) {
                        // 使用 TextReader 切分文本
                        TextReader textReader = new TextReader(512, SplitStrategy.PARAGRAPH, 50);
                        List<Document> docs = textReader.read(ReaderInput.fromString(extractedText)).block();
                        
                        // 显式设置 docId 为 paperId，确保后续可以反查
                        // 注意: DocumentMetadata 可能是 Record (Java 14+) 或不可变对象，因此我们重建 Document
                        if (docs != null) {
                            List<Document> newDocs = docs.stream().map(doc -> {
                                DocumentMetadata oldMeta = doc.getMetadata();
                                // 重建 Metadata，将 docId 设为 paperId
                                DocumentMetadata newMeta = new DocumentMetadata(
                                        oldMeta.getContent(),
                                        String.valueOf(task.getPaperId()), // 设置 paperId
                                        oldMeta.getChunkId()

                                );
                                // 重建 Document
                                Document newDoc = new Document(newMeta);
                                newDoc.setEmbedding(doc.getEmbedding()); // 保留 embedding (虽然此时通常为空)
                                newDoc.setScore(doc.getScore());
                                return newDoc;
                            }).collect(Collectors.toList());

                            knowledge.addDocuments(newDocs).block();
                            log.info("RAG indexing completed for paperId: {}, docs count: {}", task.getPaperId(),
                                    newDocs.size());
                        }
                    } else {
                        log.warn("Failed to extract text from PDF for paperId: {}", task.getPaperId());
                    }
                } catch (Exception e) {
                    log.error("RAG indexing failed for paperId: {}", task.getPaperId(), e);
                    // 不阻断流程，仅记录错误
                }

            } else {
                log.error("Analysis returned null for paperId: {}", task.getPaperId());

                createNotificationSafely(task.getUserId(), task.getPaperId(), NotificationTypeEnum.PaperAnalysisFailed,
                        "论文分析失败（空结果），请稍后重试。",
                        null);
            }

            // 4. 确认消息
            channel.basicAck(deliveryTag, false);

        } catch (Exception e) {
            log.error("Failed to process analysis task", e);

            // 尝试写入失败通知（不影响 nack）
            try {
                PaperAnalysisMessage task = JSONUtil.toBean(messageStr, PaperAnalysisMessage.class);
                if (task != null && task.getPaperId() != null) {
                    if (task.getUserId() != null) {
                        UserContext.setUserId(task.getUserId());
                    }
                    createNotificationSafely(task.getUserId(), task.getPaperId(), NotificationTypeEnum.PaperAnalysisFailed,
                            "论文分析失败（系统异常），请稍后重试。",
                            null);
                }
            } catch (Exception ignored) {
                // ignore
            }
            try {
                channel.basicNack(deliveryTag, false, false);
            } catch (IOException ex) {
                log.error("Failed to nack message", ex);
            }
        } finally {
            UserContext.clear();
        }
    }

    private void createNotificationSafely(Long userId, Long paperId, NotificationTypeEnum type, String content,
            PaperInsight insight) {
        if (userId == null || paperId == null) {
            return;
        }
        try {
            PaperInfo paperInfo = null;
            try {
                paperInfo = paperInfoService.getById(paperId);
            } catch (Exception ignored) {
                // ignore
            }
            String paperTitle = paperInfo == null ? null : paperInfo.getTitle();

            String dedupKey = String.format("%s:%s", type.getValue(), paperId);
            cn.hutool.json.JSONObject payload = new cn.hutool.json.JSONObject();
            payload.set("paperId", paperId);
            if (paperTitle != null) {
                payload.set("paperTitle", paperTitle);
            }
            if (insight != null) {
                payload.set("score", insight.getScore());
            }

            String title;
            if (paperTitle != null) {
                title = String.format("%s：%s", type.getDesc(), paperTitle);
            } else {
                title = type.getDesc();
            }

            Notification notification = Notification.builder()
                    .userId(userId)
                    .type(type.getValue())
                    .title(title)
                    .content(content)
                    .payloadJson(payload.toString())
                    .dedupKey(dedupKey)
                    .build();
            notificationService.save(notification);
        } catch (DuplicateKeyException e) {
            // dedupKey 冲突，视为成功
        } catch (Exception e) {
            log.warn("Failed to create notification for paperId: {}", paperId, e);
        }
    }
}
