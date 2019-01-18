autoscale: true
footer: Relational Database Access with Slick
slidenumbers: true

# [fit] **Relational Database Access**
## with _Slick_

---

# _Slick_

> Reactive Functional Relational Mapping for Scala
-- http://slick.lightbend.com

* Library to access **relational databases**
* Can express **queries** in a **functional way** (`map`, `filter`...)
* Also supports **plain SQL**
* Supports asynchronicity (`Future`) and streaming (reactive streams)

---

# Database **Profile**

---

# Database Profile

* Many differences between databases and SQL dialects
* A **profile** allows
  - to unify peculiarities,
  - and also to benefit from proprietary features.

---

# Tailoring a _PostgreSQL_ Profile

```scala
// Extend PostgreSQL profile with java.time and Spray JSON support
trait ExtendedPostgresProfile extends ExPostgresProfile with PgDate2Support with PgSprayJsonSupport {
  val pgjson: String = "jsonb" // jsonb type for JSON column

  // Add 'insert or update' capability
  override protected def computeCapabilities: Set[Capability] =
    super.computeCapabilities + JdbcCapabilities.insertOrUpdate

  override val api: ExtendedAPI = ExtendedAPI
  // Add API support for Date and Time, and JSON
  trait ExtendedAPI extends API with DateTimeImplicits with JsonImplicits
  object ExtendedAPI extends ExtendedAPI
}

object ExtendedPostgresProfile extends ExtendedPostgresProfile
```

---

# Importing API of the _PostgreSQL_ Profile

```scala
import ExtendedPostgresProfile.api._
```

Import when in need to

* to connect to the **database**
* to describe **tables**,
* to describe **queries**,
* and more.

---

# [fit] Describing **Tables** and Mapping **Records**

---

# `Customers` Table and `Customer` Record

```scala
class Customers(tag: Tag) extends Table[Customer](tag, "customers") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def firstName = column[String]("first_name")
  def lastName = column[String]("last_name")
  def * = (id.?, firstName, lastName).mapTo[Customer]
  
  def fullName = firstName ++ " " ++ lastName // Calculated column
}

object Customers {
  val table = TableQuery[Customers]
}

case class Customer(id: Option[Long], firstName: String, lastName: String)
```

---

# **Table** Classes

**Table Class** describes a **table in a database**

* Name of **table** in database (`Table`)
* Name and type of **columns** in table (`column`)
* **Mapping** of table row to Record Class (`mapTo`, `<>`)
* Foreign keys (`foreignKey`)
* Indexes (`index`)
* Calculated columns

---

# **Record** Classes

**Record Class** describes a **row in a table**

* Practically a **case class**
* **Strictly reflects table columns**, even foreign key columns
  - Might be a selection of columns
  - Might be **substructured**
  - Might use **custom column types**
* **Not part of the domain model**
  - Additional mapping to and from the model

---

# When `mapTo` Does Not Compile

```scala
class Customers(tag: Tag) extends Table[Customer](tag, "customers") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def firstName = column[String]("first_name")
  def lastName = column[String]("last_name")
  def * = (id.?, firstName, lastName) <> ((Customer.apply _).tupled, Customer.unapply)
}
```

---

## Making Sense of `<>` Method

```scala
(id.?, firstName, lastName) <> ((Customer.apply _).tupled, Customer.unapply)
```

| Part                          | Type                                                 |
|-------------------------------|------------------------------------------------------|
| `Customer.apply _         `   | `(Option[Long], String, String) => Customer`         |
| `(Customer.apply _).tupled`   | `((Option[Long], String, String)) => Customer`       |
| `Customer.unapply _`          | `Customer => Option[(Option[Long], String, String)]` |

---

# `Orders` Table and `Order` Record

```scala
class Orders(tag: Tag) extends Table[Order](tag, "orders") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def customerId = column[Long]("customer_id")
  def date = column[LocalDate]("date")
  def * = (id.?, customerId, date).mapTo[Order]

  def customer = foreignKey("fk_orders_customer_id", customerId, Customers.table)(_.id)
}

object Orders {
  val table = TableQuery[Orders]
}

case class Order(id: Option[Long], customerId: Long, date: LocalDate)
```

---

# `OrderLines` Table and `OrderLine` Record

