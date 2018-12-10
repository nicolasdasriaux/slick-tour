/* Execute as `ecommerceapi` */

\connect "ecommerce";

SET SEARCH_PATH = "ecommerce";

INSERT INTO "customers" ("first_name", "last_name") VALUES ('Antoine', 'Camus');
INSERT INTO "customers" ("first_name", "last_name") VALUES ('Alain', 'Gide');
INSERT INTO "customers" ("first_name", "last_name") VALUES ('Arthur', 'Char');
INSERT INTO "customers" ("first_name", "last_name") VALUES ('Marie', 'Sand');

INSERT INTO "items" ("name") VALUES ('Pen');
INSERT INTO "items" ("name") VALUES ('Desk');
INSERT INTO "items" ("name") VALUES ('Ink');
INSERT INTO "items" ("name") VALUES ('Paper');
