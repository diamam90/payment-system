CREATE TABLE transaction.wallet_types
(
    uid           UUID PRIMARY KEY     DEFAULT uuid_generate_v4(),
    name          VARCHAR(32) NOT NULL,
    currency_code VARCHAR(3)  NOT NULL,
    status        VARCHAR(24) NOT NULL,
    user_type     VARCHAR(15),
    archived_at   TIMESTAMP,
    creator       VARCHAR(255),
    modifier      VARCHAR(255),
    created_at    TIMESTAMP   NOT NULL DEFAULT now(),
    updated_at    TIMESTAMP
);

CREATE TABLE transaction.wallets
(
    uid             UUID PRIMARY KEY        DEFAULT uuid_generate_v4(),
    name            VARCHAR(32)    NOT NULL,
    wallet_type_uid UUID           NOT NULL REFERENCES transaction.wallet_types (uid),
    user_uid        UUID           NOT NULL,
    status          VARCHAR(30)    NOT NULL,
    balance         DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    archived_at     TIMESTAMP,
    created_at      TIMESTAMP      NOT NULL DEFAULT now(),
    modified_at     TIMESTAMP
);

CREATE TABLE transaction.transactions
(
    uid               UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_uid          UUID           NOT NULL,
    wallet_uid        UUID           NOT NULL REFERENCES transaction.wallets (uid),
    amount            DECIMAL(10, 2) NOT NULL,
    type              VARCHAR        NOT NULL,
    status            VARCHAR(32)    NOT NULL,
    comment           VARCHAR(255),
    fee               DECIMAL(10, 2),
    target_wallet_uid UUID,
    payment_method_id BIGINT,
    failure_reason    VARCHAR(256),
    created_at        TIMESTAMP      NOT NULL,
    updated_at        TIMESTAMP
);