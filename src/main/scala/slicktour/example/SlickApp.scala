package slicktour.example

import java.time.LocalDate

import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.Logger
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile
import slicktour.ecommerce.db.ExtendedPostgresProfile.api._
import slicktour.ecommerce.db._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.Failure

object SlickApp {
  private val logger = Logger(SlickApp.getClass)

  def main(args: Array[String]): Unit = {
    val config = ConfigFactory.load()
    val databaseConfig = DatabaseConfig.forConfig[JdbcProfile]("ecommerce.database", config)
    val database = databaseConfig.db

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
    val selectFirstNameAndLastNameForCustomerQuery: Query[(Rep[String], Rep[String]), (String, String), Seq] =
      Customers.table
        .filter(_.id =!= 1l)
        .map(c => (c.firstName, c.lastName))

    val selectFullNameForCustomerQuery: Query[Rep[String], String, Seq] =
      Customers.table
        .filter(_.firstName.startsWith("A"))
        .map(c => c.firstName ++ " " ++ c.lastName)

    // Joining
    val selectOrdersAndOrderLinesQuery: Query[(Orders, Rep[Option[OrderLines]]), (Order, Option[OrderLine]), Seq] =
      (Orders.table joinLeft OrderLines.table on (_.id === _.orderId))
        .sortBy({ case (order, maybeOrderLine) => (order.id, maybeOrderLine.map(_.id)) })

    // -----------------------------------------------------------------------------------------------------------------
    // DBIO
    // -----------------------------------------------------------------------------------------------------------------
    val selectCustomersDBIO: DBIO[Seq[Customer]] =
      selectCustomersQuery.result

    val selectItemDBIO: DBIO[Option[Item]] =
      selectItemQuery.result.headOption

    val selectOrdersAndOrderLinesDBIO: DBIO[Seq[(Order, Option[OrderLine])]] =
      selectOrdersAndOrderLinesQuery.result

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
    case class Result(
                       insertedCustomers: Seq[Customer],
                       customers: Seq[Customer],
                       maybeItem: Option[Item],
                       ordersAndOrderLines: Seq[(Order, Option[OrderLine])]
                     )

    val program1: DBIO[Result] = for {
      _ <- insertCustomerDBIO
      insertedCustomers <- insertCustomersDBIO
      _ <- updateCustomerDBIO
      _ <- deleteCustomerDBIO
      customers <- selectCustomersDBIO
      maybeItem <- selectItemDBIO
      ordersAndOrderLines <- selectOrdersAndOrderLinesDBIO
    } yield Result(insertedCustomers, customers, maybeItem, ordersAndOrderLines)


    // -----------------------------------------------------------------------------------------------------------------
    // Running DBIO
    // -----------------------------------------------------------------------------------------------------------------
    val eventualResult: Future[Result] = database.run(program1)

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
          logger.error("Exception occurred", exception)
          failure

        case success => success
      }
      .transformWith(_ => database.shutdown)

    Await.result(eventualSafeCompletion, 5.seconds)
  }
}

object Combining {
  object TransformingIO {
    object Map {
      def selectOrderById(id: Long): DBIO[Order] = Orders.table.filter(_.id === id).result.head

      def selectOrderDescriptionByOrderId(orderId: Long): DBIO[String] =
        selectOrderById(orderId)
          .map(order => s"Order #$orderId for customer #${order.customerId}")
    }

    object ForComprehension {
      def selectOrderById(id: Long): DBIO[Order] = Orders.table.filter(_.id === id).result.head

      def selectOrderDescriptionByOrderId(orderId: Long): DBIO[String] =
        for {
          order <- selectOrderById(orderId)
        } yield s"Order #$orderId for customer #${order.customerId}"
    }
  }

  object SequencingIOs {
    object BrokenMap {
      def selectOrderById(id: Long): DBIO[Order] =
        Orders.table.filter(_.id === id).result.head

      def selectCustomerById(id: Long): DBIO[Customer] =
        Customers.table.filter(_.id === id).result.head

