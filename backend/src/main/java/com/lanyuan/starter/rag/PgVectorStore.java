package com.lanyuan.starter.rag;

import com.lanyuan.starter.knowledge.KnowledgeChunk;
import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

/** PostgreSQL 环境使用 pgvector 存储和数据库侧余弦检索，H2 环境自动回退。 */
@Component
public class PgVectorStore {

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;
    private boolean postgres;

    public PgVectorStore(DataSource dataSource, JdbcTemplate jdbcTemplate) {
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    void detectDatabase() {
        try (Connection connection = dataSource.getConnection()) {
            postgres = connection.getMetaData().getDatabaseProductName().toLowerCase().contains("postgresql");
        } catch (Exception ex) {
            postgres = false;
        }
    }

    public boolean isAvailable() {
        return postgres;
    }

    public void sync(KnowledgeChunk chunk) {
        if (!postgres) return;
        jdbcTemplate.update(
                "UPDATE knowledge_chunk SET embedding_vector = CAST(? AS vector) WHERE id = ?",
                chunk.getEmbeddingData(), chunk.getId()
        );
    }

    public List<RagSearchResult> search(EmbeddingVector queryVector,
                                        RagSearchRequest request,
                                        String provider) {
        StringBuilder sql = new StringBuilder("""
                SELECT * FROM (
                  SELECT chunk.id AS chunk_id, chunk.document_id, chunk.chunk_index,
                         chunk.content, chunk.page_number, chunk.phenology, chunk.region,
                         chunk.document_type, document.title, document.source_organization,
                         1 - (chunk.embedding_vector <=> CAST(? AS vector)) AS score
                  FROM knowledge_chunk chunk
                  JOIN knowledge_document document ON document.id = chunk.document_id
                  WHERE document.deleted = FALSE
                    AND document.status = 'SUCCESS'
                    AND chunk.embedding_vector IS NOT NULL
                    AND chunk.embedding_provider = ?
                """);
        List<Object> args = new ArrayList<>();
        args.add(VectorCodec.encode(queryVector.values()));
        args.add(provider);
        RagSearchRequest.Filters filters = request.filters();
        if (filters != null && text(filters.phenology()) != null) {
            sql.append(" AND chunk.phenology = ?");
            args.add(filters.phenology().trim());
        }
        if (filters != null && text(filters.region()) != null) {
            sql.append(" AND chunk.region = ?");
            args.add(filters.region().trim());
        }
        if (filters != null && text(filters.documentType()) != null) {
            sql.append(" AND chunk.document_type = ?");
            args.add(filters.documentType().trim());
        }
        sql.append(") ranked WHERE ranked.score >= ? ORDER BY ranked.score DESC LIMIT ?");
        args.add(request.effectiveMinScore());
        args.add(request.effectiveMaxResults());

        return jdbcTemplate.query(sql.toString(), (resultSet, rowNum) -> new RagSearchResult(
                String.valueOf(resultSet.getLong("document_id")),
                resultSet.getString("title"),
                resultSet.getString("source_organization"),
                String.valueOf(resultSet.getLong("chunk_id")),
                resultSet.getInt("chunk_index"),
                (Integer) resultSet.getObject("page_number"),
                resultSet.getString("content"),
                resultSet.getDouble("score"),
                resultSet.getString("phenology"),
                resultSet.getString("region"),
                resultSet.getString("document_type")
        ), args.toArray());
    }

    private static String text(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
