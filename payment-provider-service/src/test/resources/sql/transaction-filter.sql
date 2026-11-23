INSERT INTO payment.transactions(id, merchant_id, amount, currency, method, status, created_at, external_id)
VALUES (4, 2, 20.54, 'USD', 'CARD', 'SUCCESS', '2025-01-01 00:00:00', '224'),
       (5, 2, 20.53, 'RUB', 'CARD', 'SUCCESS', '2025-05-05 02:59:22', '225'),
       (6, 2, 20.55, 'EUR', 'CARD', 'FAILED', '2026-03-03 23:59:59', '226'),
       (7, 1, 20.55, 'EUR', 'CARD', 'SUCCESS', '2026-03-03 23:59:59', '227'),
       (8, 2, 20.56, 'EUR', 'CARD', 'PENDING', '2026-12-31 23:59:59', '228'),
       (9, 2, 20.57, 'USD', 'CARD', 'SUCCESS', '2027-01-01 00:00:00', '229'),

       (20, 2, 10, 'RUB', 'CARD', 'PENDING', '2027-03-02 23:59:59', '240'),
       (10, 2, 10, 'RUB', 'CARD', 'PENDING', '2027-03-03 00:00:00', '230'),
       (11, 2, 10, 'RUB', 'CARD', 'PENDING', '2027-03-03 01:00:00', '231'),
       (12, 2, 10, 'RUB', 'CARD', 'PENDING', '2027-03-03 12:00:00', '232'),
       (13, 2, 10, 'RUB', 'CARD', 'PENDING', '2027-03-03 23:59:59', '233'),
       (14, 2, 10, 'RUB', 'CARD', 'PENDING', '2027-03-04 00:00:00', '234');