#!/usr/bin/env bash

psql postgres --file db/drop-db-schema.sql
psql postgres --file db/create-db-schema.sql

psql ecommerce ecommerceadmin --file db/alter-schema-000.sql

psql ecommerce ecommerceapi --file db/insert-sample-data.sql
