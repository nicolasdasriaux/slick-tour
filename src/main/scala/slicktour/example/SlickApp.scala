package slicktour.example

import java.time.LocalDate

import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.Logger
import slicktour.ecommerce.db.ExtendedPostgresProfile.api._
import slicktour.ecommerce.db._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util._

object SlickApp {
  private val logger = Logger(SlickApp.getClass)

  def main(args: Array[String]): Unit = {
    // -----------------------------------------------------------------------------------------------------------------
    // Query
    // -----------------------------------------------------------------------------------------------------------------

    // Filtering (where)
    val selectCustomersQuery: Query[Customers, Customer, Seq] =
      Customers.table
        .filter(_.firstName.like("A%"))

    val selectItemQuery: Query[Items, Item, Seq] =
      Items.table
        .filter(_.id === 1l)

    // Mapping (select)
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

    // Joining
    val selectOrdersAndOrderLinesQuery:
      Query[
        (Orders, Rep[Option[OrderLines]]),
        (Order, Option[OrderLine]),
        Seq
      ] =
      Orders.table joinLeft OrderLines.table on (_.id === _.orderId)

    // Sorting
    val selectOrdersAndOrderLinesOrderedQuery =
      selectOrdersAndOrderLinesQuery
        .sortBy { case (order, maybeOrderLine) =>
          (order.id, maybeOrderLine.map(_.id))
        }

    // -----------------------------------------------------------------------------------------------------------------
    // DBIO
    // -----------------------------------------------------------------------------------------------------------------
    val selectCustomersDBIO: DBIO[Seq[Customer]] =
      selectCustomersQuery.result

    val selectItemDBIO: DBIO[Option[Item]] =
      selectItemQuery.result.headOption

    val selectOrdersAndOrderLinesOrderedDBIO: DBIO[Seq[(Order, Option[OrderLine])]] =
      selectOrdersAndOrderLinesOrderedQuery.result

    val insertCustomerDBIO: DBIO[Int] =
      Customers.table +=
        Customer(None, "April", "Jones")

    val insertCustomersDBIO: DBIO[Seq[Customer]] =
      (Customers.table returning Customers.table) ++= Seq(
        Customer(None, "Anders", "Petersen"),
        Customer(None, "Pedro", "Sanchez"),
        Customer(None, "Natacha", "Borodine")
      )

    val updateCustomerDBIO: DBIO[Int] =
      Customers.table
        .filter(c => c.firstName === "Anders" && c.lastName === "Petersen")
        .map(c => (c.firstName, c.lastName))
        .update("Anton", "Peterson")

    val deleteCustomerDBIO: DBIO[Int] =
      Customers.table
        .filter(_.firstName === "April")
        .delete

    // -----------------------------------------------------------------------------------------------------------------
    // Combining DBIOs
    // -----------------------------------------------------------------------------------------------------------------
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

    val transactionalProgram: DBIO[Result] = program.transactionally

    // -----------------------------------------------------------------------------------------------------------------
    // Running DBIO
    // -----------------------------------------------------------------------------------------------------------------
    // Load configuration from application.conf (using Lightbend Config library)
    val config = ConfigFactory.load()

    // Create database object from configuration
    val database = Database.forConfig(
      "ecommerce.database",
      config
    )

    val eventualResult: Future[Result] = database.run(transactionalProgram)

    // -----------------------------------------------------------------------------------------------------------------
    // Handling future run result
    // -----------------------------------------------------------------------------------------------------------------
    val eventualCompletion: Future[Unit] = for {
      Result(insertedCustomers, customers, maybeItem, ordersAndOrderLines) <- eventualResult
    } yield {
      logger.info(s"insertedCustomers=$insertedCustomers")
      logger.info(s"customers=$customers")
      logger.info(s"maybeItem=$maybeItem")
      logger.info(s"ordersAndOrderLines=$ordersAndOrderLines")
    }

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

    Await.result(eventualSafeCompletion, 5.seconds)
  }
}

object SimpleDBIO {
  val success: DBIO[Int] = DBIO.successful(42)
  // Will produce value 42 when run

