CREATE TABLE users (
                       id          UUID PRIMARY KEY,
                       email       VARCHAR(255) NOT NULL UNIQUE,
                       created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
                       updated_at  TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE user_providers (
                                id            UUID PRIMARY KEY,
                                user_id       UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                provider      VARCHAR(50)  NOT NULL,
                                provider_id   VARCHAR(255) NOT NULL,
                                password_hash VARCHAR(255),
                                created_at    TIMESTAMP WITH TIME ZONE NOT NULL,
                                updated_at    TIMESTAMP WITH TIME ZONE NOT NULL,

                                CONSTRAINT uk_provider_provider_id UNIQUE (provider, provider_id),
                                CONSTRAINT uk_user_provider UNIQUE (user_id, provider)
);

CREATE TABLE user_roles (
                            user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                            role    VARCHAR(50) NOT NULL,

                            CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role),
                            CONSTRAINT chk_user_roles_role CHECK (role IN ('USER', 'ADMIN'))
);