package slicktour.ecommerce.service.order

import java.time.LocalDate

case class Order(id: Long, date: LocalDate, orderLines: Seq[OrderLine], customerId: Long)
