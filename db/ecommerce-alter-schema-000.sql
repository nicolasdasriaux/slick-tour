/* Execute as `ecommerceadmin` */

\connect "ecommerce";

SET SEARCH_PATH = "ecommerce";

CREATE TABLE "customers"
(
  "id"         BIGSERIAL NOT NULL PRIMARY KEY,
  "first_name" VARCHAR   NOT NULL,
  "last_name"  VARCHAR   NOT NULL
);

CREATE TABLE "items"
(
  "id"   BIGSERIAL NOT NULL PRIMARY KEY,
  "name" VARCHAR   NOT NULL
);

CREATE TABLE "orders"
(
  "id"          BIGSERIAL NOT NULL PRIMARY KEY,
  "customer_id" BIGINT    NOT NULL,
  "date"        DATE      NOT NULL
);

CREATE TABLE "order_lines"
(
  "id"       BIGSERIAL NOT NULL PRIMARY KEY,
  "order_id" BIGINT    NOT NULL,
  "item_id"  BIGINT    NOT NULL,
  "quantity" INTEGER   NOT NULL
);

ALTER TABLE "orders"
  ADD CONSTRAINT "fk_orders_customer_id" FOREIGN KEY ("customer_id") REFERENCES "customers" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

ALTER TABLE "order_lines"
  ADD CONSTRAINT "fk_order_lines_item_id" FOREIGN KEY ("item_id") REFERENCES "items" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

ALTER TABLE "order_lines"
  ADD CONSTRAINT "fk_order_lines_order_id" FOREIGN KEY ("order_id") REFERENCES "orders" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA "ecommerce" TO "ecommerceapi";
GRANT USAGE ON ALL SEQUENCES IN SCHEMA "ecommerce" TO "ecommerceapi";
