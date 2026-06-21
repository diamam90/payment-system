CREATE TABLE IF NOT EXISTS currency.CURRENCIES (
    id bigserial PRIMARY KEY,
    code VARCHAR(3) NOT NULL UNIQUE,
    iso_code VARCHAR(3) NOT NULL,
    description VARCHAR,
    active BOOLEAN DEFAULT TRUE,
    symbol VARCHAR,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    modified_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS currency.PROVIDERS (
    provider_code VARCHAR(3)  PRIMARY KEY,
    provider_name VARCHAR(20) UNIQUE,
    priority smallint NOT NULL,
    active BOOLEAN DEFAULT TRUE,
    description VARCHAR,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    modified_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS currency.CURRENCY_RATES (
    id bigserial PRIMARY KEY,
    source_code VARCHAR(3) NOT NULL REFERENCES currency.CURRENCIES(code),
    destination_code VARCHAR(3) NOT NULL REFERENCES currency.CURRENCIES(code),
    rate_begin TIMESTAMP NOT NULL,
    rate_end TIMESTAMP NOT NULL,
    rate NUMERIC(10,5) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    modified_at TIMESTAMP,
    provider_code VARCHAR(3) NOT NULL REFERENCES currency.PROVIDERS(provider_code)
);

CREATE TABLE IF NOT EXISTS shedlock (
    name VARCHAR(64) PRIMARY KEY,
    lock_until TIMESTAMP NOT NULL,
    locked_at TIMESTAMP NOT NULL,
    locked_by VARCHAR(255)
);