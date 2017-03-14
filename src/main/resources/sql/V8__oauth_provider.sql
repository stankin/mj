CREATE TABLE OAuthConsumer (
  consumer_id         BIGSERIAL PRIMARY KEY,
  email               VARCHAR(255)        NOT NULL,
  service_name        VARCHAR(255) UNIQUE NOT NULL,
  client_id           TEXT UNIQUE,
  secret              TEXT,
  token_creation_date TIMESTAMP WITH TIME ZONE DEFAULT (now() AT TIME ZONE 'MSK')
);

CREATE TABLE OAuthConsumerPermissions (
  consumer_id     BIGSERIAL REFERENCES OAuthConsumer (consumer_id) ON DELETE CASCADE,
  user_id         BIGSERIAL REFERENCES users (id) ON DELETE CASCADE,
  token           TEXT,
  creation_date   TIMESTAMP WITH TIME ZONE DEFAULT (now() AT TIME ZONE 'MSK'),
  expiration_date TIMESTAMP,
  permission      JSONB,
  PRIMARY KEY (consumer_id, user_id)
);