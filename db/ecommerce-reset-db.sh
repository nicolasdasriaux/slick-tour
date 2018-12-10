#!/usr/bin/env bash

psql postgres --file db/ecommerce-drop-db-schema.sql
psql postgres --file db/ecommerce-create-db-schema.sql

psql ecommerce ecommerceadmin --file db/ecommerce-alter-schema-000.sql

psql ecommerce ecommerceapi --file db/ecommerce-insert-sample-data.sql
