DROP INDEX IF EXISTS uq_users_phone_number;

CREATE UNIQUE INDEX uq_users_name_enabled
    ON users (name)
    WHERE is_enabled = TRUE;

CREATE UNIQUE INDEX uq_users_phone_number
    ON users (phone_number)
    WHERE phone_number IS NOT NULL
      AND is_enabled = TRUE;
