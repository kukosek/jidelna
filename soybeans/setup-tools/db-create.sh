#!/bin/bash
dir=$(cd -P -- "$(dirname -- "$0")" && pwd -P)
sudo su - postgres -c "psql -f $dir/create_db.sql"
