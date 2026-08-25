ALTER TABLE app_user
    ADD COLUMN gender VARCHAR(16);

ALTER TABLE app_user
    ADD CONSTRAINT ck_app_user_gender
        CHECK (gender IS NULL OR gender IN ('MALE', 'FEMALE'));
