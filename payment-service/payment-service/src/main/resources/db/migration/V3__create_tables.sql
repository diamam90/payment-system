CREATE TABLE payment.payment_providers (
                                   id              SERIAL PRIMARY KEY,
                                   name            VARCHAR(50) NOT NULL,
                                   description     VARCHAR(256)
);

CREATE TABLE payment.payment_methods (
                                 id                      SERIAL PRIMARY KEY,
                                 provider_id             INTEGER REFERENCES payment.payment_providers(id),
                                 type                    VARCHAR(32) NOT NULL,
                                 created_at              TIMESTAMP DEFAULT NOW(),
                                 modified_at             TIMESTAMP,
                                 name                    VARCHAR(64) NOT NULL,
                                 is_active               BOOLEAN DEFAULT TRUE,
                                 provider_unique_id      VARCHAR(128) NOT NULL,
                                 provider_method_type    VARCHAR(32) NOT NULL,
                                 logo                    TEXT,
                                 profile_type            VARCHAR(24) DEFAULT 'INDIVIDUAL' NOT NULL
);

CREATE TABLE payment.payment_method_definitions (
                                            id                    SERIAL PRIMARY KEY,
                                            payment_method_id     INTEGER REFERENCES payment.payment_methods(id),
                                            currency_code         VARCHAR(3),
                                            country_alpha3_code   VARCHAR(3),
                                            is_all_currencies     BOOLEAN DEFAULT FALSE,
                                            is_all_countries      BOOLEAN DEFAULT FALSE,
                                            is_priority           BOOLEAN DEFAULT FALSE,
                                            is_active             BOOLEAN DEFAULT TRUE
);

CREATE TABLE payment.payment_method_required_fields (
                                                uid                   UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                                created_at            TIMESTAMP DEFAULT NOW(),
                                                modified_at           TIMESTAMP DEFAULT NOW(),
                                                payment_method_id     INTEGER REFERENCES payment.payment_methods(id),
                                                payment_type          VARCHAR(64) NOT NULL,
                                                country_alpha3_code   VARCHAR(3),
                                                name                  VARCHAR(128) NOT NULL,
                                                data_type             VARCHAR(128) NOT NULL,
                                                validation_type       VARCHAR(128),
                                                validation_rule       VARCHAR(256),
                                                default_value         VARCHAR(128),
                                                values_options        TEXT,
                                                description           VARCHAR(255),
                                                placeholder           VARCHAR(255),
                                                representation_name   VARCHAR(255),
                                                language              VARCHAR(2),
                                                is_active             BOOLEAN DEFAULT TRUE,
                                                UNIQUE (language, name, payment_method_id, payment_type, country_alpha3_code)
);

CREATE TABLE payment.payments (
                          id                      SERIAL PRIMARY KEY,
                          payment_method_id       INTEGER REFERENCES payment.payment_methods(id),
                          external_transaction_id VARCHAR(128),
                          internal_transaction_id UUID,
                          amount                  NUMERIC(18, 2) NOT NULL,
                          currency                VARCHAR(3) NOT NULL,
                          status                  VARCHAR(20) NOT NULL DEFAULT 'PENDING',
                          created_at              TIMESTAMP DEFAULT NOW(),
                          modified_at             TIMESTAMP DEFAULT NOW()
);