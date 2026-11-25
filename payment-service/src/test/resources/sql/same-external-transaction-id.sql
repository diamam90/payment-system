BEGIN;

INSERT INTO payment.payment_providers (id, name, description)
VALUES (3, 'fake-provider-3', 'this is fake-3');

INSERT INTO payment.payment_methods (id, provider_id, type, name, provider_unique_id, provider_method_type, is_active)
VALUES (3, 3, 'PAYOUT', 'payout', 'provider 228', 'CASH', true);

INSERT INTO payment.payments (payment_method_id, internal_transaction_id, amount, currency, external_transaction_id, merchant_id)
VALUES (3, '11111111-1111-1111-1111-111111111111', 50.31, 'RUB', 'external 228', 'merchant 1');
COMMIT;