/* Execute as `ecommerceapi` */

\connect "ecommerce";

SET SEARCH_PATH = "ecommerce";

INSERT INTO "customers" ("first_name", "last_name")
VALUES ('Antoine', 'Camus'),
       ('Alain', 'Gide'),
       ('Arthur', 'Char'),
       ('Marie', 'Sand');

INSERT INTO "items" ("name")
VALUES ('Pen'),
       ('Desk'),
       ('Ink'),
       ('Paper'),
       ('Fork'),
       ('Spoon'),
       ('Knife'),
       ('Plate');

INSERT INTO "orders" ("customer_id")
VALUES (1),

       (2),
       (2),

       (3),
       (3),
       (3);

INSERT INTO "order_lines" ("order_id", "item_id", "quantity")
VALUES (1, 1, 1),

       (2, 2, 1),
       (3, 2, 1),
       (3, 3, 2),

       (4, 3, 1),
       (5, 3, 1),
       (5, 4, 2),
       (6, 3, 1),
       (6, 4, 2),
       (6, 5, 3);
