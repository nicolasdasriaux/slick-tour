/* Execute as `ecommerceapi` */

\connect "ecommerce";

SET SEARCH_PATH = "ecommerce";

INSERT INTO "customers" (first_name, last_name) VALUES ('Victor', 'Hugo');
INSERT INTO "customers" (first_name, last_name) VALUES ('Honoré', 'de Balzac');
INSERT INTO "customers" (first_name, last_name) VALUES ('René', 'Char');
INSERT INTO "customers" (first_name, last_name) VALUES ('Antoine', 'de Saint-Exupéry');
