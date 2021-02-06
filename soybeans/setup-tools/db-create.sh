#!/bin/bash
psql -U postgres -c "CREATE DATABASE jidelna;"
psql -U postgres -c "CREATE USER jidelna WITH ENCRYPTED PASSWORD 'jidelna';"
psql -U postgres -c "GRANT ALL PRIVILEGES ON DATABASE jidelna TO jidelna;"
