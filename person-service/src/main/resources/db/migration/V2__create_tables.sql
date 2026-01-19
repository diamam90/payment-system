CREATE TABLE IF NOT EXISTS person.countries (
    country_id SERIAL PRIMARY KEY,
    name VARCHAR(64),
    alpha2 VARCHAR(2),
    alpha3 VARCHAR(3),
    status VARCHAR(32),
    created TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS person.addresses (
    address_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    country_id INTEGER REFERENCES person.countries(country_id),
    address VARCHAR(128),
    zip_code VARCHAR(32),
    archived TIMESTAMP NOT NULL,
    city VARCHAR(32),
    state VARCHAR(32),
    created TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS person.users (
    user_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    secret_key VARCHAR(32),
    email VARCHAR(1024),
    first_name VARCHAR(32),
    last_name VARCHAR(32),
    filled BOOLEAN,
    address_id UUID REFERENCES person.addresses(address_id),
    created TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS person.individuals (
    individual_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES person.users(user_id),
    passport_number VARCHAR(32),
    phone_number VARCHAR(32),
    verified_at TIMESTAMP NOT NULL,
    archived_at TIMESTAMP NOT NULL,
    status VARCHAR(32)
);