```scala
class OrderLines(tag: Tag) extends Table[OrderLine](tag, "order_lines") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def orderId = column[Long]("order_id")
  def itemId = column[Long]("item_id")
  def quantity = column[Int]("quantity")
  def * = (id.?, orderId, itemId, quantity).mapTo[OrderLine]

  def order = foreignKey("fk_order_lines_order_id", orderId, Orders.table)(_.id)
  def item = foreignKey("fk_order_lines_item_id", itemId, Items.table)(_.id)
}

object OrderLines {
  val table = TableQuery[OrderLines]
}

case class OrderLine(id: Option[Long], orderId: Long, itemId: Long, quantity: Int)
```

---

# `Items` Table and `Item` Record

```scala
class Items(tag: Tag) extends Table[Item](tag, "items") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("name")
  def * = (id.?, name).mapTo[Item]
}

object Items {
  val table = TableQuery[Items]
}

case class Item(id: Option[Long], name: String)
```

---

# Substructuring a Record Class

[Substructuring the record class](https://stackoverflow.com/questions/42399431/22-column-limit-for-procedures/42414478#42414478)

* `Customer` record class can have an `address` attribute of type `Address` that maps to some of the fields in the `customers` table.
* `Order` record can also hold a `billingAddress` and a `shippingAddress` (also of type `Address`), each mapping to different fields of the `orders` table.
* Much better than super flat record
* Also allows to overcome the 22 fields limit

---

# Custom Column Types

[Custom column types](http://slick.lightbend.com/doc/3.2.3/userdefined.html#scalar-types)

* `Order` record class can have an attribute of enumeration type `OrderStatus`.
* `OrderStatus` can be declared as custom column type to be stored as `VARCHAR` in the `orders` table.

---

# Describing **Queries** (`Query`)

---

# Query (`Query`)

```scala
Query[+E, U, C[_]] /* extends Rep[C[U]] */
// E = SELECT Row Representation
// U = In-memory Row Class
// C = Collection Type (Seq, Set...)
```

* Describes a **`SELECT` query** retrieving **rows**
* Does **no side-effect**, just a query waiting to be run
* Must be **interpreted** against a **database** to do side-effects
* Will return a `C[U]` when run (for example `Seq[Customer]`)
 
---

# Filtering (`WHERE`)

```scala
val selectCustomersQuery: Query[Customers, Customer, Seq] =
  Customers.table
    .filter(_.firstName.like("A%"))

val selectItemQuery: Query[Items, Item, Seq] =
  Items.table
    .filter(_.id === 1l)
```

---

# Mapping (`SELECT`)

```scala
val selectFirstNameAndLastNameQuery:
  Query[(Rep[String], Rep[String]), (String, String), Seq] =
  Customers.table
    .filter(_.id =!= 1l)
    .map(c => (c.firstName, c.lastName))

val selectFullNameQuery:
  Query[Rep[String], String, Seq] =
  Customers.table
    .filter(_.firstName.startsWith("A"))
    .map(c => c.firstName ++ " " ++ c.lastName)
```

---

# Joining (`JOIN`)

```scala
val selectOrdersAndOrderLinesQuery:
  Query[
    (Orders, Rep[Option[OrderLines]]),
    (Order, Option[OrderLine]),
    Seq
  ] =
  Orders.table joinLeft OrderLines.table on (_.id === _.orderId)
```

---

# Sorting (`ORDER BY`)

```scala
val selectOrdersAndOrderLinesOrderedQuery =
  selectOrdersAndOrderLinesQuery
    .sortBy { case (order, maybeOrderLine) =>
      (order.id, maybeOrderLine.map(_.id))
    }
```

---

# Advanced Queries

* [Unioning](http://slick.lightbend.com/doc/3.2.3/queries.html#unions) (`UNION`, `UNION ALL`)
* [Aggregating](http://slick.lightbend.com/doc/3.2.3/queries.html#aggregation) (`GROUP BY`)
* [Plain SQL queries](http://slick.lightbend.com/doc/3.2.3/sql.html)
  - `INTERSECT`, `EXCEPT`
  - Other advanced SQL features
* [Compiled queries](http://slick.lightbend.com/doc/3.2.3/queries.html#compiled-queries)
  - Precompile a **parameterized query** for better performance

---

# Describing **Database I/Os** (`DBIO`)

---

# Database I/O (`DBIO`)

```scala
DBIO[A] // A = Result
```

* Describes a **program accessing a database**
* Does **no side-effect**, just a program waiting to be run
* Must be **interpreted** against a **database** to do side-effects
* When interpreted, it will either
  - **succeed** with a **result** of type `A`,
  - or **fail** holding an **exception**.

---

# Querying (`SELECT`)

```scala
val selectCustomersDBIO: DBIO[Seq[Customer]] =
  selectCustomersQuery.result

val selectItemDBIO: DBIO[Option[Item]] =
  selectItemQuery.result.headOption

val selectOrdersAndOrderLinesOrderedDBIO: DBIO[Seq[(Order, Option[OrderLine])]] =
  selectOrdersAndOrderLinesOrderedQuery.result
```

---

# Inserting (`INSERT`)

```scala
val insertCustomerDBIO: DBIO[Int] =
  Customers.table +=
    Customer(None, "April", "Jones")

val insertCustomersDBIO: DBIO[Seq[Customer]] =
  (Customers.table returning Customers.table) ++= Seq(
    Customer(None, "Anders", "Petersen"),
    Customer(None, "Pedro", "Sanchez"),
    Customer(None, "Natacha", "Borodine")
  )
```

---

# Updating (`UPDATE`)

```scala
val updateCustomerDBIO: DBIO[Int] =
  Customers.table
    .filter(c => c.firstName === "Anders" && c.lastName === "Petersen")
    .map(c => (c.firstName, c.lastName))
    .update("Anton", "Peterson")
```

---

# Deleting (`DELETE`)

```scala
val deleteCustomerDBIO: DBIO[Int] =
  Customers.table
    .filter(_.firstName === "April")
    .delete
```

---

# Combining `DBIO`s into a `DBIO` Program

```scala
case class Result(insertedCustomers: Seq[Customer],
                  customers: Seq[Customer],
                  maybeItem: Option[Item],
                  ordersAndOrderLines: Seq[(Order, Option[OrderLine])])

val program: DBIO[Result] = for {
  _ <- insertCustomerDBIO
  insertedCustomers <- insertCustomersDBIO
  _ <- updateCustomerDBIO
  _ <- deleteCustomerDBIO
  customers <- selectCustomersDBIO
  maybeItem <- selectItemDBIO
  ordersAndOrderLines <- selectOrdersAndOrderLinesOrderedDBIO
} yield Result(insertedCustomers, customers, maybeItem, ordersAndOrderLines)
```

---

# Making a `DBIO` Transactional

```scala
val transactionalProgram: DBIO[Result] = program.transactionally
```

* `transactionally` method results in a `DBIO` that will be run as a **single transaction**.
* Otherwise, each composing `DBIO` would be run in a separate transaction.

---

# Running **Database I/Os** (`DBIO`)

---

# Database Configuration (`application.conf`)

```bash
ecommerce {
  database {
    # http://slick.lightbend.com/doc/3.2.3/api/index.html#
    # slick.jdbc.JdbcBackend$DatabaseFactoryDef@forConfig(String,Config,Driver,ClassLoader):Database

    # numThreads = 200           # (Int, optional, default: 20)
    # queueSize = 100            # (Int, optional, default: 1000)

    # HikariCP Configuration (add "slick-hikaricp" dependency)
    url = "jdbc:postgresql://localhost:5432/ecommerce?currentSchema=ecommerce"
    driver = "org.postgresql.Driver"
    user = "ecommerceapi"
    password = "password"
    # isolation = SERIALIZABLE   # (String, optional)
    # maxConnections = 20        # (Int, optional, default: numThreads)
    # minConnections = 20        # (Int, optional, default: numThreads)
  }
}
```

---

# Loading Database Configuration

```scala
// Load configuration from application.conf (using Lightbend Config library)
val config = ConfigFactory.load()

// Create database object from configuration
val database = Database.forConfig(
  "ecommerce.database",
  config
)
```

---

# Running `DBIO` on Database

```scala
val eventualResult: Future[Result] = database.run(transactionalProgram)
```

* Interprets the program described by the `DBIO[A]` against a database
* Returns a `Future[A]`, a **promise** for a result that will eventually
  - **succeed** with a **result** of type `A`,
  - or **fail** holding an **exception**.
* Exception is just used as a value and is **never thrown**.

---

# Handling `Future` Result

---

# Handling **Success**

```scala
val eventualCompletion: Future[Unit] = for {
  Result(insertedCustomers, customers, maybeItem, ordersAndOrderLines) <- eventualResult
} yield {
  logger.info(s"insertedCustomers=$insertedCustomers")
  logger.info(s"customers=$customers")
  logger.info(s"maybeItem=$maybeItem")
  logger.info(s"ordersAndOrderLines=$ordersAndOrderLines")
}
```

---

# Handling **Failure**

```scala
val eventualSafeCompletion: Future[Unit] = eventualCompletion
  .transform {
    case failure @ Failure(exception) =>
      // Log exception and keep failure as is
      logger.error("Exception occurred", exception)
      failure

    case success @ Success(_) =>
      // Keep success as is
      success
  }
  // Always close database after completion (either success or failure)
  .transformWith(_ => database.shutdown)
```

---

# Waiting for **Completion**

```scala
Await.result(eventualSafeCompletion, 5.seconds)
```

* Will **block** until `Future` completes
  * Return the **result** in case of **success**
  * Raise the **exception** in case of a **failure**
  * **Timeout** after 5 seconds and fail with a `TimeoutException`
* Use this very sparingly!
* _Akka HTTP_ handles futures directly without the hassle.

---

# Combining **Database IOs** (`DBIO`)

---

# Basic `DBIO`s

```scala
val success: DBIO[Int] = DBIO.successful(42)
// Will produce result 42 when run

val failure: DBIO[Nothing] = DBIO.failed(new IllegalStateException("Failure"))
// Will never produce a result and fail with IllegalStateException when run
```

---

# Finding `Customer`, `Order` and `OrderLine`s

```scala
def findCustomer(id: Long): DBIO[Customer] =
  Customers.table.filter(_.id === id).result.head
  
def findOrder(id: Long): DBIO[Order] =
  Orders.table.filter(_.id === id).result.head

def findOrderLines(orderId: Long): DBIO[Seq[OrderLine]] =
  OrderLines.table.filter(_.orderId === orderId).result
```

---

# Transforming `DBIO` (`map`)

```scala
def findOrderDescription(orderId: Long): DBIO[String] = {
  findOrder(orderId).map { order =>
    s"Order #$orderId for customer #${order.customerId}"
  }
}
```

---

# Transforming `DBIO` (`for` / `yield`)

```scala
def findOrderDescription(orderId: Long): DBIO[String] = {
  for {
    order <- findOrder(orderId)
  } yield s"Order #$orderId for customer #${order.customerId}"
}
```

---

# Sequencing `DBIO`s (broken `map`)

```scala
def findOrderCustomer(orderId: Long): DBIO[DBIO[Customer]] = {
  findOrder(orderId).map { order =>
    findCustomer(order.customerId)
  }
} 
```

* Wrong **nested** type `DBIO[DBIO[Customer]]`
* Needs to be made **flat** somehow as `DBIO[Customer]`

---

# Sequencing `DBIO`s (`flatMap`)

```scala
def findOrderCustomer(orderId: Long): DBIO[Customer] = {
  findOrder(orderId).flatMap { order =>
    findCustomer(order.customerId)
  }
}
```

---

# Sequencing `DBIO`s (`for` / `yield`)

```scala
def findOrderCustomer(orderId: Long): DBIO[Customer] = {
  for {
    order <- findOrder(orderId)
    customer <- findCustomer(order.customerId)
  } yield customer
}
```

---

# Sequencing with Non-`DBIO`

```scala
for {
  order <- findOrder(orderId)
  customerId = order.customerId // Not a DBIO, '=' instead of '<-'
  orderLines <- findLines(orderId)
  customer <- findCustomer(customerId)
} yield Result(customer, order, orderLines)
```

---

# Pyramid of `map`s and `flatMap`s :smiling_imp:

```scala
def findOrderAndCustomerAndOrderLines(orderId: Long): DBIO[Result] =
  findOrder(orderId).flatMap { order =>
    findCustomer(order.customerId).flatMap { customer =>
      findLines(orderId).map { orderLines =>
        Result(customer, order, orderLines)
      }
    }
  }
```

---

# Flatten Them All :innocent:

```scala
def findOrderAndCustomerAndOrderLines(orderId: Long): DBIO[Result] =
  for {
    order <- findOrder(orderId)
    customer <- findCustomer(order.customerId)
    orderLines <- findLines(orderId)
  } yield Result(customer, order, orderLines)
```

---

# Anatomy of `for` Comprehension

---

# [fit] **`for` comprehension is not a `for` loop**.
## It can be a `for` loop...
# [fit] But it can handle **many other things**
## like `Option`, `Future` and... `DBIO`.

---

# `for` Comprehension **Types**

```scala
def findOrderAndCustomerAndLines(orderId: Long): DBIO[Result] = {
  for {
    order      /* Order          */ <- findOrder(orderId)       /* DBIO[Order]          */
    customerId /* Long           */ =  order.id.get             /* Long                 */
    lines      /* Seq[OrderLine] */ <- findLines(order.id.get)  /* DBIO[Seq[OrderLine]] */
    customer   /* Customer       */ <- findCustomer(customerId) /* DBIO[Customer]       */
  } yield Result(customer, order, lines) /* Result */
} /* DBIO[Result] */
```

---

# `for` Comprehension **Type Rules**

|            | `val` type | operator | expression type |
|------------|------------|----------|-----------------|
| generator  | `A`        | `<-`     | `DBIO[A]`       |
| assignment | `B`        | `=`      | `B`             |

|        | `for` comprehension type |   |`yield` expression type |
|--------|--------------------------|---|------------------------|
| result | `DBIO[R]`                |   | `R`                    |

* Combines **only `DBIO[T]`**, **no mix** with `Option[T]`, `Future[T]`, `Seq[T]`...
* But it could be **only** `Option[T]`, or **only** `Future[T]`, or **only** `Seq[T]`...

---

# `for` Comprehension **Scopes**

```scala
def findOrderAndCustomerAndLines(orderId: Long): DBIO[Result] = {
  for {
    order <- findOrder(orderId)          /* order                   */
    customerId = order.id.get            /* O    customerId         */
    lines <- findLines(order.id.get)     /* O    |    orderLines    */
    customer<- findCustomer(customerId)  /* |    O    |    customer */
  } yield Result(customer, order, lines) /* O    |    O    O        */
}
```

---

# `for` Comprehension **Implicit Nesting**

```scala
def findOrderAndCustomerAndLines(orderId: Long): DBIO[Result] = {
  for {
       order <- findOrder(orderId)
    /* | */ customerId = order.id.get
    /* |    | */ lines <- findLines(order.id.get)
    /* |    |    | */ customer <- findCustomer(customerId)
  } /* |    |    |    | */ yield Result(customer, order, lines)
}
```

Visually flattens, but still implicitly nested

---

# Conditions and Loops with **Database IOs** (`DBIO`)

---

# A Tale of Free Welcome Order

* A free gift for every customer having never ordered anything
* Materialized by a fictitious order
* Let's call it _Free Welcome Order_ or _FWO_
* Yes, this is a bit contrived :wink:

---

# Count Orders of a Customer

```scala
def findOrderCountByCustomerId(customerId: Long): DBIO[Int] =
  Orders.table
    .filter(_.customerId === customerId)
    .size // Rep[Int]
    .result
```

---

# Insert FWO for a Customer

```scala
def insertFwoByCustomerId(customerId: Long): DBIO[Unit] =
  for {
    orderId <-
      (Orders.table returning Orders.table.map(_.id)) +=
        Order(None, customerId, LocalDate.now())

    _ <- OrderLines.table += OrderLine(None, orderId, 1, 1)
  } yield ()
```

---

# **Conditionally** Insert FWO for a Customer

```scala
def conditionallyInsertFwo(customerId: Long): DBIO[Boolean] =
  for {
    orderCount <- findOrderCountByCustomerId(customerId)

    done <-
      if (orderCount == 0)
        insertFwoByCustomerId(customerId).flatMap(_ => DBIO.successful(true))
      else
        DBIO.successful(false)
  } yield done
```

---

# **Repeatedly** Insert FWO for Customers

```scala
def repeatedlyInsertFwo(customerIds: Seq[Long]): DBIO[Seq[Boolean]] = {
  val seqOfDbio: Seq[DBIO[Boolean]] = customerIds.map(conditionallyInsertFwo)
  val dbioOfSeq: DBIO[Seq[Boolean]] = DBIO.sequence(seqOfDbio)
  dbioOfSeq
}
```

* Make a `Seq[DBIO[Boolean]]` using `map` over a `Seq[Long]`
* Turn it into a `DBIO[Seq[Boolean]]` using `DBIO.sequence`

---

# Repeating with **Recursion** :fearful:

```scala
def insertCustomer(n: Int): DBIO[Int] =
  Customers.table += Customer(None, s"First Name $n", s"Last Name $n")

def insertCustomers(n: Int): DBIO[Int] =
  if (n > 0)
    for {
      count <- insertCustomer(n)
      restCount <- insertCustomers(n - 1) // Recursion
    } yield count + restCount
  else
    DBIO.successful(0)
```

---

# Replacing Recursion with **Fold**

```scala
def insertCustomers(n: Int): DBIO[Int] = {
  val counts: Seq[DBIO[Int]] = (1 to n).map(insertCustomer)
  val totalCount: DBIO[Int] = DBIO.fold(counts, 0)(_ + _)
  totalCount
}
```

* Recursion can be hard to read
* Prefer using simpler alternatives whenever possible
  - `DBIO.sequence`
  - `DBIO.fold`

---

# Further with _Slick_

---

# Generating Table Creation Script

```scala
val schema =
  Customers.table.schema ++
    Orders.table.schema ++
    OrderLines.table.schema ++
    Items.table.schema
    
schema.createStatements.foreach(sql => println(s"$sql;"))
```

```sql
create table "customers" ("id" BIGSERIAL NOT NULL PRIMARY KEY,"first_name" VARCHAR NOT NULL,"last_name" VARCHAR NOT NULL);
create table "orders" ("id" BIGSERIAL NOT NULL PRIMARY KEY,"customer_id" BIGINT NOT NULL,"date" date NOT NULL);
create table "order_lines" ("id" BIGSERIAL NOT NULL PRIMARY KEY,"order_id" BIGINT NOT NULL,"item_id" BIGINT NOT NULL,"quantity" INTEGER NOT NULL);
create table "items" ("id" BIGSERIAL NOT NULL PRIMARY KEY,"name" VARCHAR NOT NULL);
alter table "orders" add constraint "fk_orders_customer_id" foreign key("customer_id") references "customers"("id") on update NO ACTION on delete NO ACTION;
alter table "order_lines" add constraint "fk_order_lines_item_id" foreign key("item_id") references "items"("id") on update NO ACTION on delete NO ACTION;
alter table "order_lines" add constraint "fk_order_lines_order_id" foreign key("order_id") references "orders"("id") on update NO ACTION on delete NO ACTION;
```

---

# Testing for Existence

``` scala
def findOrderExistenceByCustomerId(customerId: Long): DBIO[Boolean] =
  Orders.table
    .filter(_.customerId === customerId)
    .exists // Rep[Boolean]
    .result
```

*  Avoid `.size` that counts all matching records
*  Prefer `.exists` that stops on first matching record

---

# `.headOption` vs `.head`

* `.result.headOption` **always succeeds** with an `Option[T]`
* `.result.head` **might succeed** with a `T` **or fail** with an exception
* Favor `.result.headOption`

---

# Configuring _Slick_ Logs in `logback.xml`

Be sure to add _Logback_ dependency and a `logback.xml` file

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <!-- ... -->
    <logger name="slick.basic" level="INFO"/>
    <logger name="slick.compiler" level="INFO"/>
    <logger name="slick.jdbc" level="DEBUG"/> <!-- Log SQL -->
    <logger name="slick.memory" level="INFO"/>
    <logger name="slick.relational" level="INFO"/>
    <logger name="slick.util" level="INFO"/>

    <logger name="com.zaxxer.hikari" level="INFO"/>
</configuration>
```

---

# More about _Slick_

* [Slick](http://slick.lightbend.com/docs/) documentation
  - [Queries](http://slick.lightbend.com/doc/3.2.3/queries.html)
  - [Database I/O Actions](http://slick.lightbend.com/doc/3.2.3/dbio.html)
  - [Database Configuration](http://slick.lightbend.com/doc/3.2.3/database.html)
  - [Logging](http://slick.lightbend.com/doc/3.2.3/config.html#logging) with _SLF4J_
* [Essential Slick](https://underscore.io/books/essential-slick/) book

---

# Related Libraries Supported by _Slick_

* [Lightbend Config](https://github.com/lightbend/config), application configuration (`application.conf`)
* [SL4J](https://www.slf4j.org/), logging facade
  * Already a dependency of _Slick_... and _Akka HTTP_
* [Scala Logging](https://github.com/lightbend/scala-logging), recommended Scala wrapper for _SLF4J_
* [Logback](https://logback.qos.ch/), standard implementation for _SLF4J_
* [HikariCP](https://github.com/brettwooldridge/HikariCP), database connection pool
* All compatible with _Akka HTTP_
