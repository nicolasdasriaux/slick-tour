package slicktour.ecommerce.api

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import slicktour.ecommerce.service.customer.{Customer, CustomerPost}
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

object ECommerceApiProtocol extends SprayJsonSupport with DefaultJsonProtocol {
  implicit lazy val customerPostFormat: RootJsonFormat[CustomerPost] = jsonFormat2(CustomerPost.apply)
  implicit lazy val customerFormat: RootJsonFormat[Customer] = jsonFormat3(Customer.apply)
}
