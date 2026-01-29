package com.zhemu.paperinsight.agent.config;

import com.zhemu.paperinsight.agent.common.ElasticsearchStore;
import com.zhemu.paperinsight.agent.core.ChatAgent;
import io.agentscope.core.embedding.EmbeddingModel;
import io.agentscope.core.model.Model;
import io.agentscope.core.rag.Knowledge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * ChatAgent 配置类
 * 仿照 SupervisorAgentConfig 风格配置
 * 
 * @author lushihao
 */
@Configuration
public class ChatAgentConfig {

    @Bean
    public ChatAgent chatAgent(@org.springframework.beans.factory.annotation.Qualifier("thinkingModel") Model model,
            AgentPromptConfig promptConfig,
            Knowledge knowledge,
            org.springframework.context.ApplicationContext applicationContext,
            DataSource dataSource) {
        return new ChatAgent(model, promptConfig, knowledge, applicationContext, dataSource);
    }
}
