package slicktour.example

import com.typesafe.config.ConfigFactory
import org.slf4j.{Logger, LoggerFactory}
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile
import slicktour.ecommerce.db.ExtendedPostgresProfile.api._
import slicktour.ecommerce.db.{Customer, Customers, Item, Items}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.Failure

object SlickApp {
  private val logger: Logger = LoggerFactory.getLogger(SlickApp.getClass)

  def main(args: Array[String]): Unit = {
    val config = ConfigFactory.load()
    val databaseConfig = DatabaseConfig.forConfig[JdbcProfile]("ecommerce.database", config)
    val database = databaseConfig.db

    val selectCustomersQuery: Query[Customers, Customer, Seq] = Customers.table.filter(_.firstName.like("A%"))
    val selectItemQuery: Query[Items, Item, Seq] = Items.table.filter(_.id === 1l)

    val customersDBIO: DBIO[Seq[Customer]] = selectCustomersQuery.result
    val maybeItemDBIO: DBIO[Option[Item]] = selectItemQuery.result.headOption

    val program: DBIO[(Seq[Customer], Option[Item])] = for {
      customers <- customersDBIO
      items <- maybeItemDBIO
    } yield (customers, items)

    val eventualResult: Future[(Seq[Customer], Option[Item])] = database.run(program)

    val eventualCompletion: Future[Unit] = for {
        (customers, items) <- eventualResult

        _ = {
          println(s"customers=$customers")
          println(s"items=$items")
        }
      } yield ()

    val eventualSafeCompletion = eventualCompletion
      .transform {
        case failure @ Failure(exception) =>
          logger.error("Exception occurred", exception)
          failure
      }
      .transformWith(_ => database.shutdown)

    Await.result(eventualSafeCompletion, 5.seconds)
  }
}
