begin;
INSERT INTO payment.payment_providers (id, name, description)
VALUES (1, 'fake-provider', 'this is fake');

INSERT INTO payment.payment_methods (id, provider_id, type, name, provider_unique_id, provider_method_type, is_active)
VALUES (1, 1, 'PAYOUT', 'payout', 'provider 228', 'provider method type 1', false);

commit;