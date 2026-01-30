package com.zhemu.paperinsight.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhemu.paperinsight.agent.core.ChatAgent;
import com.zhemu.paperinsight.model.entity.PaperChatSession;
import com.zhemu.paperinsight.model.entity.SysUser;
import com.zhemu.paperinsight.service.PaperChatSessionService;
import com.zhemu.paperinsight.service.SysUserService;
import io.agentscope.core.agent.Event;
import io.agentscope.core.agent.EventType;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

class ChatControllerTest {

    @Test
    void chatStream_emitsJsonEventsAndCompletes() {
        ChatAgent chatAgent = Mockito.mock(ChatAgent.class);
        SysUserService sysUserService = Mockito.mock(SysUserService.class);
        PaperChatSessionService paperChatSessionService = Mockito.mock(PaperChatSessionService.class);
        ObjectMapper objectMapper = new ObjectMapper();

        ChatController controller = new ChatController(chatAgent, objectMapper, sysUserService, paperChatSessionService);

        SysUser user = new SysUser();
        user.setId(1L);
        Mockito.when(sysUserService.getLoginUser(any())).thenReturn(user);

        Mockito.when(paperChatSessionService.getOwnedSession(anyString(), Mockito.eq(1L)))
                .thenReturn(PaperChatSession.builder().id("chat_test").paperId(1L).userId(1L).build());

        Msg delta = Msg.builder()
                .role(MsgRole.ASSISTANT)
                .content(TextBlock.builder().text("hello").build())
                .build();

        Event e1 = Mockito.mock(Event.class);
        Mockito.when(e1.getType()).thenReturn(EventType.REASONING);
        Mockito.when(e1.isLast()).thenReturn(false);
        Mockito.when(e1.getMessage()).thenReturn(delta);

        Mockito.when(chatAgent.stream(any(Msg.class), anyString(), anyString())).thenReturn(Flux.just(e1));

        List<String> events = controller.chatStream("chat_test", "hi", "hi", null)
                .map(sse -> sse.data())
                .collectList()
                .block();

        assertThat(events).isNotNull();
        String joined = String.join("\n", events);
        assertThat(joined).contains("\"type\":\"TEXT\"");
        assertThat(joined).contains("\"type\":\"COMPLETE\"");
    }
}
