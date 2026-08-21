CREATE TABLE user_roles
(
    user_id UUID        NOT NULL,
    role    VARCHAR(30) NOT NULL,
    CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT chk_user_roles_role CHECK (role IN ('USER', 'PRODUCT_MANAGER', 'ADMIN'))
);

INSERT INTO user_roles (user_id, role)
SELECT id, role
FROM users;

ALTER TABLE users
    DROP CONSTRAINT chk_users_role;

ALTER TABLE users
    DROP COLUMN role;
