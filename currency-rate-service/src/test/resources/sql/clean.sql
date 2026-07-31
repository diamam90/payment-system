START TRANSACTION;
    TRUNCATE currency.currency_rates CASCADE;
    TRUNCATE currency.providers CASCADE;
    DELETE FROM currency.currencies WHERE id > (SELECT id FROM currency.currencies WHERE code = 'CNY');
COMMIT TRANSACTION;