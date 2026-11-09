begin;
INSERT INTO payment.payment_providers (id, name, description)
VALUES (1, 'fake-provider', 'this is fake');

INSERT INTO payment.payment_methods (id, provider_id, type, name, provider_unique_id, provider_method_type, is_active)
VALUES (1, 1, 'PAYOUT', 'payout', 'provider 228', 'provider method type 1', true),
       (2, 1, 'TRANSACTION', 'transaction', 'provider 228', 'CASH', true),
       (3, 1, 'TRANSACTION', 'transaction', 'provider 228', 'CARD', false),
       (4, 1, 'TRANSACTION', 'transaction', 'provider 228', 'provider method type 4', true),
       (5, 1, 'TRANSACTION', 'transaction', 'provider 228', 'provider method type 5', true);

INSERT INTO payment.payment_method_definitions (payment_method_id, currency_code, country_alpha3_code,
                                                is_all_currencies, is_all_countries)
VALUES (1, 'RUB', 'RUS', true, true),
       (2, 'USD', 'KZH', true, false),
       (3, 'RUB', 'USA', false, false),
       (4, 'RUB', 'BBB', false, true),
       (5, 'EUR', 'AAA', false, false);

INSERT INTO payment.payment_method_required_fields (payment_method_id, payment_type, name, data_type)
VALUES (1, 'some payment type 1', '228 name', 'data type 1'),
       (1, 'some payment type 2', '229 name', 'data type 2');

commit;