  val failure: DBIO[Nothing] = DBIO.failed(new IllegalStateException("Failure"))
  // Will never produce a value and fail with IllegalStateException when run
}

object Combining {
  def findOrder(id: Long): DBIO[Order] =
    Orders.table.filter(_.id === id).result.head

  def findCustomer(id: Long): DBIO[Customer] =
    Customers.table.filter(_.id === id).result.head

  def findLines(orderId: Long): DBIO[Seq[OrderLine]] =
    OrderLines.table.filter(_.orderId === orderId).result

  object TransformingDBIO {
    object Map {
      def findOrderDescription(orderId: Long): DBIO[String] = {
        findOrder(orderId).map { order =>
          s"Order #$orderId for customer #${order.customerId}"
        }
      }
    }

    object ForComprehension {
      def findOrderDescription(orderId: Long): DBIO[String] = {
        for {
          order <- findOrder(orderId)
        } yield s"Order #$orderId for customer #${order.customerId}"
      }
    }
  }

  object SequencingDBIOs {
    object BrokenMap {
      def findOrderCustomer(orderId: Long): DBIO[DBIO[Customer]] = {
        findOrder(orderId).map { order =>
          findCustomer(order.customerId)
        }
      }
    }

    object FlatMap {
      def findOrderCustomer(orderId: Long): DBIO[Customer] = {
        findOrder(orderId).flatMap { order =>
          findCustomer(order.customerId)
        }
      }
    }

    object ForComprehension {
      def findOrderCustomer(orderId: Long): DBIO[Customer] = {
        for {
          order <- findOrder(orderId)
          customer <- findCustomer(order.customerId)
        } yield customer
      }
    }
  }

  object MapAndFlatMapTogether {
    case class Result(customer: Customer, order: Order)

    object MapAndFlatMap {
      def findOrderAndCustomer(orderId: Long): DBIO[Result] = {
        findOrder(orderId).flatMap { order =>
          findCustomer(order.customerId).map { customer =>
            Result(customer, order)
          }
        }
      }
    }

    object ForComprehension {
      def findOrderAndCustomer(orderId: Long): DBIO[Result] =
        for {
          order <- findOrder(orderId)
          customer <- findCustomer(order.customerId)
        } yield Result(customer, order)
    }
  }

  object InliningPreservesSemantics {
    // Referential Transparency
    case class Result(customer: Customer, order: Order)

    object MapAndFlatMap {
      def findOrderAndCustomer(orderId: Long): DBIO[Result] =
        Orders.table.filter(_.id === orderId).result.head
          .flatMap { order =>
            Customers.table.filter(_.id === order.customerId).result.head
              .map { customer =>
                Result(customer, order)
              }
          }
    }

    object ForComprehension {
      def findOrderAndCustomer(orderId: Long): DBIO[Result] =
        for {
          order <- Orders.table.filter(_.id === orderId).result.head
          customer <- Customers.table.filter(_.id === order.customerId).result.head
        } yield Result(customer, order)
    }
  }

  object TooManyMapsAndFlatMaps {
    case class Result(customer: Customer, order: Order, orderLines: Seq[OrderLine])

    object MapAndFlatMap { // Too deep a nesting
      def findOrderAndCustomerAndOrderLines(orderId: Long): DBIO[Result] =
        findOrder(orderId).flatMap { order =>
          findCustomer(order.customerId).flatMap { customer =>
            findLines(orderId).map { orderLines =>
              Result(customer, order, orderLines)
            }
          }
        }
    }

    object ForComprehension { // No more nesting (actually it's hidden now)
      def findOrderAndCustomerAndOrderLines(orderId: Long): DBIO[Result] =
        for {
          order <- findOrder(orderId)
          customer <- findCustomer(order.customerId)
          orderLines <- findLines(orderId)
        } yield Result(customer, order, orderLines)
    }
  }

  object SequencingNonIO {
    case class Result(customer: Customer, order: Order, orderLines: Seq[OrderLine])

