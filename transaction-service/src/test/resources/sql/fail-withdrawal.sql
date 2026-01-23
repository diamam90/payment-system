INSERT INTO transaction.wallets (uid, name, wallet_type_uid, user_uid, status, balance, archived_at)
VALUES ('fa65903f-2441-4ada-81fa-d8cd6a2e00a1',
        'custom_wallet',
        'fa65903f-2441-4ada-81fa-d8cd6a2e00af',
        '00000000-0000-0000-0000-000000000001',
        'active',
        50.05,
        '2030-01-01 00:00:00');

INSERT INTO transaction.transactions (uid, user_uid, wallet_uid, amount, fee, type, status, created_at)
VALUES ('00000000-0000-0000-0000-000000000007',
        '00000000-0000-0000-0000-000000000001',
        'fa65903f-2441-4ada-81fa-d8cd6a2e00a1',
        30.0,
        0.16,
        'WITHDRAWAL',
        'PENDING',
        '2030-01-01 00:00:00');