begin;
INSERT INTO payment.payment_providers (id, name, description)
VALUES (1, 'fake-provider', 'this is fake');

INSERT INTO payment.payment_methods (id, provider_id, type, name, provider_unique_id, provider_method_type, is_active, logo)
VALUES (1, 1, 'PAYOUT', 'payout', 'provider 228', 'provider method type 1', true, 'http://logo.url');

INSERT INTO payment.payment_method_definitions (payment_method_id, currency_code, country_alpha3_code,
                                                is_all_currencies, is_all_countries)
VALUES (1, 'JPY', 'RUS', true, true);

INSERT INTO payment.payment_method_required_fields (payment_method_id, payment_type, name, data_type)
VALUES (1, 'some payment type 1', '228 name', 'data type 1'),
       (1, 'some payment type 2', '229 name', 'data type 2');

commit;