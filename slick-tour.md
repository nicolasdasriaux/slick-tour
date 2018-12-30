autoscale: true
footer: Relational Database Access with Slick
slidenumbers: true

# [fit] **Relational Database Access**
## with _Slick_

---

# Tailoring a Database **Profile**

---

# Assembling a _PostgreSQL_ Profile

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

* Import when in need to
  - to describe **tables**,
  - to describe **queries**,
  - and more.

---

# [fit] Describing **Tables** and Mapping **Record Classes**

---

# `Customers` Table and `Customer` Record


```scala
case class Customer(id: Option[Long], firstName: String, lastName: String)
```

```scala
class Customers(tag: Tag) extends Table[Customer](tag, "customers") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def firstName = column[String]("first_name")
  def lastName = column[String]("last_name")
  def * = (id.?, firstName, lastName).mapTo[Customer]
}

object Customers {
  val table = TableQuery[Customers]
}
```

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
| `(Customer.apply _).tupled`   | `((Option[Long], String, String)) => Customer`       |
| `Customer.unapply`            | `Customer => Option[(Option[Long], String, String)]` |

---

# `Orders` Table and `Order` Record

```scala
case class Order(id: Option[Long], customerId: Long, date: LocalDate)
```

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
```

---

# `OrderLines` Table and `OrderLine` Record

```scala
case class OrderLine(id: Option[Long], orderId: Long, itemId: Long, quantity: Int)
```

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
```

---

# `Items` Table and `Item` Record

```scala
case class Item(id: Option[Long], name: String)
```

```scala
class Items(tag: Tag) extends Table[Item](tag, "items") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("name")
  def * = (id.?, name).mapTo[Item]
}

object Items {
  val table = TableQuery[Items]
}
```

---

# Substructuring a Record Class

* [Substructuring the record class](https://stackoverflow.com/questions/42399431/22-column-limit-for-procedures/42414478#42414478)
  - `Customer` record class can have an `address` attribute of type `Address` that maps on some of the fields in the `customers` table.
  - `Order` record can also hold a `billingAddress` and a `shippingAddress` (also of type `Address`), each mapping on different fields of the `orders` table.
  - Much better than super flat record
  - Also allows to overcome the 22 fields limit

---

# Custom Column Types

* [Custom column types](http://slick.lightbend.com/doc/3.2.3/userdefined.html#scalar-types)
  - `Order` record class can have an attribute of enumeration type `ShippingStatus`.
  - `ShippingStatus` can be declared as custom column type to be stored as `VARCHAR` in the `orders` table.

---

# Describing **Queries** (`Query`)

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

# Sorting (`ORDER` `BY`)

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
* Otherwise, all composing `DBIO`s would be run in a separate transaction.

---

# Running **Database I/Os**

---

# Database Configuration (`application.conf`)

```
ecommerce {
  database {
    profile = "slicktour.ecommerce.db.ExtendedPostgresProfile$"

    db {
      url = "jdbc:postgresql://localhost:5432/ecommerce?currentSchema=ecommerce"
      driver = "org.postgresql.Driver"
      user = "ecommerceapi"
      password = "password"
    }
  }
}
```

---

# Loading Database Configuration

```scala
// Load configuration from application.conf (using Lightbend Config library)
val config = ConfigFactory.load()

// Extract database configuration from configuration
val databaseConfig = DatabaseConfig.forConfig[JdbcProfile](
  "ecommerce.database",
  config
)
```

---

# Running `DBIO` on Database

```scala
val database = databaseConfig.db
val eventualResult: Future[Result] = database.run(transactionalProgram)
```

* `run` returns a `Future`, a promise for a result that will eventually
  - **succeed** with a **value**,
  - or **failure** with an **exception**.
* In case of failure, exception is just used as a value and is **never thrown**.

---

# Handling Future **Success**

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

# Handling Future **Failure**

```scala
val eventualSafeCompletion: Future[Unit] = eventualCompletion
  .transform {
    case failure @ Failure(exception) =>
      // Log exception and key failure as is
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

# Waiting for Future **Completion**

```scala
Await.result(eventualSafeCompletion, 5.seconds)
```

* Will **block** until future completes
  * Return the **value** in case of **success**
  * Raise the **exception** in case of a **failure**
  * **Timeout** after 5 seconds and fail with a `TimeoutException`
* Use this very sparingly!
* _Akka HTTP_ handles futures directly without the hassle.

---

# Combining `DBIO`s

---

# Basic `DBIO`s

```scala
val success: DBIO[Int] = DBIO.successful(42)
// Will produce value 42 when run

val failure: DBIO[Nothing] = DBIO.failed(new IllegalStateException("Failure"))
// Will never produce a value and fail with IllegalStateException when run
```

---

# Finding `Customer`, `Order` and `OrderLines`s

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
def findOrderDescription(orderId: Long): DBIO[String] =
  findOrder(orderId).map { order =>
    s"Order #$orderId for customer #${order.customerId}"
  }
```

---

# Transforming `DBIO` (`for` / `yield`)

```scala
def selectOrderDescriptionByOrderId(orderId: Long): DBIO[String] =
  for {
    order <- findOrder(orderId)
  } yield s"Order #$orderId for customer #${order.customerId}"
```

---

# Anatomy of `for` Comprehension

---

# `for` Comprehension **Types**

```scala
def selectOrderAndCustomerAndLines(orderId: Long): DBIO[Result] = {
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

|            | `for` comprehension type | `yield` expression type |
|------------|------------------------- |-------------------------|
| production | `DBIO[R]`                | `R`                     |

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

# `for` Comprehension **Nesting**

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
