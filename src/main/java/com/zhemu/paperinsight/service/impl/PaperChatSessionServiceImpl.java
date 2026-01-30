package com.zhemu.paperinsight.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhemu.paperinsight.exception.ErrorCode;
import com.zhemu.paperinsight.exception.ThrowUtils;
import com.zhemu.paperinsight.mapper.PaperChatSessionMapper;
import com.zhemu.paperinsight.model.entity.PaperChatSession;
import com.zhemu.paperinsight.model.entity.PaperInfo;
import com.zhemu.paperinsight.model.vo.ChatSessionVO;
import com.zhemu.paperinsight.service.PaperChatSessionService;
import com.zhemu.paperinsight.service.PaperInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaperChatSessionServiceImpl extends ServiceImpl<PaperChatSessionMapper, PaperChatSession>
        implements PaperChatSessionService {

    private static final int TITLE_MAX_LEN = 40;

    private final PaperInfoService paperInfoService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaperChatSession createSession(long paperId, long userId) {
        ThrowUtils.throwIf(paperId <= 0, ErrorCode.PARAMS_ERROR);

        // Allow create if paper is owned by user OR public
        PaperInfo paper = paperInfoService.getOne(new QueryWrapper<PaperInfo>()
                .eq("id", paperId)
                .and(qw -> qw.eq("user_id", userId).or().eq("is_public", 1)));
        ThrowUtils.throwIf(paper == null, ErrorCode.NOT_FOUND_ERROR);

        String id = UUID.randomUUID().toString();
        PaperChatSession session = PaperChatSession.builder()
                .id(id)
                .paperId(paperId)
                .userId(userId)
                .title(null)
                .lastMessageAt(null)
                .build();
        boolean ok = this.save(session);
        ThrowUtils.throwIf(!ok, ErrorCode.OPERATION_ERROR);
        return session;
    }

    @Override
    public List<ChatSessionVO> listSessions(long paperId, long userId) {
        ThrowUtils.throwIf(paperId <= 0, ErrorCode.PARAMS_ERROR);
        List<PaperChatSession> list = this.list(new QueryWrapper<PaperChatSession>()
                .eq("paper_id", paperId)
                .eq("user_id", userId)
                .orderByDesc("update_time"));
        return list.stream().map(this::toVO).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteSession(String chatId, long userId) {
        PaperChatSession session = getOwnedSession(chatId, userId);
        if (session == null) {
            return true;
        }
        return this.removeById(chatId);
    }

    @Override
    public PaperChatSession getOwnedSession(String chatId, long userId) {
        ThrowUtils.throwIf(StrUtil.isBlank(chatId), ErrorCode.PARAMS_ERROR);
        return this.getOne(new QueryWrapper<PaperChatSession>()
                .eq("id", chatId)
                .eq("user_id", userId));
    }

    /**
     *  更新最新时间
     * @param chatId 对话id
     * @param userId 用户id
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void touchSession(String chatId, long userId) {
        PaperChatSession session = getOwnedSession(chatId, userId);
        ThrowUtils.throwIf(session == null, ErrorCode.NO_AUTH_ERROR);
        PaperChatSession toUpdate = new PaperChatSession();
        toUpdate.setLastMessageAt(LocalDateTime.now());
        this.update(toUpdate, new QueryWrapper<PaperChatSession>()
                .eq("id", chatId)
                .eq("user_id", userId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void ensureTitle(String chatId, long userId, String firstQuestion) {
        if (StrUtil.isBlank(firstQuestion)) {
            return;
        }
        PaperChatSession session = getOwnedSession(chatId, userId);
        ThrowUtils.throwIf(session == null, ErrorCode.NO_AUTH_ERROR);
        if (StrUtil.isNotBlank(session.getTitle())) {
            return;
        }

        String title = firstQuestion.trim();
        title = title.replaceAll("\\s+", " ");
        if (title.length() > TITLE_MAX_LEN) {
            title = title.substring(0, TITLE_MAX_LEN);
        }

        PaperChatSession toUpdate = new PaperChatSession();
        toUpdate.setTitle(title);
        toUpdate.setLastMessageAt(LocalDateTime.now());
        this.update(toUpdate, new QueryWrapper<PaperChatSession>()
                .eq("id", chatId)
                .eq("user_id", userId));
    }

    private ChatSessionVO toVO(PaperChatSession entity) {
        ChatSessionVO vo = new ChatSessionVO();
        vo.setChatId(entity.getId());
        vo.setPaperId(entity.getPaperId());
        vo.setTitle(entity.getTitle());
        vo.setLastMessageAt(entity.getLastMessageAt());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }
}
