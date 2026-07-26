-- V100 已用于 PostgreSQL 专用 pgvector 迁移，因此公共迁移从 V101 继续编号。

-- 成员 D 的实训记录表在 V2 创建时 app_user 尚不存在，在此补齐外键。
ALTER TABLE training_record
    ADD CONSTRAINT fk_training_record_student
        FOREIGN KEY (student_id) REFERENCES app_user(id);

ALTER TABLE training_record
    ADD CONSTRAINT fk_training_record_orchard
        FOREIGN KEY (orchard_id) REFERENCES orchard(id);

-- 核心数值和枚举约束，阻止绕过接口后写入明显不合法的数据。
ALTER TABLE orchard
    ADD CONSTRAINT ck_orchard_area_positive CHECK (area_mu > 0);
ALTER TABLE orchard
    ADD CONSTRAINT ck_orchard_tree_count_positive CHECK (tree_count > 0);
ALTER TABLE orchard
    ADD CONSTRAINT ck_orchard_longitude CHECK (longitude IS NULL OR longitude BETWEEN -180 AND 180);
ALTER TABLE orchard
    ADD CONSTRAINT ck_orchard_latitude CHECK (latitude IS NULL OR latitude BETWEEN -90 AND 90);
ALTER TABLE orchard
    ADD CONSTRAINT ck_orchard_status CHECK (status IN ('ENABLED', 'DISABLED'));

ALTER TABLE app_user
    ADD CONSTRAINT ck_app_user_role CHECK (role IN ('ADMIN', 'STUDENT'));
ALTER TABLE app_user
    ADD CONSTRAINT ck_app_user_status CHECK (status IN ('ENABLED', 'DISABLED'));

ALTER TABLE training_record
    ADD CONSTRAINT ck_training_inspected_count CHECK (inspected_tree_count IS NULL OR inspected_tree_count >= 0);
ALTER TABLE training_record
    ADD CONSTRAINT ck_training_abnormal_count CHECK (abnormal_tree_count IS NULL OR abnormal_tree_count >= 0);
ALTER TABLE training_record
    ADD CONSTRAINT ck_training_count_relation CHECK (
        inspected_tree_count IS NULL OR abnormal_tree_count IS NULL
        OR abnormal_tree_count <= inspected_tree_count
    );
ALTER TABLE training_record
    ADD CONSTRAINT ck_training_score CHECK (score IS NULL OR score BETWEEN 0 AND 100);
ALTER TABLE training_record
    ADD CONSTRAINT ck_training_review_status CHECK (
        review_status IS NULL OR review_status IN ('PENDING', 'APPROVED', 'REJECTED')
    );

ALTER TABLE farming_task
    ADD CONSTRAINT ck_farming_task_priority CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH'));
ALTER TABLE farming_task
    ADD CONSTRAINT ck_farming_task_status CHECK (
        status IN ('DRAFT', 'CONFIRMED', 'TODO', 'DOING', 'DONE', 'CANCELLED')
    );

ALTER TABLE knowledge_document
    ADD CONSTRAINT ck_knowledge_chunk_count CHECK (chunk_count >= 0);

ALTER TABLE knowledge_chunk
    ADD CONSTRAINT ck_knowledge_page_number CHECK (page_number IS NULL OR page_number > 0);
ALTER TABLE knowledge_chunk
    ADD CONSTRAINT ck_knowledge_embedding_dimension CHECK (
        embedding_dimension IS NULL OR embedding_dimension > 0
    );

ALTER TABLE model_call_log
    ADD CONSTRAINT ck_model_call_duration CHECK (duration_ms >= 0);

ALTER TABLE tool_call_log
    ADD CONSTRAINT ck_tool_call_duration CHECK (duration_ms >= 0);

-- 高频管理页面过滤条件使用的组合索引。
CREATE INDEX idx_app_user_role_status ON app_user (role, status);
CREATE INDEX idx_training_review_status ON training_record (review_status, record_date);
CREATE INDEX idx_farming_task_assignee_status ON farming_task (assignee_id, status);
CREATE INDEX idx_knowledge_document_metadata
    ON knowledge_document (phenology, region, document_type, deleted);
