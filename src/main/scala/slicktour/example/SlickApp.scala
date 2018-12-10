package slicktour.example

import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.Logger
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile
import slicktour.ecommerce.db.ExtendedPostgresProfile.api._
import slicktour.ecommerce.db.{Customer, Customers, Item, Items}

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

    val selectCustomersQuery: Query[Customers, Customer, Seq] = Customers.table.filter(_.firstName.like("A%"))
    val selectItemQuery: Query[Items, Item, Seq] = Items.table.filter(_.id === 1l)

    val selectCustomersDBIO: DBIO[Seq[Customer]] = selectCustomersQuery.result
    val selectItemDBIO: DBIO[Option[Item]] = selectItemQuery.result.headOption

    val program: DBIO[(Seq[Customer], Option[Item])] = for {
      customers <- selectCustomersDBIO
      maybeItem <- selectItemDBIO
    } yield (customers, maybeItem)

    val eventualResult: Future[(Seq[Customer], Option[Item])] = database.run(program)

    val eventualCompletion: Future[Unit] = for {
        (customers, maybeItem) <- eventualResult

        _ = {
          logger.info(s"customers=$customers")
          logger.info(s"item=$maybeItem")
        }
      } yield ()

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
