CREATE TABLE refresh_tokens (
        id          UUID PRIMARY KEY,
        created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
        updated_at  TIMESTAMP WITH TIME ZONE NOT NULL,
        user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
        token_hash  VARCHAR(64) NOT NULL,
        expires_at  TIMESTAMP WITH TIME ZONE NOT NULL,
        is_revoked  BOOLEAN NOT NULL DEFAULT FALSE,

        CONSTRAINT uq_refresh_tokens_hash UNIQUE (token_hash)
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);