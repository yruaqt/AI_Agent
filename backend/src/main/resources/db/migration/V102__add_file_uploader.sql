-- 已执行的 Flyway 迁移不可修改；文件上传人字段通过新版本增量补齐。
ALTER TABLE file_record
    ADD COLUMN uploader_id BIGINT NOT NULL DEFAULT 0;
