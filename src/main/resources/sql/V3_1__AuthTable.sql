CREATE TABLE Authentication (
  auth_id BIGSERIAL PRIMARY KEY,
  user_id BIGSERIAL REFERENCES users (id) ON DELETE CASCADE,
  method VARCHAR(255) NOT NULL,
  creation_date timestamp with time zone default (now() at time zone 'MSK'),
  expiration_date timestamp,
  value JSONB
);