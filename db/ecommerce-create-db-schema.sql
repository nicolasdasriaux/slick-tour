/* Execute as 'super user' */

CREATE DATABASE "ecommerce";

\connect "ecommerce";

CREATE USER "ecommerceadmin";
CREATE SCHEMA "ecommerce" AUTHORIZATION "ecommerceadmin";

CREATE USER "ecommerceapi";
GRANT CONNECT ON DATABASE "ecommerce" TO "ecommerceapi";
GRANT USAGE ON SCHEMA "ecommerce" TO "ecommerceapi";
