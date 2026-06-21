BEGIN TRANSACTION;

INSERT INTO currency.providers (provider_code, provider_name, priority, active, description)
VALUES ('asc', 'test provider', 1, true, 'test provider'),
       ('ddd', 'test provider 2', 2, true, 'test provider 2');
INSERT INTO currency.currencies (code, iso_code, description, active)
VALUES ('ABC', 228, 'валюта 1', true),
       ('BCD', 229, 'валюта 2', true);

INSERT INTO currency.currency_rates (source_code, destination_code, rate_begin, rate_end, rate, provider_code)
VALUES ('ABC', 'BCD', '2026-05-05T12:00:00+00:00', '2026-05-05T12:15:00+00:00', 0.0001, 'asc'),
       ('ABC', 'BCD', '2026-05-05T12:00:00+00:00', '2026-05-05T12:15:00+00:00', 0.0001, 'ddd'),
       ('ABC', 'BCD', '2026-05-05T11:45:00+00:00', '2026-05-05T12:00:00+00:00', 0.0002, 'asc'),
       ('ABC', 'BCD', '2026-05-05T12:15:00+00:00', '2026-05-05T12:30:00+00:00', 0.0003, 'asc');

COMMIT TRANSACTION;