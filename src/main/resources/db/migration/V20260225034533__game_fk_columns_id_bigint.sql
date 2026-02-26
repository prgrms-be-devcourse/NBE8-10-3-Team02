-- game fk columns id bigint
-- created_at_utc: 20260225034533
-- game(id)가 BIGINT로 변경됨에 따라 FK로 참조하는 모든 game_id 컬럼을 BIGINT로 변경

-- ─────────────────────────────────────────────────────────
-- 1. FK 제약조건 삭제 (타입 변경 전 필수)
-- ─────────────────────────────────────────────────────────

-- V1 조인 테이블
ALTER TABLE game_developer          DROP CONSTRAINT fk_game_developer_game;
ALTER TABLE game_publisher          DROP CONSTRAINT fk_game_publisher_game;
ALTER TABLE game_genre              DROP CONSTRAINT fk_game_genre_game;
ALTER TABLE game_platform           DROP CONSTRAINT fk_game_platform_game;

-- V2 조인 테이블
ALTER TABLE game_theme              DROP CONSTRAINT fk_game_theme_game;
ALTER TABLE game_keyword            DROP CONSTRAINT fk_game_keyword_game;
ALTER TABLE game_game_mode          DROP CONSTRAINT fk_game_game_mode_game;
ALTER TABLE game_player_perspective DROP CONSTRAINT fk_game_player_perspective_game;
ALTER TABLE game_company            DROP CONSTRAINT fk_game_company_game;
ALTER TABLE game_external_id        DROP CONSTRAINT fk_game_external_id_game;

-- V1 연관 테이블
ALTER TABLE review                  DROP CONSTRAINT fk_review_game;
ALTER TABLE member_game             DROP CONSTRAINT fk_member_game_game;
ALTER TABLE game_like               DROP CONSTRAINT fk_game_like_game;

-- ─────────────────────────────────────────────────────────
-- 2. game_id INT → BIGINT
-- ─────────────────────────────────────────────────────────

-- V1 조인 테이블
ALTER TABLE game_developer          ALTER COLUMN game_id TYPE bigint;
ALTER TABLE game_publisher          ALTER COLUMN game_id TYPE bigint;
ALTER TABLE game_genre              ALTER COLUMN game_id TYPE bigint;
ALTER TABLE game_platform           ALTER COLUMN game_id TYPE bigint;

-- V2 조인 테이블
ALTER TABLE game_theme              ALTER COLUMN game_id TYPE bigint;
ALTER TABLE game_keyword            ALTER COLUMN game_id TYPE bigint;
ALTER TABLE game_game_mode          ALTER COLUMN game_id TYPE bigint;
ALTER TABLE game_player_perspective ALTER COLUMN game_id TYPE bigint;
ALTER TABLE game_company            ALTER COLUMN game_id TYPE bigint;
ALTER TABLE game_external_id        ALTER COLUMN game_id TYPE bigint;

-- V1 연관 테이블
ALTER TABLE review                  ALTER COLUMN game_id TYPE bigint;
ALTER TABLE member_game             ALTER COLUMN game_id TYPE bigint;
ALTER TABLE game_like               ALTER COLUMN game_id TYPE bigint;

-- V6 스테이징 테이블 (PK 재생성 포함)
ALTER TABLE game_vector_staging     DROP CONSTRAINT game_vector_staging_pkey;
ALTER TABLE game_vector_staging     ALTER COLUMN game_id TYPE bigint;
ALTER TABLE game_vector_staging     ADD CONSTRAINT game_vector_staging_pkey PRIMARY KEY (game_id);

-- ─────────────────────────────────────────────────────────
-- 3. FK 제약조건 재생성
-- ─────────────────────────────────────────────────────────

-- V1 조인 테이블
ALTER TABLE game_developer
    ADD CONSTRAINT fk_game_developer_game
    FOREIGN KEY (game_id) REFERENCES game(id) ON DELETE CASCADE;

ALTER TABLE game_publisher
    ADD CONSTRAINT fk_game_publisher_game
    FOREIGN KEY (game_id) REFERENCES game(id) ON DELETE CASCADE;

ALTER TABLE game_genre
    ADD CONSTRAINT fk_game_genre_game
    FOREIGN KEY (game_id) REFERENCES game(id) ON DELETE CASCADE;

ALTER TABLE game_platform
    ADD CONSTRAINT fk_game_platform_game
    FOREIGN KEY (game_id) REFERENCES game(id) ON DELETE CASCADE;

-- V2 조인 테이블
ALTER TABLE game_theme
    ADD CONSTRAINT fk_game_theme_game
    FOREIGN KEY (game_id) REFERENCES game(id) ON DELETE CASCADE;

ALTER TABLE game_keyword
    ADD CONSTRAINT fk_game_keyword_game
    FOREIGN KEY (game_id) REFERENCES game(id) ON DELETE CASCADE;

ALTER TABLE game_game_mode
    ADD CONSTRAINT fk_game_game_mode_game
    FOREIGN KEY (game_id) REFERENCES game(id) ON DELETE CASCADE;

ALTER TABLE game_player_perspective
    ADD CONSTRAINT fk_game_player_perspective_game
    FOREIGN KEY (game_id) REFERENCES game(id) ON DELETE CASCADE;

ALTER TABLE game_company
    ADD CONSTRAINT fk_game_company_game
    FOREIGN KEY (game_id) REFERENCES game(id) ON DELETE CASCADE;

ALTER TABLE game_external_id
    ADD CONSTRAINT fk_game_external_id_game
    FOREIGN KEY (game_id) REFERENCES game(id) ON DELETE CASCADE;

-- V1 연관 테이블 (ON DELETE 정책 원본 유지)
ALTER TABLE review
    ADD CONSTRAINT fk_review_game
    FOREIGN KEY (game_id) REFERENCES game(id) ON DELETE SET NULL;

ALTER TABLE member_game
    ADD CONSTRAINT fk_member_game_game
    FOREIGN KEY (game_id) REFERENCES game(id) ON DELETE CASCADE;

ALTER TABLE game_like
    ADD CONSTRAINT fk_game_like_game
    FOREIGN KEY (game_id) REFERENCES game(id) ON DELETE CASCADE;
