begin;
INSERT INTO payment.payment_providers (id, name, description)
VALUES (2, 'fake-provider-2', 'this is fake-2');

INSERT INTO payment.payment_methods (id, provider_id, type, name, provider_unique_id, provider_method_type, is_active)
VALUES (10, 2, 'PAYOUT', 'payout', 'provider 228', 'CASH', true),
       (11, 2, 'TRANSACTION', 'transaction', 'provider 229', 'CASH', true);

INSERT INTO payment.payment_method_definitions (payment_method_id, currency_code, country_alpha3_code,
                                                is_all_currencies, is_all_countries)
VALUES (10, 'RUB', 'RUS', true, true),
       (11, 'USD', 'RUS', true, true);

INSERT INTO payment.payment_method_required_fields (payment_method_id, payment_type, name, data_type)
VALUES (10, 'some payment type 1', '228 name', 'data type 1');

commit;