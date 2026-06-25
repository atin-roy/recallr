CREATE TABLE users (
                       id          UUID PRIMARY KEY,
                       username    VARCHAR(255) NOT NULL UNIQUE,
                       email       VARCHAR(255) NOT NULL UNIQUE,
                       created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
                       updated_at  TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE user_providers (
                                id          UUID PRIMARY KEY,
                                user_id     UUID         NOT NULL REFERENCES users(id),
                                provider    VARCHAR(50)  NOT NULL,
                                provider_id VARCHAR(255) NOT NULL,
                                created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
                                updated_at  TIMESTAMP WITH TIME ZONE NOT NULL,

                                CONSTRAINT uk_provider_provider_id UNIQUE (provider, provider_id),
                                CONSTRAINT uk_user_provider        UNIQUE (user_id, provider)
);