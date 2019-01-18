package slicktour.ecommerce.api

import java.time.LocalDate

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import slicktour.ecommerce.service.customer.{Customer, CustomerPost}
import slicktour.ecommerce.service.order.{Item, Order, OrderLine}
import spray.json.{DefaultJsonProtocol, JsString, JsValue, JsonFormat, RootJsonFormat, deserializationError}

import scala.util.Try

object ECommerceApiProtocol extends SprayJsonSupport with DefaultJsonProtocol {
  implicit lazy val customerPostFormat: RootJsonFormat[CustomerPost] = jsonFormat2(CustomerPost.apply)
  implicit lazy val customerFormat: RootJsonFormat[Customer] = jsonFormat3(Customer.apply)
  implicit lazy val orderFormat: RootJsonFormat[Order] = jsonFormat4(Order.apply)
  implicit lazy val orderLineFormat: RootJsonFormat[OrderLine] = jsonFormat2(OrderLine.apply)
  implicit lazy val itemFormat: RootJsonFormat[Item] = jsonFormat2(Item.apply)

  implicit lazy val localDateFormat: JsonFormat[LocalDate] = new JsonFormat[LocalDate] {
    override def write(localDate: LocalDate): JsValue = JsString(localDate.toString)

    override def read(json: JsValue): LocalDate = json match {
      case JsString(s) => Try(LocalDate.parse(s)).getOrElse(deserializationError(s"Unknown Local Date format($s)"))
      case _ => deserializationError("String expected for Local Date")
    }
  }
}