      def selectCustomerByOrderId(orderId: Long): DBIO[DBIO[Customer]] =
        selectOrderById(orderId)
          .map { order => selectCustomerById(order.customerId) }
    }

    object FlatMap {
      def selectOrderById(id: Long): DBIO[Order] =
        Orders.table.filter(_.id === id).result.head

      def selectCustomerById(id: Long): DBIO[Customer] =
        Customers.table.filter(_.id === id).result.head

      def selectCustomerByOrderId(orderId: Long): DBIO[Customer] =
        selectOrderById(orderId)
          .flatMap { order => selectCustomerById(order.customerId) }
    }

    object ForComprehension {
      def selectOrderById(id: Long): DBIO[Order] =
        Orders.table.filter(_.id === id).result.head

      def selectCustomerById(id: Long): DBIO[Customer] =
        Customers.table.filter(_.id === id).result.head

      def findCustomerByOrderId(orderId: Long): DBIO[Customer] =
        for {
          order <- selectOrderById(orderId)
          customer <- selectCustomerById(order.customerId)
        } yield customer
    }
  }

  object MapAndFlatMapTogether {
    case class Result(customer: Customer, order: Order)

    object MapAndFlatMap {
      def selectOrderById(id: Long): DBIO[Order] =
        Orders.table.filter(_.id === id).result.head

      def selectCustomerById(id: Long): DBIO[Customer] =
        Customers.table.filter(_.id === id).result.head

      def findOrderAndCustomer(orderId: Long): DBIO[Result] = {
        selectOrderById(orderId)
          .flatMap { order =>
            selectCustomerById(order.customerId)
              .map { customer =>
                Result(customer, order)
              }
          }
      }
    }

    object ForComprehension {
      def selectOrderById(id: Long): DBIO[Order] =
        Orders.table.filter(_.id === id).result.head

      def selectCustomerById(id: Long): DBIO[Customer] =
        Customers.table.filter(_.id === id).result.head

      def findOrderAndCustomer(orderId: Long): DBIO[Result] =
        for {
          order <- selectOrderById(orderId)
          customer <- selectCustomerById(order.customerId)
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
    def selectOrderById(id: Long): DBIO[Order] =
      Orders.table.filter(_.id === id).result.head

    def selectCustomerById(id: Long): DBIO[Customer] =
      Customers.table.filter(_.id === id).result.head

    def selectOrderLinesByOrderId(orderId: Long): DBIO[Seq[OrderLine]] =
      OrderLines.table.filter(_.orderId === orderId).result

    case class Result(customer: Customer, order: Order, orderLines: Seq[OrderLine])

    object MapAndFlatMap {
      def selectOrderAndCustomerAndOrderLinesByOrderId(orderId: Long): DBIO[Result] =
        selectOrderById(orderId).flatMap { order =>
          selectCustomerById(order.customerId).flatMap { customer =>
            selectOrderLinesByOrderId(orderId).map { orderLines =>
              Result(customer, order, orderLines)
            }
          }
        }
    }

    object ForComprehension {
      def selectOrderAndCustomerAndOrderLinesByOrderId(orderId: Long): DBIO[Result] =
        for {
          order <- selectOrderById(orderId)
          customer <- selectCustomerById(order.customerId)
          orderLines <- selectOrderLinesByOrderId(orderId)
        } yield Result(customer, order, orderLines)
    }
  }

  object IntermediaryExpression {
    def selectOrderById(id: Long): DBIO[Order] =
      Orders.table.filter(_.id === id).result.head

    def selectOrderLinesByOrderId(orderId: Long): DBIO[Seq[OrderLine]] =
      OrderLines.table.filter(_.orderId === orderId).result

    def selectCustomerById(id: Long): DBIO[Customer] =
      Customers.table.filter(_.id === id).result.head

    case class Result(customer: Customer, order: Order, orderLines: Seq[OrderLine])

