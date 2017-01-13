CREATE TABLE Authentication (
  user_id BIGSERIAL PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
  method VARCHAR(255) NOT NULL,
  creation_date timestamp with time zone default (now() at time zone 'MSK'),
  expiration_date timestamp,
  value JSON
);