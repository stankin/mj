#!/bin/bash

# Create mj_test user with password mj_test
psql -U postgres -c "CREATE USER mj_test WITH PASSWORD 'mj_test' CREATEDB;"
# Create mj_test database owned by mj_test user
psql -U postgres -c "CREATE DATABASE mj_test WITH OWNER=mj_test ENCODING='UTF8' LC_COLLATE='en_US.utf8' LC_CTYPE='en_US.utf8';"