    object RawCode {
      object ForComprehension {
        def selectOrderAndCustomerAndOrderLinesByOrderId(orderId: Long): DBIO[Result] = {
          for {
            order <- selectOrderById(orderId)
            customerId = order.customerId
            orderLines <- selectOrderLinesByOrderId(orderId)
            customer <- selectCustomerById(customerId)
          } yield Result(customer, order, orderLines)
        }
      }
    }
  }

  object ForComprehensionAnatomy {
    def selectOrderById(id: Long): DBIO[Order] =
      Orders.table.filter(_.id === id).result.head

    def selectOrderLinesByOrderId(orderId: Long): DBIO[Seq[OrderLine]] =
      OrderLines.table.filter(_.orderId === orderId).result

    def selectCustomerById(id: Long): DBIO[Customer] =
      Customers.table.filter(_.id === id).result.head

    case class Result(customer: Customer, order: Order, orderLines: Seq[OrderLine])

    object Types {
      def selectOrderAndCustomerAndOrderLinesByOrderId(orderId: Long): DBIO[Result] = {
        for {
          order      /* Order          */ <- selectOrderById(orderId)                /* DBIO[Order]          */
          customerId /* Long           */ =  order.id.get                            /* Long                 */
          orderLines /* Seq[OrderLine] */ <- selectOrderLinesByOrderId(order.id.get) /* DBIO[Seq[OrderLine]] */
          customer   /* Customer       */ <- selectCustomerById(customerId)          /* DBIO[Customer]       */
        } yield Result(customer, order, orderLines) /* Result */
      } /* DBIO[Result] */
    }

    object Nesting {
      def selectOrderAndCustomerAndOrderLinesByOrderId(orderId: Long): DBIO[Result] = {
        for {
             order <- selectOrderById(orderId)
          /* | */ customerId = order.id.get
          /* |    | */ orderLines <- selectOrderLinesByOrderId(order.id.get)
          /* |    |    | */ customer <- selectCustomerById(customerId)
        } /* |    |    |    | */ yield Result(customer, order, orderLines)
      }
    }
  }
}

object ConditionsAndLoops {
  def selectOrderCountByCustomerId(customerId: Long): DBIO[Int] =
    Orders.table
      .filter(_.customerId === customerId)
      .size
      .result

  def insertFwoByCustomerId(customerId: Long): DBIO[Unit] =
    for {
      orderId <- (Orders.table returning Orders.table.map(_.id)) += Order(None, 1, LocalDate.now())
      _ <- OrderLines.table += OrderLine(None, orderId, 1, 1)
    } yield ()

  def insertFwoWhenInactiveByCustomerId(customerId: Long): DBIO[Boolean] =
    for {
      orderCount <- selectOrderCountByCustomerId(customerId)

      done <-
        if (orderCount == 0)
          insertFwoByCustomerId(customerId)
            .map(_ => true)
        else
          DBIO.successful(false)
    } yield done

  def insertFwosWhenInactiveByCustomerIds(customerIds: Seq[Long]): DBIO[Seq[Boolean]] = {
    val seqOfDbio: Seq[DBIO[Boolean]] = customerIds.map(insertFwoWhenInactiveByCustomerId)
    val dbioOfSeq: DBIO[Seq[Boolean]] = DBIO.sequence(seqOfDbio)
    dbioOfSeq
  }
}

object RecursionWorksToo {
  // In these examples DBIO.sequence would be much better.
  // Recursion might be a code smell.

  def insertCustomers(n: Long): DBIO[Unit] =
    if (n > 0)
      for {
        _ <- Customers.table += Customer(None, s"First Name $n", s"Last Name $n")
        _ <- insertCustomers(n - 1)
      } yield ()
    else
      DBIO.successful(())

  def insertAndReturnCustomers(n: Long): DBIO[Seq[Customer]] =
    if (n > 0)
      for {
        customer <- (Customers.table returning Customers.table) += Customer(None, s"First Name $n", s"Last Name $n")
        customers <- insertAndReturnCustomers(n - 1)
      } yield customer +: customers
    else
      DBIO.successful(Seq.empty)
}
