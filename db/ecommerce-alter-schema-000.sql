/* Execute as `ecommerceadmin` */

\connect "ecommerce";

SET SEARCH_PATH = "ecommerce";

CREATE TABLE "customers" (
  "id"         BIGSERIAL NOT NULL PRIMARY KEY,
  "first_name" VARCHAR   NOT NULL,
  "last_name"  VARCHAR   NOT NULL
);

CREATE TABLE "items" (
  "id"   BIGSERIAL NOT NULL PRIMARY KEY,
  "name" VARCHAR   NOT NULL
);

GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA "ecommerce" TO "ecommerceapi";
GRANT USAGE ON ALL SEQUENCES IN SCHEMA "ecommerce" TO "ecommerceapi";
