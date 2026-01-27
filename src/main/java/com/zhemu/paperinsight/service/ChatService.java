package com.zhemu.paperinsight.service;

import com.zhemu.paperinsight.model.vo.ChatMessageVO;
import com.zhemu.paperinsight.model.vo.ChatSessionVO;
import reactor.core.publisher.Flux;
import org.springframework.http.codec.ServerSentEvent;

import java.util.List;

/**
 * 论文对话服务接口
 * 
 * @author lushihao
 */
public interface ChatService {

    /**
     * 流式对话
     *
     * @param paperId 论文ID
     * @param userId  用户ID
     * @param message 用户消息
     * @return SSE流
     */
    Flux<ServerSentEvent<String>> streamChat(Long paperId, Long userId, String message);

    /**
     * 获取会话历史
     *
     * @param paperId 论文ID
     * @param userId  用户ID
     * @return 消息列表
     */
    List<ChatMessageVO> getChatHistory(Long paperId, Long userId);

    /**
     * 获取或创建会话
     *
     * @param paperId 论文ID
     * @param userId  用户ID
     * @return 会话信息
     */
    ChatSessionVO getOrCreateSession(Long paperId, Long userId);

    /**
     * 删除会话 (清空历史)
     *
     * @param paperId 论文ID
     * @param userId  用户ID
     * @return 是否成功
     */
    boolean deleteSession(Long paperId, Long userId);
}
