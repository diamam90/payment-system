INSERT INTO payment.transactions(id, merchant_id, amount, currency, method, status, created_at)
VALUES (4, 2, 20.54, 'USD', 'CARD', 'SUCCESS', '2025-01-01 00:00:00'),
       (5, 2, 20.53, 'RUB', 'CARD', 'SUCCESS', '2025-05-05 02:59:22'),
       (6, 2, 20.55, 'EUR', 'CARD', 'FAILED', '2026-03-03 23:59:59'),
       (7, 1, 20.55, 'EUR', 'CARD', 'SUCCESS', '2026-03-03 23:59:59'),
       (8, 2, 20.56, 'EUR', 'CARD', 'PENDING', '2026-12-31 23:59:59'),
       (9, 2, 20.57, 'USD', 'CARD', 'SUCCESS', '2027-01-01 00:00:00');