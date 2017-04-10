CREATE TABLE lasttransactionnotifications (
  user_id BIGINT REFERENCES users (id) ON DELETE CASCADE ON UPDATE CASCADE,
  lastnotifiedtransaction BIGINT REFERENCES transactions (id) ON DELETE CASCADE ON UPDATE CASCADE
);
