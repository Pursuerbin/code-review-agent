package com.agent.codereview.service;

import com.agent.codereview.ai.AliyunEmbeddingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.List;

@Service
public class KnowledgeVectorService {

    @Autowired
    @Qualifier("vectorJdbcTemplate")  // 使用 PostgreSQL 的 JdbcTemplate
    private JdbcTemplate vectorJdbcTemplate;

    @Autowired
    private AliyunEmbeddingService embeddingService;

    // 启动时自动建表（如果不存在）
    @PostConstruct
    public void initTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS knowledge_docs (
                id SERIAL PRIMARY KEY,
                title TEXT NOT NULL,
                content TEXT NOT NULL,
                embedding vector(1536)
            )
            """;
        vectorJdbcTemplate.execute(sql);

        // 可选：创建索引加速检索（IVFFlat 或 HNSW）
        // vectorJdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_embedding ON knowledge_docs USING ivfflat (embedding vector_cosine_ops)");
    }

    /**
     * 添加文档（自动向量化并存储）
     */
    public void addDocument(String title, String content) {
        List<Double> embedding = embeddingService.embed(content);
        // 将 List<Double> 转换为 PostgreSQL 的 vector 字符串格式，例如 '[0.1,0.2,...]'
        String embeddingStr = embedding.toString();  // [0.1, 0.2, ...]
        String sql = "INSERT INTO knowledge_docs (title, content, embedding) VALUES (?, ?, ?::vector)";
        vectorJdbcTemplate.update(sql, title, content, embeddingStr);
    }

    /**
     * 根据查询文本检索最相似的 topK 条文档内容
     */
    public List<String> searchSimilar(String query, int topK) {
        List<Double> queryEmbedding = embeddingService.embed(query);
        String queryVecStr = queryEmbedding.toString();
        String sql = """
            SELECT content
            FROM knowledge_docs
            ORDER BY embedding <=> ?::vector
            LIMIT ?
            """;
        return vectorJdbcTemplate.queryForList(sql, String.class, queryVecStr, topK);
    }
}