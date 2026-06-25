CREATE TABLE user_roles (
                            user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                            role    VARCHAR(50) NOT NULL,

                            CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role),
                            CONSTRAINT chk_user_roles_role CHECK (role IN ('USER', 'ADMIN'))
);

INSERT INTO user_roles (user_id, role)
SELECT id, 'USER'
FROM users;