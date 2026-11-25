CREATE UNIQUE INDEX IF NOT EXISTS
    external_payout_id_idx on payment.payouts(external_id);
CREATE UNIQUE INDEX IF NOT EXISTS
    external_transaction_id_idx on payment.transactions(external_id);
