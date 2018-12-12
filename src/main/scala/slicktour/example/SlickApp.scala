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
    // Queries
    // -----------------------------------------------------------------------------------------------------------------
    val selectCustomersQuery: Query[Customers, Customer, Seq] =
      Customers.table
        .filter(_.firstName.like("A%"))

    val selectItemQuery: Query[Items, Item, Seq] =
      Items.table
        .filter(_.id === 1l)

    val selectOrdersAndOrderLinesQuery: Query[(Orders, Rep[Option[OrderLines]]), (Order, Option[OrderLine]), Seq]  =
      (Orders.table joinLeft OrderLines.table on (_.id === _.orderId))
        .sortBy({ case (order, maybeOrderLine) => (order.id, maybeOrderLine.map(_.id))})

    // -----------------------------------------------------------------------------------------------------------------
    // DBIOs
    // -----------------------------------------------------------------------------------------------------------------
    val selectCustomersDBIO: DBIO[Seq[Customer]] =
      selectCustomersQuery.result

    val selectItemDBIO: DBIO[Option[Item]] =
      selectItemQuery.result.headOption

    val selectOrdersAndOrderLinesDBIO: DBIO[Seq[(Order, Option[OrderLine])]] =
      selectOrdersAndOrderLinesQuery.result

    val insertCustomerDBIO: DBIO[Int] =
      Customers.table += Customer(None, "April", "Jones")

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

    val eventualSafeCompletion = eventualCompletion
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
  object Example1 {
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

  object Example2 {
    case class Result(customer: Customer, order: Order, orderLines: Seq[OrderLine])

    object MapAndFlatMap {
      def findOrderAndCustomer(orderId: Long): DBIO[Result] =
        Orders.table.filter(_.id === orderId).result.head
          .flatMap { order =>
            Customers.table.filter(_.id === order.customerId).result.head
              .flatMap { customer =>
                OrderLines.table.filter(_.orderId === order.id).result
                  .map { orderLines =>
                    Result(customer, order, orderLines)
                  }
              }
          }
    }

    object ForComprehension {
      def findOrderAndCustomer(orderId: Long): DBIO[Result] =
        for {
          order <- Orders.table.filter(_.id === orderId).result.head
          customer <- Customers.table.filter(_.id === order.customerId).result.head
          orderLines <- OrderLines.table.filter(_.orderId === order.id).result
        } yield Result(customer, order, orderLines)
    }
  }
}

object ConditionsAndLoops {
  def customerOrderCount(customerId: Long): DBIO[Int] =
    Orders.table
      .filter(_.customerId === customerId)
      .size
      .result

  def insertFreeWelcomeOrder(customerId: Long): DBIO[Unit] =
    for {
      orderId <- (Orders.table returning Orders.table.map(_.id)) += Order(None, 1, LocalDate.now())
      _ <- OrderLines.table += OrderLine(None, orderId, 1, 1)
    } yield ()

  def insertFreeWelcomeOrderForCustomer(customerId: Long): DBIO[Boolean] =
    for {
      orderCount <- customerOrderCount(customerId)

      freeWelcomeOrder <-
        if (orderCount == 0) insertFreeWelcomeOrder(customerId).map(_ => true)
        else DBIO.successful(false)

    } yield freeWelcomeOrder

  def insertFreeWelcomeOrderForCustomers(customerIds: Seq[Long]): DBIO[Seq[Boolean]] = {
    val seqOfDbio: Seq[DBIO[Boolean]] = customerIds.map(insertFreeWelcomeOrderForCustomer)
    val dbioOfSeq: DBIO[Seq[Boolean]] = DBIO.sequence(seqOfDbio)
    dbioOfSeq
  }
}
