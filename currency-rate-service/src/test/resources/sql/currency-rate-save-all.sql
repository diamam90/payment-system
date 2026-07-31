BEGIN TRANSACTION;
INSERT INTO currency.providers (provider_code, provider_name, priority, active, description, created_at, modified_at)
    VALUES ('asc', 'test provider', 1, true, 'test provider', now(), now());
INSERT INTO currency.currencies (code, iso_code, description, active)
VALUES ('ABC', 228, 'валюта 1', true),
       ('BCD', 229, 'валюта 2', true);
COMMIT TRANSACTION;