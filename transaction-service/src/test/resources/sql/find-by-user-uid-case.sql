WITH inserted as (SELECT uid FROM transaction.wallet_types WHERE currency_code = 'RUB')
INSERT
INTO transaction.wallets (name, wallet_type_uid, user_uid, status, balance, archived_at)
VALUES
    ('my wallet 1', (SELECT uid FROM inserted), '00000000-0000-0000-0000-000000000000', 'active', 23.23, '2020-12-12 00:00:00'),
    ('my wallet 2', (SELECT uid FROM inserted), '00000000-0000-0000-0000-000000000000', 'active', 24.24, '2021-12-12 00:00:00'),
    ('my wallet 3', (SELECT uid FROM inserted), '00000000-0000-0000-0000-000000000000', 'active', 25.25, '2022-12-12 00:00:00'),
    ('my wallet 4', (SELECT uid FROM inserted), '00000000-0000-0000-0000-000000000000', 'active', 26.26, '2023-12-12 00:00:00'),
    ('my wallet 5', (SELECT uid FROM inserted), '00000000-0000-0000-0000-000000000000', 'active', 27.27, '2024-12-12 00:00:00'),
    ('my wallet 11', (SELECT uid FROM inserted), '00000000-0000-0000-0000-000000000001', 'active', 27, '2024-12-12 00:00:00'),
    ('my wallet 12', (SELECT uid FROM inserted), '00000000-0000-0000-0000-000000000001', 'active', 28, '2024-12-12 00:00:00'),
    ('my wallet 13', (SELECT uid FROM inserted), '00000000-0000-0000-0000-000000000002', 'active', 29, '2024-12-12 00:00:00');
