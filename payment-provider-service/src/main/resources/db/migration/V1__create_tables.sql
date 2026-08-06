CREATE TABLE payment.merchants (
    id          SERIAL PRIMARY KEY,
    merchant_id VARCHAR(50) NOT NULL UNIQUE,
    secret_key  VARCHAR(255) NOT NULL,
    name        VARCHAR(100),
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE payment.transactions (
                              id           BIGSERIAL PRIMARY KEY,
                              merchant_id  INTEGER NOT NULL REFERENCES payment.merchants(id),
                              amount       NUMERIC(18,2) NOT NULL,
                              currency     VARCHAR(3) NOT NULL,
                              method       VARCHAR(50) NOT NULL,
                              status       VARCHAR(20) NOT NULL,
                              created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                              updated_at   TIMESTAMP,
                              description  VARCHAR(255),
                              external_id  VARCHAR(100),
                              notification_url  VARCHAR(2048)
);
CREATE INDEX idx_transactions_merchant_date ON payment.transactions(merchant_id, created_at);

CREATE TABLE payment.payouts (
                         id           BIGSERIAL PRIMARY KEY,
                         merchant_id  INTEGER NOT NULL REFERENCES payment.merchants(id),
                         amount       NUMERIC(18,2) NOT NULL,
                         currency     VARCHAR(3) NOT NULL,
                         status       VARCHAR(20) NOT NULL,
                         created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                         updated_at   TIMESTAMP,
                         external_id  VARCHAR(100),
                         notification_url  VARCHAR(2048)
);
CREATE INDEX idx_payouts_merchant_date ON payment.payouts(merchant_id, created_at);

CREATE TABLE payment.webhooks (
                          id          BIGSERIAL PRIMARY KEY,
                          event_type  VARCHAR(50) NOT NULL,
                          entity_id   BIGINT NOT NULL,
                          payload     JSONB,
                          received_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                          notification_url  VARCHAR(2048)
);
CREATE INDEX idx_webhooks_entity ON payment.webhooks(event_type, entity_id);