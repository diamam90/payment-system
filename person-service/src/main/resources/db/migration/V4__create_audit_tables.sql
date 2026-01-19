CREATE TABLE IF NOT EXISTS REVINFO (
    REV BIGINT PRIMARY KEY,
    REVTSTMP BIGINT
);

CREATE TABLE IF NOT EXISTS person.addresses_AUD (
    address_id UUID NOT NULL,
    country_id INTEGER,
    address VARCHAR(128),
    zip_code VARCHAR(32),
    archived TIMESTAMP,
    city VARCHAR(32),
    state VARCHAR(32),
    created TIMESTAMP,
    updated TIMESTAMP,
    REV BIGINT REFERENCES REVINFO(REV),
    REVTYPE SMALLINT
);

CREATE TABLE IF NOT EXISTS person.users_AUD (
    user_id UUID NOT NULL,
    secret_key VARCHAR(32),
    email VARCHAR(1024),
    first_name VARCHAR(32),
    last_name VARCHAR(32),
    filled BOOLEAN,
    address_id UUID,
    REV BIGINT REFERENCES REVINFO(REV),
    REVTYPE SMALLINT,
    created TIMESTAMP,
    updated TIMESTAMP
);

CREATE TABLE IF NOT EXISTS person.individuals_AUD (
    individual_id UUID NOT NULL,
    user_id UUID,
    passport_number VARCHAR(32),
    phone_number VARCHAR(32),
    verified_at TIMESTAMP,
    archived_at TIMESTAMP,
    status VARCHAR(32),
    REV BIGINT REFERENCES REVINFO(REV),
    REVTYPE SMALLINT
);

CREATE SEQUENCE IF NOT EXISTS revinfo_seq INCREMENT 50;