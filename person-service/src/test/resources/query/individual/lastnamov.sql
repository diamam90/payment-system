WITH addr_key AS (
    INSERT INTO person.addresses (country_id, address, zip_code, city, archived) VALUES
        (8,'test address. 63','400400','VOLGOGRAD', '2024-05-05 18:00:00') RETURNING address_id
), user_key AS (
    INSERT INTO person.users (secret_key, email, last_name, address_id)
        SELECT 'asdv232', 'email@email.email', 'lastnamov', addr_key.address_id
        FROM addr_key RETURNING user_id
)
INSERT INTO person.individuals (user_id, status, passport_number, phone_number, verified_at, archived_at)
    SELECT user_key.user_id,'active', '1331 4429', '8800', '2025-05-05 18:00:00', '2025-06-06 10:00:00'
    FROM user_key;