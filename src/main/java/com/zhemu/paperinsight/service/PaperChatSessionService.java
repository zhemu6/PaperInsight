package com.zhemu.paperinsight.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhemu.paperinsight.model.entity.PaperChatSession;
import com.zhemu.paperinsight.model.vo.ChatSessionVO;
import java.util.List;

public interface PaperChatSessionService extends IService<PaperChatSession> {

    PaperChatSession createSession(long paperId, long userId);

    List<ChatSessionVO> listSessions(long paperId, long userId);

    boolean deleteSession(String chatId, long userId);

    PaperChatSession getOwnedSession(String chatId, long userId);

    void touchSession(String chatId, long userId);

    void ensureTitle(String chatId, long userId, String firstQuestion);
}
