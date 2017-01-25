create TABLE passwordrecovery
(
  id     BIGSERIAL PRIMARY KEY,
  order_time   TIMESTAMP NOT NULL,
  user_id BIGINT NOT NULL REFERENCES users (id),
  token VARCHAR(255)
)