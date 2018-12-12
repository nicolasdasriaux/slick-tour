package slicktour.ecommerce.db

import java.time.LocalDate

case class Order(id: Option[Long], customerId: Long, date: LocalDate)
