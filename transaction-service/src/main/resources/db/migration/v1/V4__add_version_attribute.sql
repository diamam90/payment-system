ALTER TABLE transaction.transactions
    ADD COLUMN version INT NOT NULL DEFAULT 0;

UPDATE transaction.transactions
SET version = 0
WHERE version is null;


ALTER TABLE transaction.wallets
    ADD COLUMN version INT NOT NULL DEFAULT 0;

UPDATE transaction.wallets
SET version = 0
WHERE version is null;