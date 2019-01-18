package slicktour.ecommerce

import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.Logger
import slicktour.ecommerce.db.ExtendedPostgresProfile.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}

object ECommerceTestApp {
  private val logger = Logger(ECommerceTestApp.getClass)

  def main(args: Array[String]): Unit = {
    val config = ConfigFactory.load()
    val database = Database.forConfig("ecommerce.database", config)

    type Result = Nothing

    val program: DBIO[Result] = ???
    val transactionalProgram: DBIO[Result] = program.transactionally
    val eventualResult: Future[Result] = database.run(transactionalProgram)

    val eventualCompletion: Future[Unit] = ???

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
