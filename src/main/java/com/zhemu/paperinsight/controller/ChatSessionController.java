package com.zhemu.paperinsight.controller;

import com.zhemu.paperinsight.annotation.AuthCheck;
import com.zhemu.paperinsight.common.BaseResponse;
import com.zhemu.paperinsight.common.ResultUtils;
import com.zhemu.paperinsight.exception.ErrorCode;
import com.zhemu.paperinsight.exception.ThrowUtils;
import com.zhemu.paperinsight.model.dto.chat.ChatSessionCreateRequest;
import com.zhemu.paperinsight.model.dto.chat.ChatSessionDeleteRequest;
import com.zhemu.paperinsight.model.entity.PaperChatSession;
import com.zhemu.paperinsight.model.vo.ChatSessionVO;
import com.zhemu.paperinsight.agent.core.ChatAgent;
import com.zhemu.paperinsight.service.PaperChatSessionService;
import com.zhemu.paperinsight.service.SysUserService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author lushihao
 */
@RestController
@RequestMapping("/assistant/session")
@RequiredArgsConstructor
public class ChatSessionController {

    private final PaperChatSessionService paperChatSessionService;
    private final SysUserService userService;
    private final ChatAgent chatAgent;

    /**
     * 创建对话
     * @param request 创建请求
     * @param httpServletRequest 网络请求
     * @return
     */
    @PostMapping("/create")
    @AuthCheck
    public BaseResponse<Map<String, Object>> create(@RequestBody @Validated ChatSessionCreateRequest request,
            HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(request == null || request.getPaperId() == null, ErrorCode.PARAMS_ERROR);
        long userId = userService.getLoginUser(httpServletRequest).getId();
        PaperChatSession session = paperChatSessionService.createSession(request.getPaperId(), userId);
        return ResultUtils.success(Map.of(
                "chatId", session.getId(),
                "paperId", session.getPaperId(),
                "title", session.getTitle() == null ? "" : session.getTitle()));
    }

    @GetMapping("/list")
    @AuthCheck
    public BaseResponse<List<ChatSessionVO>> list(@RequestParam long paperId, HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(paperId <= 0, ErrorCode.PARAMS_ERROR);
        long userId = userService.getLoginUser(httpServletRequest).getId();
        List<ChatSessionVO> list = paperChatSessionService.listSessions(paperId, userId);
        return ResultUtils.success(list);
    }

    @PostMapping("/delete")
    @AuthCheck
    public BaseResponse<Boolean> delete(@RequestBody @Validated ChatSessionDeleteRequest request,
            HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        long userId = userService.getLoginUser(httpServletRequest).getId();
        String chatId = request.getChatId();
        // 从 session 表中删除对话信息
        boolean ok = paperChatSessionService.deleteSession(chatId, userId);
        // 从数据库中删除该 session 对应的所有对话记录
        if (ok) {
            chatAgent.deleteSessionState(chatId);
        }
        return ResultUtils.success(ok);
    }
}
