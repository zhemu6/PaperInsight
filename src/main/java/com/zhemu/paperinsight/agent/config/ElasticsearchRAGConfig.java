package com.zhemu.paperinsight.agent.config;

import com.zhemu.paperinsight.agent.common.ElasticsearchStore;
import io.agentscope.core.embedding.EmbeddingModel;
import io.agentscope.core.embedding.dashscope.DashScopeTextEmbedding;
import io.agentscope.core.rag.Knowledge;
import io.agentscope.core.rag.knowledge.SimpleKnowledge;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Elasticsearch RAG 配置类
 * 
 * @author lushihao
 */
@Configuration
public class ElasticsearchRAGConfig {

    @Value("${spring.elasticsearch.uris:http://localhost:9200}")
    private String esUrl;

    // @Value("${spring.rabbitmq.username:elastic}")
    // private String esUsername; // 注意：复用配置或需新增配置，暂时假设 application.yml 有对应配置

    // @Value("${spring.rabbitmq.password:changeme}")
    // private String esPassword;

    @Value("${agentscope.dashscope.api-key}")
    private String dashscopeApiKey;

    private static final String ES_INDEX_NAME = "paper_insight_rag";
    private static final int EMBEDDING_DIMENSIONS = 1024; // text-embedding-v3 输出维度

    /**
     * ElasticsearchStore Bean
     * 
     * @return ElasticsearchStore
     */
    @Bean
    public ElasticsearchStore elasticsearchStore() {
        try {
            return ElasticsearchStore.builder()
                    .url(esUrl)
                    .indexName(ES_INDEX_NAME)
                    .dimensions(EMBEDDING_DIMENSIONS)
                    // .username(esUsername) // 如果需要认证
                    // .password(esPassword)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create ElasticsearchStore", e);
        }
    }

    /**
     * 嵌入模型
     * 
     * @return EmbeddingModel
     */
    @Bean
    public EmbeddingModel embeddingModel() {
        return DashScopeTextEmbedding.builder()
                .apiKey(dashscopeApiKey)
                .modelName("text-embedding-v3")
                .dimensions(EMBEDDING_DIMENSIONS)
                .build();
    }

    /**
     * 知识库
     * 
     * @param elasticsearchStore
     * @param embeddingModel
     * @return
     */
    @Bean
    public Knowledge knowledge(ElasticsearchStore elasticsearchStore, EmbeddingModel embeddingModel) {
        return SimpleKnowledge.builder()
                .embeddingStore(elasticsearchStore)
                .embeddingModel(embeddingModel)
                .build();
    }
}