    def findOrderAndCustomerAndOrderLines(orderId: Long): DBIO[Result] = {
      for {
        order <- findOrder(orderId)
        customerId = order.customerId // Not a DBIO, no -> but =
        orderLines <- findLines(orderId)
        customer <- findCustomer(customerId)
      } yield Result(customer, order, orderLines)
    }
  }

  object ForComprehensionAnatomy {
    case class Result(customer: Customer, order: Order, lines: Seq[OrderLine])

    object Types {
      def findOrderAndCustomerAndLines(orderId: Long): DBIO[Result] = {
        for {
          order      /* Order          */ <- findOrder(orderId)       /* DBIO[Order]          */
          customerId /* Long           */ = order.id.get              /* Long                 */
          lines      /* Seq[OrderLine] */ <- findLines(order.id.get)  /* DBIO[Seq[OrderLine]] */
          customer   /* Customer       */ <- findCustomer(customerId) /* DBIO[Customer]       */
        } yield Result(customer, order, lines) /* Result */
      } /* DBIO[Result] */
    }

    object Scopes {
      def findOrderAndCustomerAndLines(orderId: Long): DBIO[Result] = {
        for {
          order <- findOrder(orderId)          /* order                   */
          customerId = order.id.get            /* O    customerId         */
          lines <- findLines(order.id.get)     /* O    |    orderLines    */
          customer <- findCustomer(customerId) /* |    O    |    customer */
        } yield Result(customer, order, lines) /* O    |    O    O        */
      }
    }

    object Nesting {
      def findOrderAndCustomerAndLines(orderId: Long): DBIO[Result] = {
        for {
             order <- findOrder(orderId)
          /* | */ customerId = order.id.get
          /* |    | */ lines <- findLines(order.id.get)
          /* |    |    | */ customer <- findCustomer(customerId)
        } /* |    |    |    | */ yield Result(customer, order, lines)
      }
    }
  }
}

object ConditionsAndLoops {
  def findOrderCountByCustomerId(customerId: Long): DBIO[Int] =
    Orders.table
      .filter(_.customerId === customerId)
      .size
      .result

  def insertFwoByCustomerId(customerId: Long): DBIO[Unit] =
    for {
      orderId <-
        (Orders.table returning Orders.table.map(_.id)) +=
          Order(None, customerId, LocalDate.now())

      _ <- OrderLines.table += OrderLine(None, orderId, 1, 1)
    } yield ()

  def conditionallyInsertFwo(customerId: Long): DBIO[Boolean] =
    for {
      orderCount <- findOrderCountByCustomerId(customerId)

      done <-
        if (orderCount == 0)
          insertFwoByCustomerId(customerId).flatMap(_ => DBIO.successful(true))
        else
          DBIO.successful(false)
    } yield done

  def repeatedlyInsertFwo(customerIds: Seq[Long]): DBIO[Seq[Boolean]] = {
    val seqOfDbio: Seq[DBIO[Boolean]] = customerIds.map(conditionallyInsertFwo)
    val dbioOfSeq: DBIO[Seq[Boolean]] = DBIO.sequence(seqOfDbio)
    dbioOfSeq
  }
}

object RecursionWorksToo {
  def insertCustomer(n: Int): DBIO[Int] =
    Customers.table += Customer(None, s"First Name $n", s"Last Name $n")

  object Recursion {
    def insertCustomers(n: Int): DBIO[Int] =
      if (n > 0)
        for {
          count <- insertCustomer(n)
          restCount <- insertCustomers(n - 1) // Recursion
        } yield count + restCount
      else
        DBIO.successful(0)
  }

  object Sequence {
    def insertCustomers(n: Int): DBIO[Int] = {
      val counts: Seq[DBIO[Int]] = (1 to n).map(insertCustomer)
      val totalCount: DBIO[Int] = DBIO.fold(counts, 0)(_ + _)
      totalCount
    }
  }
}

object Existence {
  def findOrderExistenceByCustomerId(customerId: Long): DBIO[Boolean] =
    Orders.table
      .filter(_.customerId === customerId)
      .exists
      .result
}
