#!/bin/bash

createuser -d mj_test
psql -c "ALTER USER mj_test WITH PASSWORD 'mj_test';"
createdb mj_test -E UTF8 -O mj_test