package slicktour.ecommerce.api

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.stream.{ActorMaterializer, Materializer}
import com.typesafe.config.ConfigFactory
import configs.syntax._
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile
import slicktour.ecommerce.api.ECommerceApiProtocol._
import slicktour.ecommerce.service.customer.{Customer, CustomerPost, CustomerService}

import scala.concurrent.ExecutionContext

object ECommerceApiApp {
  def main(args: Array[String]): Unit = {
    val config = ConfigFactory.load()
    val databaseConfig = DatabaseConfig.forConfig[JdbcProfile]("ecommerce.database", config)
    val apiConfig = config.get[ECommerceApiConfig]("ecommerce.api").value

    implicit val actorSystem: ActorSystem = ActorSystem("ecommerce-api")
    implicit val executionContext: ExecutionContext = actorSystem.dispatcher
    implicit val materializer: Materializer = ActorMaterializer()

    val database = databaseConfig.db
    val customerService = new CustomerService(database)

    // @formatter:off
    val route =
      rejectEmptyResponse {
        (get & path("customers")) {
          val eventualCustomers = customerService.findAll()
          complete(eventualCustomers)
        } ~
        (get & path("customers" / LongNumber)) { id =>
          val eventualCustomer = customerService.find(id)
          complete(eventualCustomer)
        } ~
        (post & path("customers")) {
          entity(as[CustomerPost]) { customerPost =>
            val eventualCustomer = customerService.insert(customerPost)
            complete(eventualCustomer)
          }
        } ~
        (post & path("customers" / LongNumber)) { id =>
          entity(as[CustomerPost]) { customerPost =>
            val customer = Customer(id, customerPost.firstName, customerPost.lastName)
            val eventualCount = customerService.update(customer)

            onSuccess(eventualCount) { insertCount =>
              complete(s"Updated $insertCount customers")
            }
          }
        }
      }
    // @formatter:on

    val http = Http()
    http.bindAndHandle(route, apiConfig.interface, apiConfig.port)
  }
}
